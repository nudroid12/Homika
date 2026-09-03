const PUBLIC_SIGNING_KEY_B64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsyzhDrq517vuIzP99flbfNSEPzrocJH/Dqlp07CNR+vNYadLpqpsVKGV+3SIBtF9ytcV6gB00d6dIVbfL5ORS0YY+XgKQhGjHAZ9/AWk1VqUCvXtavrZWA0kUMNy5kImzdtX/0cMclqH9WpC4kQxcsCgjpQp80mhdK3db1zmHsdi/4fH7Kxgcz1NTzFM3/8fLVXg1KdHw356vGmjJRoAxG8rg4rbymmgIRwFYnKUbyrG9xL4iBJ/J+D4zR5+DxQ3UCRKg5/576epGuWqkHARjxcR4IE1NEfsRHyqiRT4gXRoPdJfgSWB7nIGQ9Qvc8az6JQs4c7dV0wsMhUQ00XaewIDAQAB";
const TOKEN_HEADER = { alg: "RS256", typ: "HAT", v: 1 };
const TOKEN_LIFETIME_SECONDS = 5 * 365 * 24 * 60 * 60;

export default {
  async fetch(request, env) {
    try {
      const url = new URL(request.url);

      if (request.method === "OPTIONS") {
        return new Response(null, { status: 204, headers: corsHeaders() });
      }

      if (request.method === "GET" && url.pathname === "/health") {
        return json({
          ok: true,
          service: "app-license-api",
          version: 9,
          signed_tokens: true,
          license_plans: true,
          cloud_backup: true,
          cloud_backup_retention: 5,
          cloud_sync: true,
          cloud_sync_protocol: 2,
          cloud_sync_mode: "encrypted_device_snapshots",
          device_management: true,
          commercial_licensing_ux: true,
          purchase_redirect: true,
          store_catalog: true,
          self_service_trial: true,
          trial_days: 7,
          trial_max_devices: 1,
        });
      }

      if (request.method === "GET" && url.pathname === "/buy/homika-pro") {
        return homikaPurchaseRedirect(url, env);
      }

      if (request.method === "GET" && url.pathname === "/v1/plans") {
        return publicPlans(env);
      }

      if (request.method === "GET" && url.pathname === "/v1/store/catalog") {
        return storeCatalog(env);
      }

      if (request.method === "POST" && url.pathname === "/v1/trials/claim") {
        return claimTrial(request, env);
      }

      if (request.method === "POST" && url.pathname === "/v1/licenses/activate") {
        return activateLicense(request, env);
      }

      if (request.method === "POST" && url.pathname === "/v1/licenses/validate") {
        return validateLicense(request, env);
      }

      if (request.method === "POST" && url.pathname === "/v1/licenses/deactivate") {
        return deactivateDevice(request, env);
      }

      if (request.method === "GET" && url.pathname === "/v1/licenses/devices") {
        return listLicenseDevices(request, env);
      }

      if (request.method === "POST" && url.pathname === "/v1/licenses/devices/deactivate") {
        return deactivateLicenseDevice(request, env);
      }

      if (request.method === "GET" && url.pathname === "/v1/cloud/key") {
        return cloudKey(request, env);
      }

      if (request.method === "POST" && url.pathname === "/v1/cloud/backups") {
        return uploadCloudBackup(request, env);
      }

      if (request.method === "GET" && url.pathname === "/v1/cloud/backups/latest") {
        return latestCloudBackup(request, env);
      }

      if (request.method === "GET" && url.pathname === "/v1/cloud/backups/latest/content") {
        return latestCloudBackupContent(request, env);
      }


      if (request.method === "POST" && url.pathname === "/v1/cloud/sync/push") {
        return pushCloudSync(request, env);
      }

      if (request.method === "GET" && url.pathname === "/v1/cloud/sync/pull") {
        return pullCloudSync(request, env);
      }

      if (request.method === "GET" && url.pathname === "/v1/cloud/sync/snapshots") {
        return listCloudSyncSnapshots(request, env);
      }

      if (request.method === "PUT" && url.pathname === "/v1/cloud/sync/snapshots/current") {
        return uploadCloudSyncSnapshot(request, env);
      }

      if (request.method === "GET" && url.pathname === "/v1/cloud/sync/snapshots/content") {
        return cloudSyncSnapshotContent(request, env);
      }

      if (request.method === "GET" && url.pathname === "/v1/licenses/status") {
        return licenseStatus(request, env);
      }

      return json({ ok: false, error: "not_found" }, 404);
    } catch (err) {
      console.error(err);
      return json({ ok: false, error: "internal_error" }, 500);
    }
  },
};


