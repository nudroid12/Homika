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
          version: 4,
          signed_tokens: true,
          license_plans: true,
          cloud_backup: true,
          cloud_backup_retention: 5,
        });
      }

      if (request.method === "GET" && url.pathname === "/v1/plans") {
        return publicPlans(env);
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

  return json({
    ok: true,
    status: "deactivated",
    max_devices: Number(license.max_devices),
    active_devices: await countActiveDevices(env, license.id),
  });
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
