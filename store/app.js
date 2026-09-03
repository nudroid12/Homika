const API_BASE = 'https://app-license-api.nudroids.workers.dev';
const FALLBACK_PLANS = [
  { plan_key: '1_month', name: '1 Bulan', price_cents: 700, compare_at_price_cents: 1000, is_featured: false },
  { plan_key: '3_month', name: '3 Bulan', price_cents: 1900, compare_at_price_cents: 3000, is_featured: false },
  { plan_key: '6_month', name: '6 Bulan', price_cents: 3500, compare_at_price_cents: 6000, is_featured: false },
  { plan_key: '1_year', name: '1 Tahun', price_cents: 5900, compare_at_price_cents: 12000, is_featured: true },
];

const planLabels = {
  '1_month': '1 Bulan',
  '3_month': '3 Bulan',
  '6_month': '6 Bulan',
  '1_year': '1 Tahun',
};

const state = { plans: FALLBACK_PLANS };
const plansEl = document.querySelector('#plans');
const statusEl = document.querySelector('#catalog-status');
const renewPlanEl = document.querySelector('#renew-plan');
const dialog = document.querySelector('#checkout-dialog');
const dialogTitle = document.querySelector('#dialog-title');
const dialogCopy = document.querySelector('#dialog-copy');
const dialogSummary = document.querySelector('#dialog-summary');

function money(cents) {
  return `RM${(Number(cents || 0) / 100).toFixed(0)}`;
}

function saving(plan) {
  const oldPrice = Number(plan.compare_at_price_cents || 0);
  const price = Number(plan.price_cents || 0);
  return Math.max(0, oldPrice - price);
}

function renderPlans(plans) {
  plansEl.innerHTML = '';
  renewPlanEl.innerHTML = '';

  plans.forEach((plan) => {
    const label = planLabels[plan.plan_key] || plan.name || plan.plan_key;
    const card = document.createElement('article');
    card.className = `plan${plan.is_featured ? ' featured' : ''}`;
    card.innerHTML = `
      ${plan.is_featured ? '<div class="badge">PALING BERBALOI</div>' : ''}
      <div class="plan-name">${label}</div>
      <div class="price-old">${money(plan.compare_at_price_cents)}</div>
      <div class="price">${money(plan.price_cents)}</div>
      <div class="saving">Harga pelancaran · Jimat ${money(saving(plan))}</div>
      <ul>
        <li>Sehingga 3 peranti</li>
        <li>Homika Cloud Sync</li>
        <li>Encrypted Cloud Backup</li>
        <li>Semua fungsi Homika Pro</li>
      </ul>
      <button class="button ${plan.is_featured ? 'primary' : 'secondary'}" type="button">Pilih ${label}</button>
    `;
    card.querySelector('button').addEventListener('click', () => showCheckout('buy', plan, ''));
    plansEl.appendChild(card);

    const option = document.createElement('option');
    option.value = plan.plan_key;
    option.textContent = `${label} · ${money(plan.price_cents)}`;
    renewPlanEl.appendChild(option);
  });
}

function showCheckout(action, plan, licenceKey) {
  const label = planLabels[plan.plan_key] || plan.name || plan.plan_key;
  dialogTitle.textContent = action === 'renew' ? 'Renew Homika Pro' : 'Beli Homika Pro';
  dialogCopy.textContent = action === 'renew'
    ? 'Kod lesen yang sama akan digunakan. Tempoh baru akan ditambah selepas entitlement semasa.'
    : 'Pelan anda sudah dipilih dan checkout foundation sedia untuk payment gateway.';
  dialogSummary.textContent = `${label} · ${money(plan.price_cents)}${licenceKey ? ` · ${licenceKey}` : ''}`;
  dialog.showModal();
}

async function loadCatalog() {
  try {
    const response = await fetch(`${API_BASE}/v1/store/catalog`, { headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error('catalog_http');
    const body = await response.json();
    if (!body.ok || !Array.isArray(body.plans) || body.plans.length === 0) throw new Error('catalog_invalid');
    state.plans = body.plans;
    statusEl.textContent = 'Harga terkini dari Homika licence server.';
  } catch (_) {
    state.plans = FALLBACK_PLANS;
    statusEl.textContent = 'Menggunakan harga pelancaran yang disimpan pada halaman ini.';
  }
  renderPlans(state.plans);
}

document.querySelector('#renew-form').addEventListener('submit', (event) => {
  event.preventDefault();
  const licence = document.querySelector('#renew-license').value.trim().toUpperCase();
  const plan = state.plans.find((item) => item.plan_key === renewPlanEl.value);
  if (!plan) return;
  if (!licence) {
    document.querySelector('#renew-license').focus();
    return;
  }
  showCheckout('renew', plan, licence);
});

document.querySelector('.dialog-close').addEventListener('click', () => dialog.close());
dialog.addEventListener('click', (event) => {
  if (event.target === dialog) dialog.close();
});

loadCatalog().then(() => {
  const params = new URLSearchParams(window.location.search);
  const action = params.get('action');
  if (action === 'renew') document.querySelector('#renew').scrollIntoView();
  if (action === 'buy') document.querySelector('#pricing').scrollIntoView();
});