function homikaPurchaseRedirect(requestUrl, env) {
  const configured = cleanString(env.HOMIKA_STORE_URL, 2000);
  if (configured) {
    try {
      const target = new URL(configured);
      if (target.protocol === "https:" || target.protocol === "http:") {
        for (const [name, value] of requestUrl.searchParams.entries()) {
          if (!target.searchParams.has(name)) target.searchParams.set(name, value);
        }
        return Response.redirect(target.toString(), 302);
      }
    } catch (_) {
      // Fall through to the safe placeholder page when the configured URL is invalid.
    }
  }

  const html = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Homika Pro</title>
<style>body{font-family:system-ui,-apple-system,sans-serif;margin:0;background:#f6f6f3;color:#1f2421}main{max-width:560px;margin:0 auto;padding:48px 24px}.card{background:white;border:1px solid #dedfd9;border-radius:22px;padding:28px}h1{margin:0 0 12px;font-size:30px}p{line-height:1.55}.note{color:#5f665f;font-size:14px}</style>
</head>
<body><main><div class="card"><h1>Homika Pro</h1><p>Online checkout is being prepared.</p><p>If you have already renewed an existing licence, return to Homika and tap <strong>Verify now</strong>. Renewal keeps the same licence code.</p><p class="note">Your local Homika data is not deleted when a subscription expires.</p></div></main></body>
</html>`;
  return new Response(html, {
    status: 200,
    headers: {
      "content-type": "text/html; charset=utf-8",
      "cache-control": "no-store",
    },
  });
}

async function publicPlans(env) {
  const result = await env.DB.prepare(
    `SELECT plan_key, name, duration_unit, duration_value, max_devices
       FROM license_plans
      WHERE product_id = ?1
        AND sale_enabled = 1
      ORDER BY sort_order ASC, plan_key ASC`
  ).bind("homika_pro").all();

  return json({
    ok: true,
    plans: (result.results || []).map((plan) => ({
      id: plan.plan_key,
      plan_key: plan.plan_key,
      name: plan.name,
      duration_unit: plan.duration_unit,
      duration_value: plan.duration_value == null ? null : Number(plan.duration_value),
      max_devices: Number(plan.max_devices || 3),
    })),
  });
}


async function storeCatalog(env) {
  const result = await env.DB.prepare(
    `SELECT plan_key, name, duration_unit, duration_value, max_devices,
            price_cents, compare_at_price_cents, currency, is_featured
       FROM license_plans
      WHERE product_id = ?1
        AND sale_enabled = 1
        AND price_cents IS NOT NULL
      ORDER BY sort_order ASC, plan_key ASC`
  ).bind("homika_pro").all();

  const trial = await env.DB.prepare(
    `SELECT plan_key, name, duration_unit, duration_value, max_devices
       FROM license_plans
      WHERE product_id = ?1 AND plan_key = 'trial_7d'
      LIMIT 1`
  ).bind("homika_pro").first();

  return json({
    ok: true,
    product: {
      id: "homika_pro",
      name: "Homika Pro",
      max_devices: 3,
      cloud_sync: true,
      cloud_backup: true,
    },
    launch_promotion: true,
    trial: trial ? {
      plan_key: trial.plan_key,
      name: trial.name,
      duration_unit: trial.duration_unit,
      duration_value: Number(trial.duration_value || 7),
      max_devices: Number(trial.max_devices || 1),
      price_cents: 0,
      claim_in_app: true,
      card_required: false,
      one_per_device: true,
      one_per_customer: true,
    } : null,
    plans: (result.results || []).map((plan) => ({
      plan_key: plan.plan_key,
      name: plan.name,
      duration_unit: plan.duration_unit,
      duration_value: Number(plan.duration_value || 0),
      max_devices: Number(plan.max_devices || 3),
      currency: cleanString(plan.currency, 8) || "MYR",
      price_cents: Number(plan.price_cents || 0),
      compare_at_price_cents: Number(plan.compare_at_price_cents || 0),
      is_featured: Number(plan.is_featured || 0) === 1,
    })),
  });
}

async function claimTrial(request, env) {
  const body = await readJson(request);
  if (!body) return badRequest("invalid_json");

  const email = normalizeEmail(body.email);
  const deviceId = cleanString(body.device_id, 300);
  const deviceName = cleanString(body.device_name, 120);

  if (!email) return badRequest("invalid_email");
  if (!deviceId) return badRequest("device_id_required");

  const deviceHash = await sha256Hex(deviceId);
  const customerHash = await sha256Hex(email);

  const deviceRedemption = await env.DB.prepare(
    `SELECT id, license_id, customer_hash, device_hash
       FROM trial_redemptions
      WHERE product_id = ?1 AND device_hash = ?2
      LIMIT 1`
  ).bind("homika_pro", deviceHash).first();

  const customerRedemption = await env.DB.prepare(
    `SELECT id, license_id, customer_hash, device_hash
       FROM trial_redemptions
      WHERE product_id = ?1 AND customer_hash = ?2
      LIMIT 1`
  ).bind("homika_pro", customerHash).first();

  if (deviceRedemption || customerRedemption) {
    const sameRedemption =
      deviceRedemption && customerRedemption && deviceRedemption.id === customerRedemption.id;

    if (sameRedemption) {
      const existingLicense = await getLicenseById(env, deviceRedemption.license_id);
      if (existingLicense && evaluateLicense(existingLicense).valid) {
        const current = await env.DB.prepare(
          `SELECT id, status
             FROM devices
            WHERE license_id = ?1 AND device_hash = ?2
            LIMIT 1`
        ).bind(existingLicense.id, deviceHash).first();

        if (current) {
          await env.DB.prepare(
            `UPDATE devices
                SET status = 'active', device_name = ?1,
                    last_seen_at = CURRENT_TIMESTAMP, deactivated_at = NULL
              WHERE id = ?2`
          ).bind(deviceName || null, current.id).run();
        } else {
          await env.DB.prepare(
            `INSERT INTO devices (id, license_id, device_hash, device_name, status)
             VALUES (?1, ?2, ?3, ?4, 'active')`
          ).bind(crypto.randomUUID(), existingLicense.id, deviceHash, deviceName || null).run();
        }
        return activationResponse(env, existingLicense, deviceHash, true);
      }

      return json({ ok: false, error: "trial_already_used" }, 409);
    }

    if (deviceRedemption) {
      return json({ ok: false, error: "trial_already_used_device" }, 409);
    }
    return json({ ok: false, error: "trial_already_used_customer" }, 409);
  }

  const plan = await env.DB.prepare(
    `SELECT plan_key, duration_value, max_devices
       FROM license_plans
      WHERE product_id = ?1
        AND plan_key = 'trial_7d'
      LIMIT 1`
  ).bind("homika_pro").first();

  if (!plan || Number(plan.duration_value || 0) !== 7) {
    return json({ ok: false, error: "trial_unavailable" }, 503);
  }

  const customerId = crypto.randomUUID();
  const licenseId = crypto.randomUUID();
  const redemptionId = crypto.randomUUID();
  const licenseKey = generateTrialLicenseKey();
  const expiresAt = formatSqliteTimestamp(Date.now() + (7 * 24 * 60 * 60 * 1000));

  try {
    await env.DB.batch([
      env.DB.prepare(
        `INSERT INTO customers (id, email) VALUES (?1, ?2)`
      ).bind(customerId, email),
      env.DB.prepare(
        `INSERT INTO licenses (
           id, license_key, product_id, customer_id, status,
           expires_at, max_devices, plan_type, plan_key
         ) VALUES (?1, ?2, 'homika_pro', ?3, 'active', ?4, 1, 'trial', 'trial_7d')`
      ).bind(licenseId, licenseKey, customerId, expiresAt),
      env.DB.prepare(
        `INSERT INTO devices (
           id, license_id, device_hash, device_name, status
         ) VALUES (?1, ?2, ?3, ?4, 'active')`
      ).bind(crypto.randomUUID(), licenseId, deviceHash, deviceName || null),
      env.DB.prepare(
        `INSERT INTO trial_redemptions (
           id, product_id, license_id, customer_id,
           customer_hash, device_hash, redeemed_at, expires_at
         ) VALUES (?1, 'homika_pro', ?2, ?3, ?4, ?5, CURRENT_TIMESTAMP, ?6)`
      ).bind(redemptionId, licenseId, customerId, customerHash, deviceHash, expiresAt),
    ]);
  } catch (err) {
    console.warn("Trial claim race or insert failure", err);

    const racedDevice = await env.DB.prepare(
      `SELECT id FROM trial_redemptions
        WHERE product_id = 'homika_pro' AND device_hash = ?1 LIMIT 1`
    ).bind(deviceHash).first();
    if (racedDevice) return json({ ok: false, error: "trial_already_used_device" }, 409);

    const racedCustomer = await env.DB.prepare(
      `SELECT id FROM trial_redemptions
        WHERE product_id = 'homika_pro' AND customer_hash = ?1 LIMIT 1`
    ).bind(customerHash).first();
    if (racedCustomer) return json({ ok: false, error: "trial_already_used_customer" }, 409);

    throw err;
  }

  const license = await getLicenseById(env, licenseId);
  if (!license) throw new Error("Trial licence was not created");
  return activationResponse(env, license, deviceHash, false);
}

async function activateLicense(request, env) {
  const body = await readJson(request);
  if (!body) return badRequest("invalid_json");

  const licenseKey = normalizeLicenseKey(body.license_key);
  const deviceId = cleanString(body.device_id, 300);
  const deviceName = cleanString(body.device_name, 120);

  if (!licenseKey) return badRequest("license_key_required");
  if (!deviceId) return badRequest("device_id_required");

  const license = await getLicenseByKey(env, licenseKey);
  if (!license) return licenseError("license_not_found", 404);

  const state = evaluateLicense(license);
  if (!state.valid) return licenseError(state.error, 403, license);

  const deviceHash = await sha256Hex(deviceId);
  const existing = await env.DB.prepare(
    `SELECT id, status
       FROM devices
      WHERE license_id = ?1 AND device_hash = ?2
      LIMIT 1`
  ).bind(license.id, deviceHash).first();

  if (existing) {
    if (existing.status !== "active") {
      const activeDevices = await countActiveDevices(env, license.id);
      if (activeDevices >= Number(license.max_devices)) {
        return json({
          ok: false,
          error: "device_limit_reached",
          max_devices: Number(license.max_devices),
          active_devices: activeDevices,
        }, 409);
      }
    }

    await env.DB.prepare(
      `UPDATE devices
          SET status = 'active',
              device_name = ?1,
              last_seen_at = CURRENT_TIMESTAMP,
              deactivated_at = NULL
        WHERE id = ?2`
    ).bind(deviceName || null, existing.id).run();

    return activationResponse(env, license, deviceHash, true);
  }

  const activeDevices = await countActiveDevices(env, license.id);
  if (activeDevices >= Number(license.max_devices)) {
    return json({
      ok: false,
      error: "device_limit_reached",
      max_devices: Number(license.max_devices),
      active_devices: activeDevices,
    }, 409);
  }

  const insert = await env.DB.prepare(
    `INSERT INTO devices (
       id, license_id, device_hash, device_name, status
     )
     SELECT ?1, ?2, ?3, ?4, 'active'
     WHERE (
       SELECT COUNT(*)
       FROM devices
       WHERE license_id = ?2 AND status = 'active'
     ) < ?5`
  ).bind(
    crypto.randomUUID(),
    license.id,
    deviceHash,
    deviceName || null,
    Number(license.max_devices),
  ).run();

  if (!insert.meta?.changes) {
    return json({
      ok: false,
      error: "device_limit_reached",
      max_devices: Number(license.max_devices),
      active_devices: await countActiveDevices(env, license.id),
    }, 409);
  }

  return activationResponse(env, license, deviceHash, false);
}

async function validateLicense(request, env) {
  const body = await readJson(request);
  if (!body) return badRequest("invalid_json");

  const activationToken = cleanString(body.activation_token, 6000);
  const deviceId = cleanString(body.device_id, 300);

  if (!activationToken) return badRequest("activation_token_required");
  if (!deviceId) return badRequest("device_id_required");

  const verified = await verifyActivationToken(activationToken);
  if (!verified.ok) {
    return json({ ok: false, error: verified.error }, 403);
  }

  const deviceHash = await sha256Hex(deviceId);
  if (verified.claims.device_hash !== deviceHash) {
    return json({ ok: false, error: "token_device_mismatch" }, 403);
  }

  const license = await getLicenseById(env, verified.claims.license_id);
  if (!license) return licenseError("license_not_found", 404);

  const state = evaluateLicense(license);
  if (!state.valid) return licenseError(state.error, 403, license);

  const device = await env.DB.prepare(
    `SELECT id, status
       FROM devices
      WHERE license_id = ?1 AND device_hash = ?2
      LIMIT 1`
  ).bind(license.id, deviceHash).first();

  if (!device || device.status !== "active") {
    return licenseError("device_not_activated", 403, license);
  }

  await env.DB.prepare(
    `UPDATE devices
        SET last_seen_at = CURRENT_TIMESTAMP
      WHERE id = ?1`
  ).bind(device.id).run();

  return activationResponse(env, license, deviceHash, true);
}

async function deactivateDevice(request, env) {
  const body = await readJson(request);
  if (!body) return badRequest("invalid_json");

  const activationToken = cleanString(body.activation_token, 6000);
  const deviceId = cleanString(body.device_id, 300);

  if (!activationToken) return badRequest("activation_token_required");
  if (!deviceId) return badRequest("device_id_required");

  const verified = await verifyActivationToken(activationToken);
  if (!verified.ok) {
    return json({ ok: false, error: verified.error }, 403);
  }

  const deviceHash = await sha256Hex(deviceId);
  if (verified.claims.device_hash !== deviceHash) {
    return json({ ok: false, error: "token_device_mismatch" }, 403);
  }

  const license = await getLicenseById(env, verified.claims.license_id);
  if (!license) return licenseError("license_not_found", 404);

  const result = await env.DB.prepare(
    `UPDATE devices
        SET status = 'inactive',
            deactivated_at = CURRENT_TIMESTAMP,
            last_seen_at = CURRENT_TIMESTAMP
      WHERE license_id = ?1
        AND device_hash = ?2
        AND status = 'active'`
  ).bind(license.id, deviceHash).run();

  if (!result.meta?.changes) {
    return json({ ok: false, error: "device_not_activated" }, 404);
  }

  await removeDeviceSyncSnapshot(env, license.id, deviceHash);

  return json({
    ok: true,
    status: "deactivated",
    max_devices: Number(license.max_devices),
    active_devices: await countActiveDevices(env, license.id),
  });
}

async function listLicenseDevices(request, env) {
  const auth = await authenticateCloudRequest(request, env);
  if (!auth.ok) return auth.response;

  const result = await env.DB.prepare(
    `SELECT device_hash, device_name, status, activated_at, last_seen_at
       FROM devices
      WHERE license_id = ?1
        AND status = 'active'
      ORDER BY CASE WHEN device_hash = ?2 THEN 0 ELSE 1 END, last_seen_at DESC`
  ).bind(auth.license.id, auth.deviceHash).all();

  const devices = (result.results || []).map((row) => ({
    device_hash: row.device_hash,
    device_name: row.device_name || null,
    status: row.status,
    activated_at: row.activated_at,
    last_seen_at: row.last_seen_at,
    is_current_device: row.device_hash === auth.deviceHash,
  }));

  return json({
    ok: true,
    max_devices: Number(auth.license.max_devices),
    active_devices: devices.length,
    devices,
  });
}

async function deactivateLicenseDevice(request, env) {
  const auth = await authenticateCloudRequest(request, env);
  if (!auth.ok) return auth.response;

  const body = await readJson(request);
  if (!body) return badRequest("invalid_json");

  const targetDeviceHash = cleanString(body.device_hash, 64).toLowerCase();
  if (!/^[a-f0-9]{64}$/.test(targetDeviceHash)) {
    return badRequest("device_hash_required");
  }
  if (targetDeviceHash === auth.deviceHash) {
    return json({ ok: false, error: "current_device_use_self_deactivate" }, 400);
  }

  const result = await env.DB.prepare(
    `UPDATE devices
        SET status = 'inactive',
            deactivated_at = CURRENT_TIMESTAMP,
            last_seen_at = CURRENT_TIMESTAMP
      WHERE license_id = ?1
        AND device_hash = ?2
        AND status = 'active'`
  ).bind(auth.license.id, targetDeviceHash).run();

  if (!result.meta?.changes) {
    return json({ ok: false, error: "device_not_activated" }, 404);
  }

  await removeDeviceSyncSnapshot(env, auth.license.id, targetDeviceHash);

  return json({
    ok: true,
    status: "deactivated",
    device_hash: targetDeviceHash,
    max_devices: Number(auth.license.max_devices),
    active_devices: await countActiveDevices(env, auth.license.id),
  });
}

async function removeDeviceSyncSnapshot(env, licenseId, deviceHash) {
  try {
    const row = await env.DB.prepare(
      `SELECT object_key
         FROM cloud_sync_snapshots
        WHERE license_id = ?1 AND device_hash = ?2`
    ).bind(licenseId, deviceHash).first();

    await env.DB.prepare(
      `DELETE FROM cloud_sync_snapshots
        WHERE license_id = ?1 AND device_hash = ?2`
    ).bind(licenseId, deviceHash).run();

    if (row?.object_key && env.BACKUPS) {
      try {
        await env.BACKUPS.delete(row.object_key);
      } catch (err) {
        console.warn("Could not delete deactivated device sync object", err);
      }
    }
  } catch (err) {
    // Device deactivation must not fail just because optional cloud-sync cleanup failed.
    console.warn("Could not clean deactivated device sync snapshot", err);
  }
}

async function licenseStatus(request, env) {
  const url = new URL(request.url);
  const licenseKey = normalizeLicenseKey(url.searchParams.get("license_key"));
  if (!licenseKey) return badRequest("license_key_required");

  const license = await getLicenseByKey(env, licenseKey);
  if (!license) return licenseError("license_not_found", 404);

  const state = evaluateLicense(license);
  return json({
    ok: true,
    license: {
      product_id: license.product_id,
      plan_type: normalizePlanType(license.plan_type),
      status: license.status,
      valid: state.valid,
      error: state.valid ? null : state.error,
      expires_at: license.expires_at,
      max_devices: Number(license.max_devices),
      active_devices: await countActiveDevices(env, license.id),
    },
  });
}

async function activationResponse(env, license, deviceHash, existingDevice) {
  const token = await createActivationToken(env, license, deviceHash);

  return json({
    ok: true,
    activation: {
      activation_token: token,
      product_id: license.product_id,
      plan_type: normalizePlanType(license.plan_type),
      status: "active",
      expires_at: license.expires_at,
      max_devices: Number(license.max_devices),
      active_devices: await countActiveDevices(env, license.id),
      existing_device: existingDevice,
      license_hint: maskLicenseKey(license.license_key),
    },
  });
}

async function createActivationToken(env, license, deviceHash) {
  const privateKeyB64 = cleanString(env.LICENSE_SIGNING_PRIVATE_KEY, 10000);
  if (!privateKeyB64) throw new Error("LICENSE_SIGNING_PRIVATE_KEY is not configured");

  const planType = normalizePlanType(license.plan_type);
  const expiry = parseSqliteTimestamp(license.expires_at);
  if (!expiry && planType !== "lifetime") throw new Error("Invalid licence expiry");

  const nowSeconds = Math.floor(Date.now() / 1000);
  const entitlementExpirySeconds = planType === "lifetime"
    ? 253402300799
    : Math.floor(expiry.getTime() / 1000);
  const payload = {
    v: 1,
    iss: "app-license-api",
    product_id: license.product_id,
    plan_type: planType,
    license_id: license.id,
    device_hash: deviceHash,
    license_hint: maskLicenseKey(license.license_key),
    max_devices: Number(license.max_devices),
    iat: nowSeconds,
    license_exp: entitlementExpirySeconds,
    exp: nowSeconds + TOKEN_LIFETIME_SECONDS,
    jti: crypto.randomUUID(),
  };

  const headerPart = base64UrlEncodeUtf8(JSON.stringify(TOKEN_HEADER));
  const payloadPart = base64UrlEncodeUtf8(JSON.stringify(payload));
  const signingInput = `${headerPart}.${payloadPart}`;

  const privateKey = await crypto.subtle.importKey(
    "pkcs8",
    base64ToBytes(privateKeyB64),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    privateKey,
    new TextEncoder().encode(signingInput),
  );

  return `${signingInput}.${base64UrlEncodeBytes(new Uint8Array(signature))}`;
}

async function verifyActivationToken(token) {
  const parts = token.split(".");
  if (parts.length !== 3) return { ok: false, error: "invalid_activation_token" };

  try {
    const header = JSON.parse(base64UrlDecodeUtf8(parts[0]));
    if (header.alg !== "RS256" || header.typ !== "HAT" || Number(header.v) !== 1) {
      return { ok: false, error: "invalid_activation_token" };
    }

    const publicKey = await crypto.subtle.importKey(
      "spki",
      base64ToBytes(PUBLIC_SIGNING_KEY_B64),
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["verify"],
    );

    const validSignature = await crypto.subtle.verify(
      "RSASSA-PKCS1-v1_5",
      publicKey,
      base64UrlToBytes(parts[2]),
      new TextEncoder().encode(`${parts[0]}.${parts[1]}`),
    );

    if (!validSignature) return { ok: false, error: "invalid_activation_token" };

    const claims = JSON.parse(base64UrlDecodeUtf8(parts[1]));
    if (
      Number(claims.v) !== 1 ||
      claims.iss !== "app-license-api" ||
      claims.product_id !== "homika_pro" ||
      typeof claims.license_id !== "string" ||
      typeof claims.device_hash !== "string" ||
      !Number.isFinite(Number(claims.exp)) ||
      !Number.isFinite(Number(claims.license_exp))
    ) {
      return { ok: false, error: "invalid_activation_token" };
    }

    if (Math.floor(Date.now() / 1000) >= Number(claims.exp)) {
      return { ok: false, error: "license_expired" };
    }

    return { ok: true, claims };
  } catch {
    return { ok: false, error: "invalid_activation_token" };
  }
}


async function cloudKey(request, env) {
  const auth = await authenticateCloudRequest(request, env);
  if (!auth.ok) return auth.response;

  const keyBytes = await deriveCloudVaultKey(env, auth.license.id);
  return json({
    ok: true,
    cloud: {
      key_version: 1,
      key_b64: bytesToBase64(keyBytes),
    },
  });
}

async function uploadCloudBackup(request, env) {
  const auth = await authenticateCloudRequest(request, env);
  if (!auth.ok) return auth.response;
  if (!env.BACKUPS) return json({ ok: false, error: "cloud_storage_not_configured" }, 503);

  const raw = new Uint8Array(await request.arrayBuffer());
  if (raw.byteLength <= 0) return badRequest("backup_payload_required");
  if (raw.byteLength > MAX_CLOUD_BACKUP_BYTES) {
    return json({ ok: false, error: "backup_too_large", max_bytes: MAX_CLOUD_BACKUP_BYTES }, 413);
  }

  const createdAtMillis = positiveHeaderInt(request, "x-homika-backup-created-at") || Date.now();
  const recordCount = nonNegativeHeaderInt(request, "x-homika-record-count");
  const formatVersion = positiveHeaderInt(request, "x-homika-format-version") || 1;
  const databaseSchemaVersion = positiveHeaderInt(request, "x-homika-database-schema-version") || 1;
  const sha256 = await sha256HexBytes(raw);
  const backupId = crypto.randomUUID();
  const objectKey = `homika_pro/${auth.license.id}/${backupId}.hcb`;

  await env.BACKUPS.put(objectKey, raw, {
    httpMetadata: { contentType: "application/octet-stream" },
    customMetadata: {
      backupId,
      licenseId: auth.license.id,
      createdAtMillis: String(createdAtMillis),
      sha256,
    },
  });

  try {
    await env.DB.prepare(
      `INSERT INTO cloud_backups (
         id, license_id, device_hash, object_key,
         created_at_epoch_millis, record_count,
         format_version, database_schema_version,
         byte_size, sha256, status
       ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, 'ready')`
    ).bind(
      backupId,
      auth.license.id,
      auth.deviceHash,
      objectKey,
      createdAtMillis,
      recordCount,
      formatVersion,
      databaseSchemaVersion,
      raw.byteLength,
      sha256,
    ).run();
  } catch (error) {
    await env.BACKUPS.delete(objectKey);
    throw error;
  }

  await pruneCloudBackups(env, auth.license.id);
  const metadata = await getCloudBackupById(env, backupId);

  return json({ ok: true, backup: cloudBackupJson(metadata) }, 201);
}

async function latestCloudBackup(request, env) {
  const auth = await authenticateCloudRequest(request, env);
  if (!auth.ok) return auth.response;

  const backup = await getLatestCloudBackup(env, auth.license.id);
  return json({
    ok: true,
    backup: backup ? cloudBackupJson(backup) : null,
  });
}

async function latestCloudBackupContent(request, env) {
  const auth = await authenticateCloudRequest(request, env);
  if (!auth.ok) return auth.response;
  if (!env.BACKUPS) return json({ ok: false, error: "cloud_storage_not_configured" }, 503);

  const backup = await getLatestCloudBackup(env, auth.license.id);
  if (!backup) return json({ ok: false, error: "cloud_backup_not_found" }, 404);

  const object = await env.BACKUPS.get(backup.object_key);
  if (!object) {
    await env.DB.prepare(
      `UPDATE cloud_backups SET status = 'missing' WHERE id = ?1`
    ).bind(backup.id).run();
    return json({ ok: false, error: "cloud_backup_missing" }, 404);
  }

  return new Response(object.body, {
    status: 200,
    headers: {
      "content-type": "application/octet-stream",
      "content-length": String(backup.byte_size),
      "x-homika-backup-id": backup.id,
      "x-homika-backup-sha256": backup.sha256,
      "cache-control": "no-store",
      ...corsHeaders(),
    },
  });
}

async function authenticateCloudRequest(request, env) {
  const authorization = cleanString(request.headers.get("authorization"), 7000);
  const deviceId = cleanString(request.headers.get("x-homika-device-id"), 300);
  if (!authorization.toLowerCase().startsWith("bearer ")) {
    return { ok: false, response: json({ ok: false, error: "activation_token_required" }, 401) };
  }
  if (!deviceId) {
    return { ok: false, response: json({ ok: false, error: "device_id_required" }, 400) };
  }

  const activationToken = authorization.slice(7).trim();
  const verified = await verifyActivationToken(activationToken);
  if (!verified.ok) {
    return { ok: false, response: json({ ok: false, error: verified.error }, 403) };
  }

  const deviceHash = await sha256Hex(deviceId);
  if (verified.claims.device_hash !== deviceHash) {
    return { ok: false, response: json({ ok: false, error: "token_device_mismatch" }, 403) };
  }

  const license = await getLicenseById(env, verified.claims.license_id);
  if (!license) {
    return { ok: false, response: licenseError("license_not_found", 404) };
  }

  const state = evaluateLicense(license);
  if (!state.valid) {
    return { ok: false, response: licenseError(state.error, 403, license) };
  }

  const device = await env.DB.prepare(
    `SELECT id, status
       FROM devices
      WHERE license_id = ?1 AND device_hash = ?2
      LIMIT 1`
  ).bind(license.id, deviceHash).first();

  if (!device || device.status !== "active") {
    return { ok: false, response: licenseError("device_not_activated", 403, license) };
  }

  await env.DB.prepare(
    `UPDATE devices SET last_seen_at = CURRENT_TIMESTAMP WHERE id = ?1`
  ).bind(device.id).run();

  return { ok: true, license, deviceHash };
}

async function deriveCloudVaultKey(env, licenseId) {
  const masterB64 = cleanString(env.CLOUD_MASTER_KEY, 10000);
  if (!masterB64) throw new Error("CLOUD_MASTER_KEY is not configured");
  const master = base64ToBytes(masterB64);
  if (master.byteLength < 32) throw new Error("CLOUD_MASTER_KEY must be at least 32 bytes");

  const key = await crypto.subtle.importKey(
    "raw",
    master,
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    "HMAC",
    key,
    new TextEncoder().encode(`homika-cloud-v1|${licenseId}`),
  );
  return new Uint8Array(signature);
}

async function getLatestCloudBackup(env, licenseId) {
  return env.DB.prepare(
    `SELECT id, license_id, device_hash, object_key,
            created_at_epoch_millis, record_count,
            format_version, database_schema_version,
            byte_size, sha256, created_at
       FROM cloud_backups
      WHERE license_id = ?1 AND status = 'ready'
      ORDER BY created_at_epoch_millis DESC, created_at DESC
      LIMIT 1`
  ).bind(licenseId).first();
}

async function getCloudBackupById(env, backupId) {
  return env.DB.prepare(
    `SELECT id, license_id, device_hash, object_key,
            created_at_epoch_millis, record_count,
            format_version, database_schema_version,
            byte_size, sha256, created_at
       FROM cloud_backups
      WHERE id = ?1
      LIMIT 1`
  ).bind(backupId).first();
}

function cloudBackupJson(backup) {
  return {
    id: backup.id,
    created_at_epoch_millis: Number(backup.created_at_epoch_millis),
    record_count: Number(backup.record_count || 0),
    format_version: Number(backup.format_version || 1),
    database_schema_version: Number(backup.database_schema_version || 1),
    byte_size: Number(backup.byte_size || 0),
    sha256: backup.sha256,
    uploaded_at: backup.created_at,
  };
}

async function pruneCloudBackups(env, licenseId) {
  const old = await env.DB.prepare(
    `SELECT id, object_key
       FROM cloud_backups
      WHERE license_id = ?1 AND status = 'ready'
      ORDER BY created_at_epoch_millis DESC, created_at DESC
      LIMIT 100 OFFSET ?2`
  ).bind(licenseId, CLOUD_BACKUP_RETENTION).all();

  for (const row of old.results || []) {
    if (env.BACKUPS) await env.BACKUPS.delete(row.object_key);
    await env.DB.prepare(`DELETE FROM cloud_backups WHERE id = ?1`).bind(row.id).run();
  }
}

function positiveHeaderInt(request, name) {
  const value = Number.parseInt(request.headers.get(name) || "", 10);
  return Number.isFinite(value) && value > 0 ? value : 0;
}

function nonNegativeHeaderInt(request, name) {
  const value = Number.parseInt(request.headers.get(name) || "", 10);
  return Number.isFinite(value) && value >= 0 ? value : 0;
}

async function sha256HexBytes(bytes) {
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return [...new Uint8Array(digest)]
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

function bytesToBase64(bytes) {
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary);
}

const MAX_CLOUD_BACKUP_BYTES = 10 * 1024 * 1024;
const CLOUD_BACKUP_RETENTION = 5;



const CLOUD_SYNC_SNAPSHOT_MAX_BYTES = 10 * 1024 * 1024;

async function listCloudSyncSnapshots(request, env) {
  const auth = await authenticateCloudRequest(request, env);
  if (!auth.ok) return auth.response;

  const result = await env.DB.prepare(
    `SELECT device_hash, object_key, updated_at_epoch_millis, record_count,
            format_version, database_schema_version, byte_size, sha256,
            content_sha256, updated_at
       FROM cloud_sync_snapshots
      WHERE license_id = ?1 AND status = 'ready'
      ORDER BY updated_at_epoch_millis DESC, updated_at DESC`
  ).bind(auth.license.id).all();

  return json({
    ok: true,
    protocol: 2,
    mode: "encrypted_device_snapshots",
    snapshots: (result.results || []).map((row) =>
      cloudSyncSnapshotJson(row, row.device_hash === auth.deviceHash)
    ),
  });
}

async function uploadCloudSyncSnapshot(request, env) {
  const auth = await authenticateCloudRequest(request, env);
  if (!auth.ok) return auth.response;
  if (!env.BACKUPS) return json({ ok: false, error: "cloud_storage_not_configured" }, 503);

  const raw = new Uint8Array(await request.arrayBuffer());
  if (raw.byteLength <= 0) return badRequest("sync_snapshot_payload_required");
  if (raw.byteLength > CLOUD_SYNC_SNAPSHOT_MAX_BYTES) {
    return json({
      ok: false,
      error: "sync_snapshot_too_large",
      max_bytes: CLOUD_SYNC_SNAPSHOT_MAX_BYTES,
    }, 413);
  }

  const contentSha256 = cleanString(request.headers.get("x-homika-content-sha256"), 64).toLowerCase();
  if (!/^[a-f0-9]{64}$/.test(contentSha256)) {
    return badRequest("sync_snapshot_content_sha256_required");
  }

  const updatedAtMillis = positiveHeaderInt(request, "x-homika-sync-updated-at") || Date.now();
  const recordCount = nonNegativeHeaderInt(request, "x-homika-record-count");
  const formatVersion = positiveHeaderInt(request, "x-homika-format-version") || 1;
  const databaseSchemaVersion = positiveHeaderInt(request, "x-homika-database-schema-version") || 1;
  const sha256 = await sha256HexBytes(raw);

  const previous = await env.DB.prepare(
    `SELECT object_key
       FROM cloud_sync_snapshots
      WHERE license_id = ?1 AND device_hash = ?2
      LIMIT 1`
  ).bind(auth.license.id, auth.deviceHash).first();

  const objectKey = `homika_pro/${auth.license.id}/sync/${auth.deviceHash}/${crypto.randomUUID()}.hcs`;
  await env.BACKUPS.put(objectKey, raw, {
    httpMetadata: { contentType: "application/octet-stream" },
    customMetadata: {
      licenseId: auth.license.id,
      deviceHash: auth.deviceHash,
      contentSha256,
      updatedAtMillis: String(updatedAtMillis),
      sha256,
    },
  });

  try {
    await env.DB.prepare(
      `INSERT INTO cloud_sync_snapshots (
         license_id, device_hash, object_key, updated_at_epoch_millis,
         record_count, format_version, database_schema_version,
         byte_size, sha256, content_sha256, status, created_at, updated_at
       ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, 'ready', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
       ON CONFLICT(license_id, device_hash) DO UPDATE SET
         object_key = excluded.object_key,
         updated_at_epoch_millis = excluded.updated_at_epoch_millis,
         record_count = excluded.record_count,
         format_version = excluded.format_version,
         database_schema_version = excluded.database_schema_version,
         byte_size = excluded.byte_size,
         sha256 = excluded.sha256,
         content_sha256 = excluded.content_sha256,
         status = 'ready',
         updated_at = CURRENT_TIMESTAMP`
    ).bind(
      auth.license.id,
      auth.deviceHash,
      objectKey,
      updatedAtMillis,
      recordCount,
      formatVersion,
      databaseSchemaVersion,
      raw.byteLength,
      sha256,
      contentSha256,
    ).run();
  } catch (error) {
    await env.BACKUPS.delete(objectKey);
    throw error;
  }

  if (previous?.object_key && previous.object_key !== objectKey) {
    try {
      await env.BACKUPS.delete(previous.object_key);
    } catch (error) {
      console.warn("Could not prune previous sync snapshot", error);
    }
  }

  const row = await getCloudSyncSnapshot(env, auth.license.id, auth.deviceHash);
  return json({ ok: true, snapshot: cloudSyncSnapshotJson(row, true) });
}

async function cloudSyncSnapshotContent(request, env) {
  const auth = await authenticateCloudRequest(request, env);
  if (!auth.ok) return auth.response;
  if (!env.BACKUPS) return json({ ok: false, error: "cloud_storage_not_configured" }, 503);

  const url = new URL(request.url);
  const deviceHash = cleanString(url.searchParams.get("device_hash"), 64).toLowerCase();
  if (!/^[a-f0-9]{64}$/.test(deviceHash)) return badRequest("device_hash_required");

  const snapshot = await getCloudSyncSnapshot(env, auth.license.id, deviceHash);
  if (!snapshot || snapshot.status !== "ready") {
    return json({ ok: false, error: "sync_snapshot_not_found" }, 404);
  }

  const object = await env.BACKUPS.get(snapshot.object_key);
  if (!object) {
    await env.DB.prepare(
      `UPDATE cloud_sync_snapshots
          SET status = 'missing', updated_at = CURRENT_TIMESTAMP
        WHERE license_id = ?1 AND device_hash = ?2`
    ).bind(auth.license.id, deviceHash).run();
    return json({ ok: false, error: "sync_snapshot_missing" }, 404);
  }

  return new Response(object.body, {
    status: 200,
    headers: {
      "content-type": "application/octet-stream",
      "content-length": String(snapshot.byte_size),
      "x-homika-device-hash": snapshot.device_hash,
      "x-homika-sync-sha256": snapshot.sha256,
      "x-homika-content-sha256": snapshot.content_sha256,
      "cache-control": "no-store",
      ...corsHeaders(),
    },
  });
}

async function getCloudSyncSnapshot(env, licenseId, deviceHash) {
  return env.DB.prepare(
    `SELECT license_id, device_hash, object_key, updated_at_epoch_millis,
            record_count, format_version, database_schema_version,
            byte_size, sha256, content_sha256, status, created_at, updated_at
       FROM cloud_sync_snapshots
      WHERE license_id = ?1 AND device_hash = ?2
      LIMIT 1`
  ).bind(licenseId, deviceHash).first();
}

function cloudSyncSnapshotJson(row, isCurrentDevice) {
  return {
    device_hash: row.device_hash,
    updated_at_epoch_millis: Number(row.updated_at_epoch_millis || 0),
    record_count: Number(row.record_count || 0),
    format_version: Number(row.format_version || 1),
    database_schema_version: Number(row.database_schema_version || 1),
    byte_size: Number(row.byte_size || 0),
    sha256: row.sha256,
    content_sha256: row.content_sha256,
    is_current_device: Boolean(isCurrentDevice),
  };
}

const CLOUD_SYNC_PROTOCOL = 1;
const CLOUD_SYNC_MAX_BATCH = 100;
const CLOUD_SYNC_MAX_PULL = 200;
const CLOUD_SYNC_MAX_PAYLOAD_BYTES = 256 * 1024;
const CLOUD_SYNC_ALLOWED_TYPES = new Set([
  "property",
  "booking",
  "payment",
  "deposit",
  "expense",
  "blocked_date",
]);

async function pushCloudSync(request, env) {
  const auth = await authenticateCloudRequest(request, env);
  if (!auth.ok) return auth.response;

  const body = await readJson(request);
  if (!body) return badRequest("invalid_json");
  if (Number(body.protocol || CLOUD_SYNC_PROTOCOL) !== CLOUD_SYNC_PROTOCOL) {
    return badRequest("unsupported_sync_protocol");
  }

  const rawChanges = Array.isArray(body.changes) ? body.changes : null;
  if (!rawChanges) return badRequest("sync_changes_required");
  if (rawChanges.length > CLOUD_SYNC_MAX_BATCH) {
    return json({ ok: false, error: "sync_batch_too_large", max_changes: CLOUD_SYNC_MAX_BATCH }, 413);
  }

  const accepted = [];
  const conflicts = [];

  for (let index = 0; index < rawChanges.length; index += 1) {
    const parsed = parseSyncChange(rawChanges[index]);
    if (!parsed.ok) {
      return json({ ok: false, error: parsed.error, change_index: index }, 400);
    }
    const change = parsed.change;

    let payloadBytes;
    try {
      payloadBytes = base64ToBytes(change.payload_b64);
    } catch {
      return json({ ok: false, error: "invalid_sync_payload", change_index: index }, 400);
    }
    if (payloadBytes.byteLength <= 0 || payloadBytes.byteLength > CLOUD_SYNC_MAX_PAYLOAD_BYTES) {
      return json({
        ok: false,
        error: "sync_payload_too_large",
        change_index: index,
        max_bytes: CLOUD_SYNC_MAX_PAYLOAD_BYTES,
      }, 413);
    }

    const encryptedSha256 = await sha256HexBytes(payloadBytes);
    const current = await getCloudSyncItem(
      env,
      auth.license.id,
      change.entity_type,
      change.entity_id,
    );

    if (
      current &&
      Number(current.revision) === change.revision &&
      current.content_sha256 === change.content_sha256
    ) {
      accepted.push({
        entity_type: change.entity_type,
        entity_id: change.entity_id,
        revision: change.revision,
        server_sequence: Number(current.server_sequence || 0),
        duplicate: true,
      });
      continue;
    }

    if (current) {
      if (Number(current.revision) !== change.base_revision || change.revision <= change.base_revision) {
        conflicts.push(syncConflictJson(change, current, "revision_conflict"));
        continue;
      }
    } else if (change.base_revision !== 0) {
      conflicts.push(syncConflictJson(change, null, "server_item_missing"));
      continue;
    }

    const eventId = crypto.randomUUID();
    await env.DB.prepare(
      `INSERT INTO cloud_sync_events (
         id, license_id, entity_type, entity_id,
         revision, base_revision, updated_at_epoch_millis, is_deleted,
         payload_b64, payload_sha256, content_sha256,
         source_device_hash, status
       ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, 'pending')`
    ).bind(
      eventId,
      auth.license.id,
      change.entity_type,
      change.entity_id,
      change.revision,
      change.base_revision,
      change.updated_at_epoch_millis,
      change.is_deleted ? 1 : 0,
      change.payload_b64,
      encryptedSha256,
      change.content_sha256,
      auth.deviceHash,
    ).run();

    const event = await env.DB.prepare(
      `SELECT sequence FROM cloud_sync_events WHERE id = ?1 LIMIT 1`
    ).bind(eventId).first();
    const sequence = Number(event?.sequence || 0);
    if (sequence <= 0) {
      await env.DB.prepare(`DELETE FROM cloud_sync_events WHERE id = ?1`).bind(eventId).run();
      throw new Error("Failed to reserve sync sequence");
    }

    let writeResult;
    if (current) {
      writeResult = await env.DB.prepare(
        `UPDATE cloud_sync_items
            SET revision = ?1,
                updated_at_epoch_millis = ?2,
                is_deleted = ?3,
                payload_b64 = ?4,
                payload_sha256 = ?5,
                content_sha256 = ?6,
                source_device_hash = ?7,
                server_sequence = ?8,
                updated_at = CURRENT_TIMESTAMP
          WHERE license_id = ?9
            AND entity_type = ?10
            AND entity_id = ?11
            AND revision = ?12
            AND content_sha256 = ?13`
      ).bind(
        change.revision,
        change.updated_at_epoch_millis,
        change.is_deleted ? 1 : 0,
        change.payload_b64,
        encryptedSha256,
        change.content_sha256,
        auth.deviceHash,
        sequence,
        auth.license.id,
        change.entity_type,
        change.entity_id,
        change.base_revision,
        current.content_sha256,
      ).run();
    } else {
      writeResult = await env.DB.prepare(
        `INSERT OR IGNORE INTO cloud_sync_items (
           license_id, entity_type, entity_id, revision,
           updated_at_epoch_millis, is_deleted,
           payload_b64, payload_sha256, content_sha256,
           source_device_hash, server_sequence
         ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11)`
      ).bind(
        auth.license.id,
        change.entity_type,
        change.entity_id,
        change.revision,
        change.updated_at_epoch_millis,
        change.is_deleted ? 1 : 0,
        change.payload_b64,
        encryptedSha256,
        change.content_sha256,
        auth.deviceHash,
        sequence,
      ).run();
    }

    if (!writeResult.meta?.changes) {
      await env.DB.prepare(`DELETE FROM cloud_sync_events WHERE id = ?1`).bind(eventId).run();
      const latest = await getCloudSyncItem(
        env,
        auth.license.id,
        change.entity_type,
        change.entity_id,
      );
      conflicts.push(syncConflictJson(change, latest, "concurrent_update"));
      continue;
    }

    await env.DB.prepare(
      `UPDATE cloud_sync_events SET status = 'ready' WHERE id = ?1`
    ).bind(eventId).run();

    accepted.push({
      entity_type: change.entity_type,
      entity_id: change.entity_id,
      revision: change.revision,
      server_sequence: sequence,
      duplicate: false,
    });
  }

  return json({
    ok: true,
    protocol: CLOUD_SYNC_PROTOCOL,
    accepted,
    conflicts,
  });
}

async function pullCloudSync(request, env) {
  const auth = await authenticateCloudRequest(request, env);
  if (!auth.ok) return auth.response;

  const url = new URL(request.url);
  const cursorRaw = Number.parseInt(url.searchParams.get("cursor") || "0", 10);
  const limitRaw = Number.parseInt(url.searchParams.get("limit") || "100", 10);
  const cursor = Number.isFinite(cursorRaw) && cursorRaw >= 0 ? cursorRaw : 0;
  const limit = Math.min(
    CLOUD_SYNC_MAX_PULL,
    Math.max(1, Number.isFinite(limitRaw) ? limitRaw : 100),
  );

  const result = await env.DB.prepare(
    `SELECT sequence, entity_type, entity_id, revision,
            updated_at_epoch_millis, is_deleted,
            payload_b64, payload_sha256, content_sha256,
            source_device_hash, created_at
       FROM cloud_sync_events
      WHERE license_id = ?1
        AND status = 'ready'
        AND sequence > ?2
      ORDER BY sequence ASC
      LIMIT ?3`
  ).bind(auth.license.id, cursor, limit + 1).all();

  const rows = result.results || [];
  const hasMore = rows.length > limit;
  const selected = hasMore ? rows.slice(0, limit) : rows;
  const nextCursor = selected.length > 0
    ? Number(selected[selected.length - 1].sequence)
    : cursor;

  return json({
    ok: true,
    protocol: CLOUD_SYNC_PROTOCOL,
    cursor,
    next_cursor: nextCursor,
    has_more: hasMore,
    changes: selected.map(syncEventJson),
  });
}

function parseSyncChange(value) {
  if (!value || typeof value !== "object") return { ok: false, error: "invalid_sync_change" };

  const entityType = cleanString(value.entity_type, 40).toLowerCase();
  const entityId = cleanString(value.entity_id, 120);
  const revision = Number(value.revision);
  const baseRevision = Number(value.base_revision);
  const updatedAt = Number(value.updated_at_epoch_millis);
  const payloadB64 = cleanString(value.payload_b64, 400000);
  const contentSha256 = cleanString(value.content_sha256, 64).toLowerCase();

  if (!CLOUD_SYNC_ALLOWED_TYPES.has(entityType)) return { ok: false, error: "invalid_sync_entity_type" };
  if (!entityId) return { ok: false, error: "sync_entity_id_required" };
  if (!Number.isSafeInteger(revision) || revision < 0) return { ok: false, error: "invalid_sync_revision" };
  if (!Number.isSafeInteger(baseRevision) || baseRevision < 0) return { ok: false, error: "invalid_sync_base_revision" };
  if (!Number.isSafeInteger(updatedAt) || updatedAt <= 0) return { ok: false, error: "invalid_sync_updated_at" };
  if (!payloadB64) return { ok: false, error: "sync_payload_required" };
  if (!/^[a-f0-9]{64}$/.test(contentSha256)) return { ok: false, error: "invalid_sync_content_sha256" };

  return {
    ok: true,
    change: {
      entity_type: entityType,
      entity_id: entityId,
      revision,
      base_revision: baseRevision,
      updated_at_epoch_millis: updatedAt,
      is_deleted: value.is_deleted === true || Number(value.is_deleted) === 1,
      payload_b64: payloadB64,
      content_sha256: contentSha256,
    },
  };
}

async function getCloudSyncItem(env, licenseId, entityType, entityId) {
  return env.DB.prepare(
    `SELECT license_id, entity_type, entity_id, revision,
            updated_at_epoch_millis, is_deleted,
            payload_b64, payload_sha256, content_sha256,
            source_device_hash, server_sequence, updated_at
       FROM cloud_sync_items
      WHERE license_id = ?1
        AND entity_type = ?2
        AND entity_id = ?3
      LIMIT 1`
  ).bind(licenseId, entityType, entityId).first();
}

function syncEventJson(row) {
  return {
    server_sequence: Number(row.sequence),
    entity_type: row.entity_type,
    entity_id: row.entity_id,
    revision: Number(row.revision),
    updated_at_epoch_millis: Number(row.updated_at_epoch_millis),
    is_deleted: Number(row.is_deleted) === 1,
    payload_b64: row.payload_b64,
    payload_sha256: row.payload_sha256,
    content_sha256: row.content_sha256,
    source_device_hash: row.source_device_hash,
    created_at: row.created_at,
  };
}

function syncItemJson(row) {
  if (!row) return null;
  return {
    server_sequence: Number(row.server_sequence || 0),
    entity_type: row.entity_type,
    entity_id: row.entity_id,
    revision: Number(row.revision),
    updated_at_epoch_millis: Number(row.updated_at_epoch_millis),
    is_deleted: Number(row.is_deleted) === 1,
    payload_b64: row.payload_b64,
    payload_sha256: row.payload_sha256,
    content_sha256: row.content_sha256,
    source_device_hash: row.source_device_hash,
    updated_at: row.updated_at,
  };
}

function syncConflictJson(incoming, current, reason) {
  return {
    entity_type: incoming.entity_type,
    entity_id: incoming.entity_id,
    local_revision: incoming.revision,
    base_revision: incoming.base_revision,
    reason,
    current: syncItemJson(current),
  };
}

async function getLicenseByKey(env, licenseKey) {
  return env.DB.prepare(
    `SELECT id, license_key, product_id, customer_id, status, plan_type, expires_at, max_devices
       FROM licenses
      WHERE license_key = ?1
      LIMIT 1`
  ).bind(licenseKey).first();
}

async function getLicenseById(env, licenseId) {
  return env.DB.prepare(
    `SELECT id, license_key, product_id, customer_id, status, plan_type, expires_at, max_devices
       FROM licenses
      WHERE id = ?1
      LIMIT 1`
  ).bind(licenseId).first();
}

async function countActiveDevices(env, licenseId) {
  const row = await env.DB.prepare(
    `SELECT COUNT(*) AS total
       FROM devices
      WHERE license_id = ?1 AND status = 'active'`
  ).bind(licenseId).first();

  return Number(row?.total || 0);
}

function evaluateLicense(license) {
  if (license.status !== "active") {
    return { valid: false, error: "license_inactive" };
  }

  const planType = normalizePlanType(license.plan_type);
  if (planType === "lifetime") {
    return { valid: true };
  }

  const expiry = parseSqliteTimestamp(license.expires_at);
  if (!expiry || expiry.getTime() <= Date.now()) {
    return { valid: false, error: "license_expired" };
  }

  return { valid: true };
}

function parseSqliteTimestamp(value) {
  if (!value) return null;
  const raw = String(value).trim();
  const date = new Date(raw.includes("T") ? raw : raw.replace(" ", "T") + "Z");
  return Number.isNaN(date.getTime()) ? null : date;
}

function maskLicenseKey(value) {
  const key = normalizeLicenseKey(value);
  if (!key) return "••••";
  const last = key.replace(/-/g, "").slice(-4);
  return `••••-${last}`;
}

function normalizeLicenseKey(value) {
  const raw = cleanString(value, 80);
  if (!raw) return "";
  return raw.toUpperCase().replace(/\s+/g, "").replace(/[^A-Z0-9-]/g, "");
}


function normalizeEmail(value) {
  const email = cleanString(value, 254).toLowerCase();
  if (!email) return "";
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return "";
  return email;
}

function generateTrialLicenseKey() {
  const bytes = new Uint8Array(10);
  crypto.getRandomValues(bytes);
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let token = "";
  for (const byte of bytes) token += alphabet[byte % alphabet.length];
  return `HMK-TRIAL-${token.slice(0, 5)}-${token.slice(5, 10)}`;
}

function formatSqliteTimestamp(epochMillis) {
  return new Date(epochMillis).toISOString().replace("T", " ").replace(/\.\d{3}Z$/, "");
}

function normalizePlanType(value) {
  const normalized = cleanString(value, 20).toLowerCase();
  return ["trial", "monthly", "annual", "lifetime"].includes(normalized)
    ? normalized
    : "annual";
}

function cleanString(value, maxLength) {
  if (typeof value !== "string") return "";
  const trimmed = value.trim();
  return trimmed ? trimmed.slice(0, maxLength) : "";
}

async function sha256Hex(value) {
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(value),
  );
  return [...new Uint8Array(digest)]
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

function base64ToBytes(value) {
  const binary = atob(value.replace(/\s+/g, ""));
  return Uint8Array.from(binary, (c) => c.charCodeAt(0));
}

function base64UrlEncodeUtf8(value) {
  return base64UrlEncodeBytes(new TextEncoder().encode(value));
}

function base64UrlEncodeBytes(bytes) {
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function base64UrlToBytes(value) {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4);
  return base64ToBytes(padded);
}

function base64UrlDecodeUtf8(value) {
  return new TextDecoder().decode(base64UrlToBytes(value));
}

async function readJson(request) {
  const type = request.headers.get("content-type") || "";
  if (!type.toLowerCase().includes("application/json")) return null;
  try {
    return await request.json();
  } catch {
    return null;
  }
}

function badRequest(error) {
  return json({ ok: false, error }, 400);
}

function licenseError(error, status, license = null) {
  return json({
    ok: false,
    error,
    ...(license ? {
      plan_type: normalizePlanType(license.plan_type),
      expires_at: license.expires_at,
      max_devices: Number(license.max_devices),
    } : {}),
  }, status);
}

function json(data, status = 200) {
  return new Response(JSON.stringify(data, null, 2), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      ...corsHeaders(),
    },
  });
}

function corsHeaders() {
  return {
    "access-control-allow-origin": "*",
    "access-control-allow-methods": "GET,POST,OPTIONS",
    "access-control-allow-headers": "content-type,authorization,x-homika-device-id,x-homika-backup-created-at,x-homika-record-count,x-homika-format-version,x-homika-database-schema-version",
    "cache-control": "no-store",
  };
}
