const PUBLIC_SIGNING_KEY_B64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsyzhDrq517vuIzP99flbfNSEPzrocJH/Dqlp07CNR+vNYadLpqpsVKGV+3SIBtF9ytcV6gB00d6dIVbfL5ORS0YY+XgKQhGjHAZ9/AWk1VqUCvXtavrZWA0kUMNy5kImzdtX/0cMclqH9WpC4kQxcsCgjpQp80mhdK3db1zmHsdi/4fH7Kxgcz1NTzFM3/8fLVXg1KdHw356vGmjJRoAxG8rg4rbymmgIRwFYnKUbyrG9xL4iBJ/J+D4zR5+DxQ3UCRKg5/576epGuWqkHARjxcR4IE1NEfsRHyqiRT4gXRoPdJfgSWB7nIGQ9Qvc8az6JQs4c7dV0wsMhUQ00XaewIDAQAB";
const TOKEN_HEADER = { alg: "RS256", typ: "HAT", v: 1 };
const TOKEN_LIFETIME_SECONDS = 5 * 365 * 24 * 60 * 60;
const DEFAULT_HOMIKA_STORE_URL = "https://homika-store.pages.dev/";

export default {
  async fetch(request, env, ctx) {
    try {
      const url = new URL(request.url);

      if (request.method === "OPTIONS") {
        return new Response(null, { status: 204, headers: corsHeaders() });
      }

      if (request.method === "GET" && url.pathname === "/health") {
        return json({
          ok: true,
          service: "app-license-api",
          version: 18,
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
          persistent_store_url_default: true,
          store_catalog: true,
          checkout_intents: true,
          authenticated_renewal_checkout: true,
          payment_webhook_foundation: true,
          manual_qr_payment: true,
          manual_payment_proof: true,
          admin_payment_approval: true,
          admin_payment_notification: Boolean(cleanString(env.HOMIKA_ADMIN_TELEGRAM_BOT_TOKEN, 300) && cleanString(env.HOMIKA_ADMIN_TELEGRAM_CHAT_ID, 120)),
          payment_completion_fk_fix: true,
          approval_completion_ux: true,
          customer_email_delivery: true,
          customer_email_provider: "brevo",
          customer_email_configured: Boolean(cleanString(env.BREVO_API_KEY, 400) && parseBrevoSender(env.HOMIKA_EMAIL_FROM, env.HOMIKA_EMAIL_FROM_NAME)),
          rejection_reason_required: true,
          same_license_renewal: true,
          exact_plan_key_in_token: true,
          self_service_trial: true,
          trial_days: 7,
          trial_max_devices: 1,
          trial_error_reporting: true,
          trial_storage_check: true,
          trial_response_contract: 3,
          trial_ledger: "hashed_device_email_v2",
          trial_customer_dependency: false,
          activation_theme_fix: true,
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

      if (request.method === "POST" && url.pathname === "/v1/store/checkout-intents") {
        return createStoreCheckoutIntent(request, env);
      }

      if (request.method === "POST" && url.pathname === "/v1/store/renewal-intents") {
        return createAuthenticatedRenewalIntent(request, env);
      }

      if (request.method === "GET" && url.pathname === "/v1/store/checkout") {
        return getStoreCheckout(request, env);
      }

      if (request.method === "POST" && url.pathname === "/v1/store/checkout/select-plan") {
        return selectStoreCheckoutPlan(request, env);
      }

      if (request.method === "POST" && url.pathname === "/v1/store/payment-webhook") {
        return handleStorePaymentWebhook(request, env);
      }

      if (request.method === "POST" && url.pathname === "/v1/store/manual-payment/submit") {
        return submitManualPayment(request, env, ctx);
      }

      if (request.method === "GET" && url.pathname === "/v1/admin/payments") {
        return listManualPayments(request, env);
      }

      if (request.method === "GET" && url.pathname === "/v1/admin/payments/proof") {
        return getManualPaymentProof(request, env);
      }

      if (request.method === "POST" && url.pathname === "/v1/admin/payments/review") {
        return reviewManualPayment(request, env);
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


function homikaStoreUrl(env) {
  // HOMIKA_STORE_URL remains an optional override. The canonical production
  // Pages URL is compiled in so Wrangler deployments cannot break checkout
  // merely because a dashboard text variable was not preserved.
  const configured = cleanString(env.HOMIKA_STORE_URL, 2000);
  return configured || DEFAULT_HOMIKA_STORE_URL;
}

function homikaPurchaseRedirect(requestUrl, env) {
  const configured = homikaStoreUrl(env);
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
    manual_payment: publicManualPaymentConfig(env),
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

async function createStoreCheckoutIntent(request, env) {
  const body = await readJson(request);
  if (!body) return badRequest("invalid_json");

  const action = cleanString(body.action, 20).toLowerCase();
  const email = normalizeEmail(body.email);
  const planKey = cleanString(body.plan_key, 40).toLowerCase();
  if (!["buy", "renew"].includes(action)) return badRequest("invalid_checkout_action");
  if (!email) return badRequest("invalid_email");

  const plan = await getSalePlan(env, planKey);
  if (!plan) return json({ ok: false, error: "plan_not_available" }, 404);

  let licenseId = null;
  if (action === "renew") {
    const licenseKey = normalizeLicenseKey(body.license_key);
    if (!licenseKey) return badRequest("license_key_required");
    const license = await getLicenseByKey(env, licenseKey);
    if (!license) return licenseError("license_not_found", 404);
    if (license.status !== "active") return licenseError("license_inactive", 403, license);
    licenseId = license.id;
  }

  const intent = await insertCheckoutIntent(env, { action, licenseId, email, plan });
  return json({ ok: true, checkout: await checkoutIntentJson(env, intent) }, 201);
}

async function createAuthenticatedRenewalIntent(request, env) {
  const auth = await authenticateLicenseIdentityRequest(request, env);
  if (!auth.ok) return auth.response;

  const intent = await insertCheckoutIntent(env, {
    action: "renew",
    licenseId: auth.license.id,
    email: "",
    plan: null,
  });

  return json({
    ok: true,
    checkout_url: checkoutUrlFor(request, env, intent.public_token),
    checkout: await checkoutIntentJson(env, intent),
  }, 201);
}

async function getStoreCheckout(request, env) {
  const url = new URL(request.url);
  const token = cleanString(url.searchParams.get("token"), 160);
  if (!token) return badRequest("checkout_token_required");

  let intent = await getCheckoutIntent(env, token);
  if (!intent) return json({ ok: false, error: "checkout_not_found" }, 404);

  if (["pending", "processing"].includes(intent.status) && checkoutHasExpired(intent)) {
    await env.DB.prepare(
      `UPDATE checkout_intents SET status = 'expired', updated_at = CURRENT_TIMESTAMP
        WHERE id = ?1 AND status IN ('pending', 'processing')`
    ).bind(intent.id).run();
    intent = await getCheckoutIntent(env, token);
  }

  return json({ ok: true, checkout: await checkoutIntentJson(env, intent) });
}

async function selectStoreCheckoutPlan(request, env) {
  const body = await readJson(request);
  if (!body) return badRequest("invalid_json");

  const token = cleanString(body.checkout_token, 160);
  const planKey = cleanString(body.plan_key, 40).toLowerCase();
  const email = normalizeEmail(body.email);
  if (!token) return badRequest("checkout_token_required");
  if (!email) return badRequest("invalid_email");

  const plan = await getSalePlan(env, planKey);
  if (!plan) return json({ ok: false, error: "plan_not_available" }, 404);

  const intent = await getCheckoutIntent(env, token);
  if (!intent) return json({ ok: false, error: "checkout_not_found" }, 404);
  if (checkoutHasExpired(intent)) return json({ ok: false, error: "checkout_expired" }, 410);
  if (intent.status !== "pending") return json({ ok: false, error: "checkout_not_editable" }, 409);

  await env.DB.prepare(
    `UPDATE checkout_intents
        SET customer_email = ?1, plan_key = ?2, amount_cents = ?3,
            currency = ?4, updated_at = CURRENT_TIMESTAMP
      WHERE id = ?5 AND status = 'pending'`
  ).bind(email, plan.plan_key, Number(plan.price_cents), cleanString(plan.currency, 8) || "MYR", intent.id).run();

  return json({ ok: true, checkout: await checkoutIntentJson(env, await getCheckoutIntent(env, token)) });
}

async function handleStorePaymentWebhook(request, env) {
  const configuredSecret = cleanString(env.HOMIKA_PAYMENT_WEBHOOK_SECRET, 2000);
  if (!configuredSecret) return json({ ok: false, error: "payment_webhook_not_configured" }, 503);

  const suppliedSecret = cleanString(request.headers.get("x-homika-payment-secret"), 2000);
  if (!suppliedSecret || suppliedSecret !== configuredSecret) return json({ ok: false, error: "unauthorized" }, 401);

  const body = await readJson(request);
  if (!body) return badRequest("invalid_json");

  const token = cleanString(body.checkout_token, 160);
  const provider = cleanString(body.provider, 60).toLowerCase() || "payment_gateway";
  const providerPaymentId = cleanString(body.provider_payment_id, 160);
  const paymentStatus = cleanString(body.status, 40).toLowerCase();
  const amountCents = Number(body.amount_cents);
  const currency = cleanString(body.currency, 8).toUpperCase() || "MYR";

  if (!token) return badRequest("checkout_token_required");
  if (!providerPaymentId) return badRequest("provider_payment_id_required");
  if (!["paid", "success", "completed"].includes(paymentStatus)) {
    return json({ ok: true, ignored: true, reason: "payment_not_completed" });
  }
  if (!Number.isSafeInteger(amountCents) || amountCents < 0) return badRequest("invalid_payment_amount");

  const intent = await getCheckoutIntent(env, token);
  if (!intent) return json({ ok: false, error: "checkout_not_found" }, 404);
  if (!intent.plan_key || intent.amount_cents == null) return json({ ok: false, error: "checkout_plan_required" }, 409);
  if (intent.status === "completed") return json({ ok: true, idempotent: true, checkout: await checkoutIntentJson(env, intent) });
  if (!["pending", "processing"].includes(intent.status)) return json({ ok: false, error: "checkout_not_payable" }, 409);

  if (amountCents !== Number(intent.amount_cents) || currency !== String(intent.currency || "MYR").toUpperCase()) {
    return json({ ok: false, error: "payment_amount_mismatch" }, 409);
  }

  const completed = await completePaidCheckout(env, intent, provider, providerPaymentId);
  return json({ ok: true, checkout: await checkoutIntentJson(env, completed) });
}

function publicManualPaymentConfig(env) {
  const configuredQrUrl = cleanString(env.HOMIKA_PAYMENT_QR_URL, 2000);
  const displayName = cleanString(env.HOMIKA_PAYMENT_DISPLAY_NAME, 120);
  let safeQrUrl = safeHttpsUrl(configuredQrUrl);
  if (!safeQrUrl) safeQrUrl = storeAssetUrl(env, "payment-qr.jpg");
  return {
    enabled: Boolean(safeQrUrl),
    method: "qr_manual_approval",
    qr_url: safeQrUrl || null,
    display_name: displayName || "Touch n Go eWallet / DuitNow QR",
    proof_required: true,
    max_proof_bytes: MANUAL_PAYMENT_MAX_PROOF_BYTES,
    manual_review: true,
  };
}

function safeHttpsUrl(value) {
  if (!value) return "";
  try {
    const parsed = new URL(value);
    return parsed.protocol === "https:" ? parsed.toString() : "";
  } catch (_) {
    return "";
  }
}

function storeAssetUrl(env, filename) {
  const storeUrl = homikaStoreUrl(env);
  if (!storeUrl) return "";
  try {
    const base = new URL(storeUrl);
    if (base.protocol !== "https:") return "";
    if (!base.pathname.endsWith("/")) base.pathname += "/";
    base.search = "";
    base.hash = "";
    return new URL(filename, base).toString();
  } catch (_) {
    return "";
  }
}

function adminDashboardUrl(env) {
  return storeAssetUrl(env, "admin.html");
}

async function notifyAdminManualPayment(env, intent, submissionId, paymentReference) {
  const botToken = cleanString(env.HOMIKA_ADMIN_TELEGRAM_BOT_TOKEN, 300);
  const chatId = cleanString(env.HOMIKA_ADMIN_TELEGRAM_CHAT_ID, 120);
  if (!botToken || !chatId) return;

  const planNames = {
    "1_month": "1 Bulan",
    "3_month": "3 Bulan",
    "6_month": "6 Bulan",
    "1_year": "1 Tahun",
  };
  const amount = `RM${(Number(intent.amount_cents || 0) / 100).toFixed(0)}`;
  const orderRef = `HMK-${String(intent.id).replace(/-/g, "").slice(0, 10).toUpperCase()}`;
  const dashboard = adminDashboardUrl(env);
  const lines = [
    "🔔 Homika: bayaran menunggu semakan",
    `${amount} · ${planNames[intent.plan_key] || intent.plan_key || "Pelan"}`,
    `Order: ${orderRef}`,
    `Jenis: ${intent.action === "renew" ? "Renew / Upgrade" : "Pembelian baru"}`,
  ];
  if (paymentReference) lines.push(`Transaction ID: ${paymentReference}`);
  if (dashboard) lines.push(`Admin: ${dashboard}`);

  const response = await fetch(`https://api.telegram.org/bot${botToken}/sendMessage`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ chat_id: chatId, text: lines.join("\n"), disable_web_page_preview: true }),
  });
  if (!response.ok) throw new Error(`telegram_notification_http_${response.status}`);
}


function homikaPlanLabel(planKey) {
  return ({
    "1_month": "1 Bulan",
    "3_month": "3 Bulan",
    "6_month": "6 Bulan",
    "1_year": "1 Tahun",
  })[cleanString(planKey, 40)] || cleanString(planKey, 40) || "Homika Pro";
}

function homikaMoney(amountCents) {
  return `RM${(Number(amountCents || 0) / 100).toFixed(0)}`;
}

function homikaOrderReference(checkout) {
  const raw = cleanString(checkout?.reference, 80);
  return raw || "Homika order";
}

function escapeEmailHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/\"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function emailShell(title, bodyHtml) {
  return `<!doctype html><html><body style="margin:0;background:#f4f6f4;font-family:Arial,sans-serif;color:#18221e"><div style="max-width:620px;margin:0 auto;padding:28px 18px"><div style="background:#fff;border:1px solid #dde3df;border-radius:18px;padding:28px"><div style="font-size:13px;font-weight:700;letter-spacing:.08em;color:#24735a">HOMIKA PRO</div><h1 style="font-size:24px;margin:10px 0 18px">${escapeEmailHtml(title)}</h1>${bodyHtml}<p style="margin-top:28px;font-size:12px;line-height:1.55;color:#6c756f">Email ini ialah notifikasi transaksi Homika. Email anda bukan credential untuk mengakses Homika Cloud. Jangan kongsi Licence Key dengan orang lain.</p></div></div></body></html>`;
}

function approvalEmailContent(checkout) {
  const orderRef = homikaOrderReference(checkout);
  const plan = homikaPlanLabel(checkout?.plan_key);
  const amount = homikaMoney(checkout?.amount_cents);
  const isFreshPurchase = checkout?.action === "buy";
  if (isFreshPurchase) {
    const key = cleanString(checkout?.license_key, 120);
    return {
      subject: "Homika Pro - Bayaran diluluskan & Licence Key",
      text: [
        "Bayaran Homika Pro anda telah diluluskan.",
        `Order: ${orderRef}`,
        `Pelan: ${plan}`,
        `Jumlah: ${amount}`,
        "",
        `Licence Key: ${key}`,
        "",
        "Buka Homika > Activate Licence, masukkan Licence Key di atas dan aktifkan pada peranti anda.",
        "Simpan email ini untuk rujukan dan jangan kongsi Licence Key dengan orang lain.",
      ].join("\n"),
      html: emailShell("Bayaran diluluskan ✓", `<p style="line-height:1.65">Bayaran anda telah disahkan dan Homika Pro sudah sedia untuk diaktifkan.</p><p><strong>Order:</strong> ${escapeEmailHtml(orderRef)}<br><strong>Pelan:</strong> ${escapeEmailHtml(plan)}<br><strong>Jumlah:</strong> ${escapeEmailHtml(amount)}</p><div style="margin:22px 0;padding:18px;background:#f1f7f4;border-radius:14px"><div style="font-size:12px;color:#647069;margin-bottom:6px">LICENCE KEY</div><div style="font-family:monospace;font-size:20px;font-weight:700;word-break:break-all">${escapeEmailHtml(key)}</div></div><p style="line-height:1.65">Buka <strong>Homika → Activate Licence</strong>, masukkan Licence Key di atas dan aktifkan pada peranti anda.</p>`),
    };
  }
  return {
    subject: "Homika Pro - Bayaran diluluskan",
    text: [
      "Bayaran Homika Pro anda telah diluluskan.",
      `Order: ${orderRef}`,
      `Pelan: ${plan}`,
      `Jumlah: ${amount}`,
      "",
      "Lesen Homika sedia ada anda telah dikemas kini. Tiada Licence Key baharu diperlukan.",
      "Buka Homika > Licence dan tekan Verify Now.",
    ].join("\n"),
    html: emailShell("Bayaran diluluskan ✓", `<p style="line-height:1.65">Bayaran anda telah disahkan.</p><p><strong>Order:</strong> ${escapeEmailHtml(orderRef)}<br><strong>Pelan:</strong> ${escapeEmailHtml(plan)}<br><strong>Jumlah:</strong> ${escapeEmailHtml(amount)}</p><p style="line-height:1.65">Lesen Homika sedia ada anda telah dikemas kini. <strong>Tiada Licence Key baharu diperlukan.</strong></p><p style="line-height:1.65">Buka <strong>Homika → Licence → Verify Now</strong> untuk refresh status lesen.</p>`),
  };
}

function rejectionEmailContent(env, checkout, reason) {
  const orderRef = homikaOrderReference(checkout);
  const safeReason = cleanString(reason, 500) || "Bukti pembayaran tidak dapat disahkan.";
  const isRenewal = checkout?.action === "renew";
  const nextInstruction = isRenewal
    ? "Buka Homika > Licence > Upgrade / Renew dan buat checkout baru. Kemudian muat naik resit pembayaran yang betul."
    : "Buat order baru di Homika Store dan muat naik resit pembayaran yang betul.";
  const storeUrl = homikaStoreUrl(env);
  return {
    subject: "Homika Pro - Bayaran tidak dapat disahkan",
    text: [
      "Bayaran Homika anda tidak dapat disahkan.",
      `Order: ${orderRef}`,
      `Sebab: ${safeReason}`,
      "",
      "Tiada pengaktifan atau pembaharuan lesen dibuat untuk order ini.",
      nextInstruction,
      !isRenewal && storeUrl ? `Homika Store: ${storeUrl}` : "",
    ].filter(Boolean).join("\n"),
    html: emailShell("Bayaran tidak dapat disahkan", `<p style="line-height:1.65">Kami tidak dapat mengesahkan bukti pembayaran untuk order <strong>${escapeEmailHtml(orderRef)}</strong>.</p><div style="margin:18px 0;padding:16px;background:#fff4f2;border-radius:14px"><strong>Sebab ditolak:</strong><br>${escapeEmailHtml(safeReason)}</div><p style="line-height:1.65">Tiada pengaktifan atau pembaharuan lesen dibuat untuk order ini.</p><p style="line-height:1.65"><strong>Sila buat order baru</strong> dan muat naik resit/bukti pembayaran yang betul.</p>${!isRenewal && storeUrl ? `<p><a href="${escapeEmailHtml(storeUrl)}" style="display:inline-block;padding:12px 18px;border-radius:10px;background:#1d6f55;color:#fff;text-decoration:none;font-weight:700">Buka Homika Store</a></p>` : `<p style="line-height:1.65">Buka <strong>Homika → Licence → Upgrade / Renew</strong> untuk mulakan checkout baru.</p>`}`),
  };
}

function parseBrevoSender(value, nameValue = "") {
  const raw = cleanString(value, 320);
  if (!raw) return null;

  let email = raw;
  let embeddedName = "";
  const match = raw.match(/^\s*(.*?)\s*<([^<>]+)>\s*$/);
  if (match) {
    embeddedName = cleanString(match[1], 80).replace(/^['"]|['"]$/g, "");
    email = match[2];
  }

  const normalizedEmail = normalizeEmail(email);
  if (!normalizedEmail) return null;
  const configuredName = cleanString(nameValue, 80);
  return {
    email: normalizedEmail,
    name: configuredName || embeddedName || "Homika",
  };
}

async function sendCustomerPaymentDecisionEmail(env, { submission, checkout, decision, rejectionReason = "", forceResend = false }) {
  const apiKey = cleanString(env.BREVO_API_KEY, 400);
  const sender = parseBrevoSender(env.HOMIKA_EMAIL_FROM, env.HOMIKA_EMAIL_FROM_NAME);
  const to = normalizeEmail(checkout?.customer_email);
  if (!apiKey || !sender) return { configured: false, ok: false, error: "customer_email_not_configured" };
  if (!to) return { configured: true, ok: false, error: "customer_email_missing" };

  const content = decision === "reject"
    ? rejectionEmailContent(env, checkout, rejectionReason)
    : approvalEmailContent(checkout);
  const submissionId = cleanString(submission?.id, 80) || crypto.randomUUID();
  const deliveryRef = `homika-${submissionId}-${forceResend ? crypto.randomUUID() : decision}`.slice(0, 180);

  try {
    const response = await fetch("https://api.brevo.com/v3/smtp/email", {
      method: "POST",
      headers: {
        "accept": "application/json",
        "api-key": apiKey,
        "content-type": "application/json",
      },
      body: JSON.stringify({
        sender,
        to: [{ email: to }],
        subject: content.subject,
        htmlContent: content.html,
        headers: {
          "X-Mailin-custom": `homika_payment_ref:${deliveryRef}`,
        },
      }),
    });

    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      console.error("customer_payment_email_failed", {
        submission_id: submissionId,
        decision,
        status: response.status,
        provider_error: cleanString(payload?.message || payload?.code || "", 240),
      });
      return { configured: true, ok: false, error: `email_provider_http_${response.status}` };
    }

    return {
      configured: true,
      ok: true,
      provider: "brevo",
      id: cleanString(payload?.messageId, 180) || null,
      to,
    };
  } catch (err) {
    console.error("customer_payment_email_failed", {
      submission_id: submissionId,
      decision,
      error: String(err?.message || err),
    });
    return { configured: true, ok: false, error: "email_provider_unreachable" };
  }
}

async function submitManualPayment(request, env, ctx) {
  if (!env.BACKUPS) return json({ ok: false, error: "payment_proof_storage_not_configured" }, 503);
  const body = await readJson(request);
  if (!body) return badRequest("invalid_json");

  const token = cleanString(body.checkout_token, 160);
  const payerNameInput = cleanString(body.payer_name, 120);
  const paymentReference = cleanString(body.payment_reference, 160);
  const proofContentType = cleanString(body.proof_content_type, 80).toLowerCase();
  const proofBase64 = cleanString(body.proof_base64, 4_000_000);
  if (!token) return badRequest("checkout_token_required");
  if (!MANUAL_PAYMENT_IMAGE_TYPES.has(proofContentType)) return badRequest("invalid_proof_type");
  if (!proofBase64) return badRequest("payment_proof_required");

  const intent = await getCheckoutIntent(env, token);
  if (!intent) return json({ ok: false, error: "checkout_not_found" }, 404);
  if (checkoutHasExpired(intent)) return json({ ok: false, error: "checkout_expired" }, 410);
  if (!intent.plan_key || intent.amount_cents == null || !intent.customer_email) {
    return json({ ok: false, error: "checkout_plan_required" }, 409);
  }
  if (intent.status === "completed") return json({ ok: false, error: "checkout_already_completed" }, 409);
  if (!['pending','processing'].includes(intent.status)) return json({ ok: false, error: "checkout_not_payable" }, 409);

  const payerName = payerNameInput || cleanString(intent.customer_email, 120) || "Homika customer";

  let proofBytes;
  try {
    proofBytes = base64ToBytes(stripDataUrlPrefix(proofBase64));
  } catch (_) {
    return badRequest("invalid_payment_proof");
  }
  if (!proofBytes.byteLength || proofBytes.byteLength > MANUAL_PAYMENT_MAX_PROOF_BYTES) {
    return json({ ok: false, error: "payment_proof_too_large", max_bytes: MANUAL_PAYMENT_MAX_PROOF_BYTES }, 413);
  }

  const existing = await getManualPaymentSubmissionForCheckout(env, intent.id);
  if (existing?.status === "approved") return json({ ok: false, error: "payment_already_approved" }, 409);

  const submissionId = existing?.id || crypto.randomUUID();
  const extension = proofExtensionFor(proofContentType);
  const objectKey = `manual-payments/${intent.id}/${crypto.randomUUID()}.${extension}`;
  await env.BACKUPS.put(objectKey, proofBytes, {
    httpMetadata: { contentType: proofContentType },
    customMetadata: {
      kind: "manual_payment_proof",
      checkout_id: intent.id,
      submission_id: submissionId,
    },
  });

  try {
    if (existing) {
      await env.DB.prepare(
        `UPDATE manual_payment_submissions
            SET payer_name = ?1, payment_reference = ?2, proof_object_key = ?3,
                proof_content_type = ?4, proof_size = ?5, status = 'submitted',
                admin_note = NULL, reviewed_at = NULL, updated_at = CURRENT_TIMESTAMP,
                submitted_at = CURRENT_TIMESTAMP
          WHERE id = ?6`
      ).bind(payerName, paymentReference || null, objectKey, proofContentType, proofBytes.byteLength, submissionId).run();
    } else {
      await env.DB.prepare(
        `INSERT INTO manual_payment_submissions (
           id, checkout_id, payer_name, payment_reference, proof_object_key,
           proof_content_type, proof_size, status
         ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, 'submitted')`
      ).bind(submissionId, intent.id, payerName, paymentReference || null, objectKey,
        proofContentType, proofBytes.byteLength).run();
    }
  } catch (err) {
    try { await env.BACKUPS.delete(objectKey); } catch (_) {}
    throw err;
  }

  if (existing?.proof_object_key && existing.proof_object_key !== objectKey) {
    try { await env.BACKUPS.delete(existing.proof_object_key); } catch (_) {}
  }

  const notification = notifyAdminManualPayment(env, intent, submissionId, paymentReference)
    .catch((err) => console.error("admin_payment_notification_failed", err));
  if (ctx?.waitUntil) ctx.waitUntil(notification);
  else await notification;

  return json({
    ok: true,
    message: "payment_submitted_for_review",
    checkout: await checkoutIntentJson(env, await getCheckoutIntent(env, token)),
  }, existing ? 200 : 201);
}

async function listManualPayments(request, env) {
  const auth = authorizeAdmin(request, env);
  if (!auth.ok) return auth.response;
  const url = new URL(request.url);
  const requestedStatus = cleanString(url.searchParams.get("status"), 20).toLowerCase();
  const status = ["submitted", "approved", "rejected", "all"].includes(requestedStatus) ? requestedStatus : "submitted";
  const limitRaw = Number(url.searchParams.get("limit") || 100);
  const limit = Number.isSafeInteger(limitRaw) ? Math.min(Math.max(limitRaw, 1), 200) : 100;
  const where = status === "all" ? "" : "WHERE m.status = ?1";
  const statement = env.DB.prepare(
    `SELECT m.id, m.checkout_id, m.payer_name, m.payment_reference, m.proof_content_type,
            m.proof_size, m.status, m.admin_note, m.submitted_at, m.reviewed_at, m.updated_at,
            c.public_token, c.action, c.customer_email, c.plan_key, c.amount_cents, c.currency,
            c.status AS checkout_status, c.license_id, c.resulting_license_key
       FROM manual_payment_submissions m
       JOIN checkout_intents c ON c.id = m.checkout_id
       ${where}
      ORDER BY CASE WHEN m.status = 'submitted' THEN 0 ELSE 1 END,
               m.submitted_at DESC
      LIMIT ${limit}`
  );
  const result = status === "all" ? await statement.all() : await statement.bind(status).all();
  const items = [];
  for (const row of (result.results || [])) {
    let licenseHint = null;
    if (row.license_id) {
      const license = await getLicenseById(env, row.license_id);
      if (license) licenseHint = maskLicenseKey(license.license_key);
    }
    items.push({
      id: row.id,
      checkout_token: row.public_token,
      checkout_reference: `HMK-${String(row.checkout_id).replace(/-/g, "").slice(0, 10).toUpperCase()}`,
      action: row.action,
      email: row.customer_email,
      plan_key: row.plan_key,
      amount_cents: Number(row.amount_cents || 0),
      currency: row.currency || "MYR",
      checkout_status: row.checkout_status,
      license_hint: licenseHint,
      payer_name: row.payer_name,
      payment_reference: row.payment_reference || null,
      proof_content_type: row.proof_content_type,
      proof_size: Number(row.proof_size || 0),
      status: row.status,
      admin_note: row.admin_note || null,
      submitted_at: row.submitted_at,
      reviewed_at: row.reviewed_at || null,
      license_key: row.action === "buy" && row.checkout_status === "completed"
        ? (row.resulting_license_key || null)
        : null,
    });
  }
  return json({ ok: true, status, items });
}

async function getManualPaymentProof(request, env) {
  const auth = authorizeAdmin(request, env);
  if (!auth.ok) return auth.response;
  if (!env.BACKUPS) return json({ ok: false, error: "payment_proof_storage_not_configured" }, 503);
  const url = new URL(request.url);
  const id = cleanString(url.searchParams.get("id"), 80);
  if (!id) return badRequest("submission_id_required");
  const submission = await env.DB.prepare(
    `SELECT proof_object_key, proof_content_type FROM manual_payment_submissions WHERE id = ?1 LIMIT 1`
  ).bind(id).first();
  if (!submission?.proof_object_key) return json({ ok: false, error: "payment_proof_not_found" }, 404);
  const object = await env.BACKUPS.get(submission.proof_object_key);
  if (!object) return json({ ok: false, error: "payment_proof_not_found" }, 404);
  const headers = corsHeaders();
  headers["content-type"] = submission.proof_content_type || "application/octet-stream";
  headers["content-disposition"] = `inline; filename="homika-payment-${id}.jpg"`;
  return new Response(object.body, { status: 200, headers });
}

async function reviewManualPayment(request, env) {
  const auth = authorizeAdmin(request, env);
  if (!auth.ok) return auth.response;
  const body = await readJson(request);
  if (!body) return badRequest("invalid_json");
  const id = cleanString(body.submission_id, 80);
  const action = cleanString(body.action, 20).toLowerCase();
  const adminNote = cleanString(body.admin_note, 500);
  if (!id) return badRequest("submission_id_required");
  if (!["approve", "reject", "resend_email"].includes(action)) return badRequest("invalid_review_action");

  const submission = await env.DB.prepare(
    `SELECT m.*, c.public_token, c.status AS checkout_status
       FROM manual_payment_submissions m
       JOIN checkout_intents c ON c.id = m.checkout_id
      WHERE m.id = ?1 LIMIT 1`
  ).bind(id).first();
  if (!submission) return json({ ok: false, error: "payment_submission_not_found" }, 404);

  if (action === "resend_email") {
    if (!["approved", "rejected"].includes(submission.status)) {
      return json({ ok: false, error: "payment_not_reviewed" }, 409);
    }
    const intent = await getCheckoutIntent(env, submission.public_token);
    if (!intent) return json({ ok: false, error: "checkout_not_found" }, 404);
    const checkout = await checkoutIntentJson(env, intent);
    const emailDelivery = await sendCustomerPaymentDecisionEmail(env, {
      submission,
      checkout,
      decision: submission.status === "approved" ? "approve" : "reject",
      rejectionReason: submission.admin_note || "Bukti pembayaran tidak dapat disahkan.",
      forceResend: true,
    });
    return json({ ok: true, checkout, email_delivery: emailDelivery });
  }

  if (action === "reject") {
    if (submission.status === "approved") return json({ ok: false, error: "payment_already_approved" }, 409);
    if (submission.status === "rejected") {
      const checkout = await getCheckoutIntent(env, submission.public_token);
      return json({ ok: true, idempotent: true, checkout: await checkoutIntentJson(env, checkout), email_delivery: { ok: true, skipped: "already_rejected" } });
    }
    if (!adminNote) return badRequest("rejection_reason_required");
    await env.DB.prepare(
      `UPDATE manual_payment_submissions
          SET status = 'rejected', admin_note = ?1, reviewed_at = CURRENT_TIMESTAMP,
              updated_at = CURRENT_TIMESTAMP
        WHERE id = ?2`
    ).bind(adminNote, id).run();
    const checkout = await getCheckoutIntent(env, submission.public_token);
    const checkoutJson = await checkoutIntentJson(env, checkout);
    const emailDelivery = await sendCustomerPaymentDecisionEmail(env, {
      submission: { ...submission, status: "rejected", admin_note: adminNote },
      checkout: checkoutJson,
      decision: "reject",
      rejectionReason: adminNote,
    });
    return json({ ok: true, checkout: checkoutJson, email_delivery: emailDelivery });
  }

  if (submission.status === "approved" || submission.checkout_status === "completed") {
    const checkout = await getCheckoutIntent(env, submission.public_token);
    return json({ ok: true, idempotent: true, checkout: await checkoutIntentJson(env, checkout), email_delivery: { ok: true, skipped: "already_approved" } });
  }
  if (submission.status !== "submitted") return json({ ok: false, error: "payment_not_ready_for_review" }, 409);

  const intent = await getCheckoutIntent(env, submission.public_token);
  if (!intent) return json({ ok: false, error: "checkout_not_found" }, 404);
  if (!intent.plan_key || intent.amount_cents == null) return json({ ok: false, error: "checkout_plan_required" }, 409);
  if (!["pending", "processing"].includes(intent.status)) return json({ ok: false, error: "checkout_not_payable" }, 409);

  const providerPaymentId = `manual_qr:${submission.id}`;
  let completed;
  try {
    completed = await completePaidCheckout(env, intent, "manual_qr", providerPaymentId);
  } catch (err) {
    console.error("manual_payment_completion_failed", {
      submission_id: submission.id,
      checkout_id: submission.checkout_id,
      action: intent.action,
      license_id: intent.license_id || null,
      error: String(err?.message || err),
    });
    return json({ ok: false, error: paymentCompletionErrorCode(err) }, 500);
  }
  await env.DB.prepare(
    `UPDATE manual_payment_submissions
        SET status = 'approved', admin_note = ?1, reviewed_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
      WHERE id = ?2`
  ).bind(adminNote || null, id).run();
  const checkoutJson = await checkoutIntentJson(env, completed);
  const emailDelivery = await sendCustomerPaymentDecisionEmail(env, {
    submission: { ...submission, status: "approved", admin_note: adminNote || null },
    checkout: checkoutJson,
    decision: "approve",
  });
  return json({ ok: true, checkout: checkoutJson, email_delivery: emailDelivery });
}

function paymentCompletionErrorCode(err) {
  const message = String(err?.message || err || "").toLowerCase();
  const known = [
    "checkout_not_found",
    "plan_not_available",
    "license_not_found",
    "license_not_found_after_update",
    "resulting_license_not_created",
    "payment_license_not_found",
    "provider_payment_conflict",
    "invalid_email",
    "invalid_plan_duration",
    "unsupported_plan_duration",
  ];
  for (const code of known) {
    if (message.includes(code)) return code;
  }
  if (message.includes("foreign key")) return "payment_completion_foreign_key_error";
  if (message.includes("d1_error") || message.includes("sqlite")) return "payment_completion_database_error";
  return "payment_completion_failed";
}

async function getManualPaymentSubmissionForCheckout(env, checkoutId) {
  return env.DB.prepare(
    `SELECT id, checkout_id, payer_name, payment_reference, proof_object_key,
            proof_content_type, proof_size, status, admin_note,
            submitted_at, reviewed_at, updated_at
       FROM manual_payment_submissions WHERE checkout_id = ?1 LIMIT 1`
  ).bind(checkoutId).first();
}

function authorizeAdmin(request, env) {
  const configured = cleanString(env.HOMIKA_ADMIN_SECRET, 2000);
  if (!configured) return { ok: false, response: json({ ok: false, error: "admin_not_configured" }, 503) };
  const supplied = cleanString(request.headers.get("x-homika-admin-secret"), 2000);
  if (!supplied || supplied !== configured) return { ok: false, response: json({ ok: false, error: "unauthorized" }, 401) };
  return { ok: true };
}

function stripDataUrlPrefix(value) {
  const comma = value.indexOf(",");
  return value.startsWith("data:") && comma >= 0 ? value.slice(comma + 1) : value;
}

function proofExtensionFor(contentType) {
  if (contentType === "image/png") return "png";
  if (contentType === "image/webp") return "webp";
  return "jpg";
}

const MANUAL_PAYMENT_MAX_PROOF_BYTES = 2 * 1024 * 1024;
const MANUAL_PAYMENT_IMAGE_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);

async function insertCheckoutIntent(env, { action, licenseId = null, email = "", plan = null }) {
  const id = crypto.randomUUID();
  const token = generateCheckoutToken();
  const expiresAt = formatSqliteTimestamp(Date.now() + STORE_CHECKOUT_TTL_MS);
  await env.DB.prepare(
    `INSERT INTO checkout_intents (
       id, public_token, product_id, action, license_id,
       customer_email, plan_key, amount_cents, currency, status, expires_at
     ) VALUES (?1, ?2, 'homika_pro', ?3, ?4, ?5, ?6, ?7, ?8, 'pending', ?9)`
  ).bind(id, token, action, licenseId, email || null, plan?.plan_key || null,
    plan ? Number(plan.price_cents) : null,
    plan ? (cleanString(plan.currency, 8) || "MYR") : "MYR", expiresAt).run();
  return getCheckoutIntent(env, token);
}

async function getCheckoutIntent(env, token) {
  return env.DB.prepare(
    `SELECT id, public_token, product_id, action, license_id,
            customer_email, plan_key, amount_cents, currency, status,
            provider, provider_reference, target_expires_at,
            resulting_license_id, resulting_license_key,
            created_at, updated_at, expires_at, paid_at, completed_at
       FROM checkout_intents WHERE public_token = ?1 LIMIT 1`
  ).bind(token).first();
}

async function checkoutIntentJson(env, intent) {
  let licenseHint = "";
  let entitlementExpiresAt = intent?.target_expires_at || null;
  const manualPayment = intent?.id ? await getManualPaymentSubmissionForCheckout(env, intent.id) : null;
  if (intent?.license_id) {
    const license = await getLicenseById(env, intent.license_id);
    if (license) {
      licenseHint = maskLicenseKey(license.license_key);
      if (intent.status === "completed") entitlementExpiresAt = license.expires_at;
    }
  }
  return {
    token: intent.public_token,
    reference: `HMK-${String(intent.id).replace(/-/g, "").slice(0, 10).toUpperCase()}`,
    action: intent.action,
    status: intent.status,
    plan_key: intent.plan_key,
    amount_cents: intent.amount_cents == null ? null : Number(intent.amount_cents),
    currency: cleanString(intent.currency, 8) || "MYR",
    customer_email: intent.customer_email || null,
    license_hint: licenseHint || null,
    expires_at: intent.expires_at,
    entitlement_expires_at: entitlementExpiresAt,
    completed_at: intent.completed_at || null,
    manual_payment: manualPayment ? {
      status: manualPayment.status,
      payer_name: manualPayment.payer_name || null,
      payment_reference: manualPayment.payment_reference || null,
      submitted_at: manualPayment.submitted_at || null,
      reviewed_at: manualPayment.reviewed_at || null,
      admin_note: manualPayment.admin_note || null,
    } : null,
    ...(intent.status === "completed" && intent.action === "buy" ? { license_key: intent.resulting_license_key || null } : {}),
  };
}

async function getSalePlan(env, planKey) {
  if (!planKey) return null;
  return env.DB.prepare(
    `SELECT plan_key, duration_unit, duration_value, max_devices, price_cents, currency
       FROM license_plans
      WHERE product_id = 'homika_pro' AND plan_key = ?1
        AND sale_enabled = 1 AND price_cents IS NOT NULL LIMIT 1`
  ).bind(planKey).first();
}

function checkoutHasExpired(intent) {
  const expiry = parseSqliteTimestamp(intent?.expires_at);
  return !expiry || expiry.getTime() <= Date.now();
}

function checkoutUrlFor(request, env, token) {
  const configured = homikaStoreUrl(env);
  if (configured) {
    try {
      const url = new URL(configured);
      if (url.protocol === "https:" || url.protocol === "http:") {
        url.searchParams.set("checkout", token);
        url.searchParams.set("action", "renew");
        return url.toString();
      }
    } catch (_) {}
  }
  const fallback = new URL("/buy/homika-pro", request.url);
  fallback.searchParams.set("checkout", token);
  fallback.searchParams.set("action", "renew");
  return fallback.toString();
}

async function authenticateLicenseIdentityRequest(request, env) {
  const authorization = cleanString(request.headers.get("authorization"), 7000);
  const deviceId = cleanString(request.headers.get("x-homika-device-id"), 300);
  if (!authorization.toLowerCase().startsWith("bearer ")) return { ok: false, response: json({ ok: false, error: "activation_token_required" }, 401) };
  if (!deviceId) return { ok: false, response: json({ ok: false, error: "device_id_required" }, 400) };

  const verified = await verifyActivationToken(authorization.slice(7).trim());
  if (!verified.ok) return { ok: false, response: json({ ok: false, error: verified.error }, 403) };

  const deviceHash = await sha256Hex(deviceId);
  if (verified.claims.device_hash !== deviceHash) return { ok: false, response: json({ ok: false, error: "token_device_mismatch" }, 403) };

  const license = await getLicenseById(env, verified.claims.license_id);
  if (!license) return { ok: false, response: licenseError("license_not_found", 404) };
  if (license.status !== "active") return { ok: false, response: licenseError("license_inactive", 403, license) };

  const device = await env.DB.prepare(
    `SELECT id, status FROM devices WHERE license_id = ?1 AND device_hash = ?2 LIMIT 1`
  ).bind(license.id, deviceHash).first();
  if (!device || device.status !== "active") return { ok: false, response: licenseError("device_not_activated", 403, license) };
  return { ok: true, license, deviceHash };
}

async function completePaidCheckout(env, originalIntent, provider, providerPaymentId) {
  let intent = await getCheckoutIntent(env, originalIntent.public_token);
  if (!intent) throw new Error("checkout_not_found");
  if (intent.status === "completed") return intent;
  const plan = await getSalePlan(env, intent.plan_key);
  if (!plan) throw new Error("plan_not_available");

  let renewalLicense = null;
  let targetExpiresAt = intent.target_expires_at;
  if (!targetExpiresAt) {
    let baseMillis = Date.now();
    if (intent.action === "renew") {
      renewalLicense = await getLicenseById(env, intent.license_id);
      if (!renewalLicense) throw new Error("license_not_found");
      const currentExpiry = parseSqliteTimestamp(renewalLicense.expires_at);
      if (currentExpiry && currentExpiry.getTime() > baseMillis) baseMillis = currentExpiry.getTime();
    }
    targetExpiresAt = formatSqliteTimestamp(addPlanDuration(baseMillis, plan));
    await env.DB.prepare(
      `UPDATE checkout_intents SET status = 'processing', target_expires_at = ?1,
              provider = ?2, provider_reference = ?3,
              paid_at = COALESCE(paid_at, CURRENT_TIMESTAMP), updated_at = CURRENT_TIMESTAMP
        WHERE id = ?4 AND status IN ('pending', 'processing')`
    ).bind(targetExpiresAt, provider, providerPaymentId, intent.id).run();
    intent = await getCheckoutIntent(env, intent.public_token);
  }

  let license = null;
  if (intent.action === "buy") {
    // IMPORTANT: checkout_intents.resulting_license_id has a foreign key to
    // licenses(id). Never persist a new result ID before the license row exists.
    // Persist only the generated key first, so retries can recover a partially
    // completed purchase without creating duplicate licenses.
    let resultLicenseId = cleanString(intent.resulting_license_id, 80) || null;
    let resultLicenseKey = cleanString(intent.resulting_license_key, 120) || null;

    if (resultLicenseId) {
      license = await getLicenseById(env, resultLicenseId);
    }
    if (!license && resultLicenseKey) {
      license = await getLicenseByKey(env, resultLicenseKey);
      if (license) resultLicenseId = license.id;
    }

    if (!resultLicenseKey) {
      resultLicenseKey = generatePaidLicenseKey();
      await env.DB.prepare(
        `UPDATE checkout_intents
            SET resulting_license_key = ?1, updated_at = CURRENT_TIMESTAMP
          WHERE id = ?2`
      ).bind(resultLicenseKey, intent.id).run();
      intent = await getCheckoutIntent(env, intent.public_token);
    }

    if (!license) {
      resultLicenseId = resultLicenseId || crypto.randomUUID();
      const customerId = await findOrCreateCustomerByEmail(env, intent.customer_email);
      await env.DB.prepare(
        `INSERT INTO licenses (id, license_key, product_id, customer_id, status,
          expires_at, max_devices, plan_type, plan_key)
         VALUES (?1, ?2, 'homika_pro', ?3, 'active', ?4, ?5, ?6, ?7)`
      ).bind(resultLicenseId, resultLicenseKey, customerId, targetExpiresAt,
        Number(plan.max_devices || 3), paidPlanType(plan), plan.plan_key).run();
      license = await getLicenseById(env, resultLicenseId);
    }

    if (!license) throw new Error("resulting_license_not_created");

    // Safe only after the referenced license exists.
    await env.DB.prepare(
      `UPDATE checkout_intents
          SET resulting_license_id = ?1,
              resulting_license_key = COALESCE(resulting_license_key, ?2),
              updated_at = CURRENT_TIMESTAMP
        WHERE id = ?3`
    ).bind(license.id, license.license_key, intent.id).run();
    intent = await getCheckoutIntent(env, intent.public_token);
  } else {
    license = renewalLicense || await getLicenseById(env, intent.license_id);
    if (!license) throw new Error("license_not_found");

    // Keep a valid existing customer relation. If legacy data contains an
    // orphan customer_id, repair it from the checkout email rather than
    // triggering another foreign-key failure when the licence row is updated.
    let customerId = license.customer_id || null;
    if (customerId) {
      const customer = await env.DB.prepare(
        `SELECT id FROM customers WHERE id = ?1 LIMIT 1`
      ).bind(customerId).first();
      if (!customer?.id) customerId = null;
    }
    if (!customerId && intent.customer_email) {
      customerId = await findOrCreateCustomerByEmail(env, intent.customer_email);
    }

    await env.DB.prepare(
      `UPDATE licenses SET customer_id = ?1, status = 'active',
              expires_at = ?2, max_devices = ?3, plan_type = ?4, plan_key = ?5,
              updated_at = CURRENT_TIMESTAMP WHERE id = ?6`
    ).bind(customerId, targetExpiresAt, Number(plan.max_devices || 3), paidPlanType(plan), plan.plan_key, license.id).run();
    license = await getLicenseById(env, license.id);
    if (!license) throw new Error("license_not_found_after_update");
  }

  // Confirm the payment foreign-key target still exists immediately before
  // writing payments. This gives a deterministic error instead of a raw D1 FK.
  const persistedLicense = await getLicenseById(env, license.id);
  if (!persistedLicense) throw new Error("payment_license_not_found");
  license = persistedLicense;

  const existingPayment = await env.DB.prepare(
    `SELECT id, license_id, provider, amount_cents, currency
       FROM payments WHERE provider_payment_id = ?1 LIMIT 1`
  ).bind(providerPaymentId).first();
  if (!existingPayment) {
    await env.DB.prepare(
      `INSERT INTO payments (id, license_id, provider, provider_payment_id,
       amount_cents, currency, status, plan_key, paid_at)
       VALUES (?1, ?2, ?3, ?4, ?5, ?6, 'paid', ?7, CURRENT_TIMESTAMP)`
    ).bind(crypto.randomUUID(), license.id, provider, providerPaymentId,
      Number(intent.amount_cents), cleanString(intent.currency, 8) || "MYR", intent.plan_key).run();
  } else {
    if (String(existingPayment.license_id || "") !== String(license.id) ||
        Number(existingPayment.amount_cents) !== Number(intent.amount_cents) ||
        String(existingPayment.currency).toUpperCase() !== String(intent.currency).toUpperCase()) {
      throw new Error("provider_payment_conflict");
    }
  }

  await env.DB.prepare(
    `UPDATE checkout_intents SET status = 'completed', provider = ?1, provider_reference = ?2,
            resulting_license_id = ?3,
            resulting_license_key = CASE WHEN action = 'buy' THEN COALESCE(resulting_license_key, ?4) ELSE resulting_license_key END,
            completed_at = COALESCE(completed_at, CURRENT_TIMESTAMP), updated_at = CURRENT_TIMESTAMP
      WHERE id = ?5`
  ).bind(provider, providerPaymentId, license.id, license.license_key, intent.id).run();
  return getCheckoutIntent(env, intent.public_token);
}

async function findOrCreateCustomerByEmail(env, email) {
  const normalized = normalizeEmail(email);
  if (!normalized) throw new Error("invalid_email");
  const existing = await env.DB.prepare(
    `SELECT id FROM customers WHERE lower(trim(email)) = lower(trim(?1)) ORDER BY created_at ASC LIMIT 1`
  ).bind(normalized).first();
  if (existing?.id) return existing.id;
  const id = crypto.randomUUID();
  await env.DB.prepare(`INSERT INTO customers (id, email) VALUES (?1, ?2)`).bind(id, normalized).run();
  return id;
}

function paidPlanType(plan) {
  return cleanString(plan.duration_unit, 20).toLowerCase() === "year" ? "annual" : "monthly";
}

function addPlanDuration(baseMillis, plan) {
  const unit = cleanString(plan.duration_unit, 20).toLowerCase();
  const value = Number(plan.duration_value || 0);
  if (!Number.isInteger(value) || value <= 0) throw new Error("invalid_plan_duration");
  if (unit === "day") return baseMillis + (value * 24 * 60 * 60 * 1000);
  if (unit === "month") return addUtcMonths(baseMillis, value);
  if (unit === "year") return addUtcMonths(baseMillis, value * 12);
  throw new Error("unsupported_plan_duration");
}

function addUtcMonths(baseMillis, months) {
  const source = new Date(baseMillis);
  const day = source.getUTCDate();
  const target = new Date(Date.UTC(source.getUTCFullYear(), source.getUTCMonth(), 1,
    source.getUTCHours(), source.getUTCMinutes(), source.getUTCSeconds(), source.getUTCMilliseconds()));
  target.setUTCMonth(target.getUTCMonth() + months);
  const lastDay = new Date(Date.UTC(target.getUTCFullYear(), target.getUTCMonth() + 1, 0)).getUTCDate();
  target.setUTCDate(Math.min(day, lastDay));
  return target.getTime();
}

function generateCheckoutToken() {
  const bytes = new Uint8Array(24);
  crypto.getRandomValues(bytes);
  return base64UrlEncodeBytes(bytes);
}

function generatePaidLicenseKey() {
  const bytes = new Uint8Array(12);
  crypto.getRandomValues(bytes);
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let token = "";
  for (const byte of bytes) token += alphabet[byte % alphabet.length];
  return `HMK-${token.slice(0, 4)}-${token.slice(4, 8)}-${token.slice(8, 12)}`;
}

const STORE_CHECKOUT_TTL_MS = 24 * 60 * 60 * 1000;


async function claimTrial(request, env) {
  const body = await readJson(request);
  if (!body) return trialJson({ ok: false, error: "invalid_json" }, 400);

  const email = normalizeEmail(body.email);
  const deviceId = cleanString(body.device_id, 300);
  const deviceName = cleanString(body.device_name, 120);

  if (!email) return trialJson({ ok: false, error: "invalid_email" }, 400);
  if (!deviceId) return trialJson({ ok: false, error: "device_id_required" }, 400);

  const readiness = await trialStorageReadiness(env);
  if (!readiness.ready) {
    console.warn("Trial storage is not ready", readiness);
    return trialJson({ ok: false, error: "trial_setup_required" }, 503);
  }

  const deviceHash = await sha256Hex(deviceId);
  const emailHash = await sha256Hex(email);

  // V2 trial ledger deliberately stores only hashes and does not depend on
  // the customers table. This keeps trial eligibility independent of legacy
  // customer schema/constraints while still enforcing one trial per device/email.
  const deviceClaim = await env.DB.prepare(
    `SELECT id, license_id, email_hash, device_hash
       FROM trial_claims_v2
      WHERE product_id = ?1 AND device_hash = ?2
      LIMIT 1`
  ).bind("homika_pro", deviceHash).first();

  const emailClaim = await env.DB.prepare(
    `SELECT id, license_id, email_hash, device_hash
       FROM trial_claims_v2
      WHERE product_id = ?1 AND email_hash = ?2
      LIMIT 1`
  ).bind("homika_pro", emailHash).first();

  // Preserve eligibility history created by Patch 13A if any successful
  // redemption already exists in the legacy ledger.
  let legacyDeviceClaim = null;
  let legacyEmailClaim = null;
  try {
    legacyDeviceClaim = await env.DB.prepare(
      `SELECT id, license_id, customer_hash AS email_hash, device_hash
         FROM trial_redemptions
        WHERE product_id = ?1 AND device_hash = ?2
        LIMIT 1`
    ).bind("homika_pro", deviceHash).first();

    legacyEmailClaim = await env.DB.prepare(
      `SELECT id, license_id, customer_hash AS email_hash, device_hash
         FROM trial_redemptions
        WHERE product_id = ?1 AND customer_hash = ?2
        LIMIT 1`
    ).bind("homika_pro", emailHash).first();
  } catch (err) {
    // The V2 ledger is authoritative. Legacy-table read failure must not
    // prevent a new trial after migration 0007 is installed.
    console.warn("Legacy trial ledger unavailable", err);
  }

  const byDevice = deviceClaim || legacyDeviceClaim;
  const byEmail = emailClaim || legacyEmailClaim;

  if (byDevice || byEmail) {
    const sameClaim =
      byDevice && byEmail && byDevice.license_id === byEmail.license_id;

    if (sameClaim) {
      const existingLicense = await getLicenseById(env, byDevice.license_id);
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
                SET status = 'active',
                    device_name = ?1,
                    last_seen_at = CURRENT_TIMESTAMP,
                    deactivated_at = NULL
              WHERE id = ?2`
          ).bind(deviceName || null, current.id).run();
        } else {
          await env.DB.prepare(
            `INSERT INTO devices (id, license_id, device_hash, device_name, status)
             VALUES (?1, ?2, ?3, ?4, 'active')`
          ).bind(
            crypto.randomUUID(),
            existingLicense.id,
            deviceHash,
            deviceName || null,
          ).run();
        }

        return activationResponse(env, existingLicense, deviceHash, true);
      }

      return trialJson({ ok: false, error: "trial_already_used" }, 409);
    }

    if (byDevice) {
      return trialJson({ ok: false, error: "trial_already_used_device" }, 409);
    }
    return trialJson({ ok: false, error: "trial_already_used_customer" }, 409);
  }

  const plan = await env.DB.prepare(
    `SELECT plan_key, duration_value, max_devices
       FROM license_plans
      WHERE product_id = ?1
        AND plan_key = 'trial_7d'
      LIMIT 1`
  ).bind("homika_pro").first();

  if (!plan || Number(plan.duration_value || 0) !== 7) {
    return trialJson({ ok: false, error: "trial_unavailable" }, 503);
  }

  const licenseId = crypto.randomUUID();
  const licenseKey = generateTrialLicenseKey();
  const expiresAt = formatSqliteTimestamp(Date.now() + (7 * 24 * 60 * 60 * 1000));

  // Create sequentially. This avoids cross-table customer dependencies and
  // makes cleanup deterministic if a later write fails.
  try {
    await env.DB.prepare(
      `INSERT INTO licenses (
         id, license_key, product_id, customer_id, status,
         expires_at, max_devices, plan_type, plan_key
       ) VALUES (?1, ?2, 'homika_pro', NULL, 'active', ?3, 1, 'trial', 'trial_7d')`
    ).bind(licenseId, licenseKey, expiresAt).run();
  } catch (err) {
    console.error("Trial licence insert failed", err);
    return trialJson({ ok: false, error: "trial_server_error" }, 500);
  }

  try {
    await env.DB.prepare(
      `INSERT INTO devices (
         id, license_id, device_hash, device_name, status
       ) VALUES (?1, ?2, ?3, ?4, 'active')`
    ).bind(
      crypto.randomUUID(),
      licenseId,
      deviceHash,
      deviceName || null,
    ).run();
  } catch (err) {
    console.error("Trial device insert failed", err);
    await cleanupFailedTrial(env, licenseId);
    return trialJson({ ok: false, error: "trial_server_error" }, 500);
  }

  try {
    await env.DB.prepare(
      `INSERT INTO trial_claims_v2 (
         id, product_id, license_id, email_hash, device_hash,
         redeemed_at, expires_at
       ) VALUES (?1, 'homika_pro', ?2, ?3, ?4, CURRENT_TIMESTAMP, ?5)`
    ).bind(
      crypto.randomUUID(),
      licenseId,
      emailHash,
      deviceHash,
      expiresAt,
    ).run();
  } catch (err) {
    // A concurrent request may have won the unique device/email claim.
    console.warn("Trial claim ledger insert failed", err);
    await cleanupFailedTrial(env, licenseId);

    const racedDevice = await env.DB.prepare(
      `SELECT license_id
         FROM trial_claims_v2
        WHERE product_id = 'homika_pro' AND device_hash = ?1
        LIMIT 1`
    ).bind(deviceHash).first();
    if (racedDevice) {
      return trialJson({ ok: false, error: "trial_already_used_device" }, 409);
    }

    const racedEmail = await env.DB.prepare(
      `SELECT license_id
         FROM trial_claims_v2
        WHERE product_id = 'homika_pro' AND email_hash = ?1
        LIMIT 1`
    ).bind(emailHash).first();
    if (racedEmail) {
      return trialJson({ ok: false, error: "trial_already_used_customer" }, 409);
    }

    return trialJson({ ok: false, error: "trial_server_error" }, 500);
  }

  const license = await getLicenseById(env, licenseId);
  if (!license) {
    await cleanupFailedTrial(env, licenseId);
    return trialJson({ ok: false, error: "trial_server_error" }, 500);
  }

  return activationResponse(env, license, deviceHash, false);
}

async function cleanupFailedTrial(env, licenseId) {
  try {
    await env.DB.prepare(
      `DELETE FROM trial_claims_v2 WHERE license_id = ?1`
    ).bind(licenseId).run();
  } catch (_) {}
  try {
    await env.DB.prepare(
      `DELETE FROM devices WHERE license_id = ?1`
    ).bind(licenseId).run();
  } catch (_) {}
  try {
    await env.DB.prepare(
      `DELETE FROM licenses WHERE id = ?1 AND plan_type = 'trial'`
    ).bind(licenseId).run();
  } catch (_) {}
}

function trialJson(data, status = 200) {
  return json({ ...data, endpoint: "trial_claim", contract: 2 }, status);
}

async function trialStorageReadiness(env) {
  try {
    const ledger = await env.DB.prepare(
      `SELECT name FROM sqlite_master
        WHERE type = 'table' AND name = 'trial_claims_v2'
        LIMIT 1`
    ).first();
    if (!ledger) return { ready: false, reason: "trial_claims_v2_missing" };

    const plan = await env.DB.prepare(
      `SELECT plan_key, duration_value, max_devices
         FROM license_plans
        WHERE product_id = 'homika_pro' AND plan_key = 'trial_7d'
        LIMIT 1`
    ).first();
    if (!plan) return { ready: false, reason: "trial_plan_missing" };

    // Confirm the licence columns required by self-service trial creation.
    const columns = await env.DB.prepare(`PRAGMA table_info(licenses)`).all();
    const names = new Set((columns.results || []).map((row) => String(row.name || "")));
    for (const required of [
      "id",
      "license_key",
      "product_id",
      "customer_id",
      "status",
      "expires_at",
      "max_devices",
      "plan_type",
      "plan_key",
    ]) {
      if (!names.has(required)) {
        return { ready: false, reason: `licenses_${required}_missing` };
      }
    }

    return { ready: true };
  } catch (err) {
    console.warn("Trial storage readiness check failed", err);
    return { ready: false, reason: "trial_schema_incomplete" };
  }
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
      plan_key: cleanString(license.plan_key, 40).toLowerCase(),
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
    plan_key: cleanString(license.plan_key, 40).toLowerCase(),
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
    `SELECT id, license_key, product_id, customer_id, status, plan_type, plan_key, expires_at, max_devices
       FROM licenses
      WHERE license_key = ?1
      LIMIT 1`
  ).bind(licenseKey).first();
}

async function getLicenseById(env, licenseId) {
  return env.DB.prepare(
    `SELECT id, license_key, product_id, customer_id, status, plan_type, plan_key, expires_at, max_devices
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
    "access-control-allow-headers": "content-type,authorization,x-homika-device-id,x-homika-backup-created-at,x-homika-record-count,x-homika-format-version,x-homika-database-schema-version,x-homika-payment-secret,x-homika-admin-secret",
    "cache-control": "no-store",
  };
}
