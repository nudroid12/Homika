const API_BASE='https://app-license-api.nudroids.workers.dev';
const FALLBACK_PLANS=[
  {plan_key:'1_month',name:'1 Bulan',price_cents:700,compare_at_price_cents:1000,currency:'MYR',is_featured:false},
  {plan_key:'3_month',name:'3 Bulan',price_cents:1900,compare_at_price_cents:3000,currency:'MYR',is_featured:false},
  {plan_key:'6_month',name:'6 Bulan',price_cents:3500,compare_at_price_cents:6000,currency:'MYR',is_featured:false},
  {plan_key:'1_year',name:'1 Tahun',price_cents:5900,compare_at_price_cents:12000,currency:'MYR',is_featured:true}
];
const labels={'1_month':'1 Bulan','3_month':'3 Bulan','6_month':'6 Bulan','1_year':'1 Tahun'};
const state={
  plans:FALLBACK_PLANS,
  selectedPlan:null,
  checkoutToken:new URLSearchParams(location.search).get('checkout')||'',
  checkout:null,
  paymentConfig:{enabled:false},
  pollTimer:null
};

const plansEl=document.querySelector('#plans');
const statusEl=document.querySelector('#catalog-status');
const renewPlanEl=document.querySelector('#renew-plan');
const dialog=document.querySelector('#checkout-dialog');
const dialogTitle=document.querySelector('#dialog-title');
const dialogCopy=document.querySelector('#dialog-copy');
const dialogSummary=document.querySelector('#dialog-summary');
const checkoutResult=document.querySelector('#checkout-result');
const checkoutForm=document.querySelector('#checkout-form');
const checkoutEmail=document.querySelector('#checkout-email');
const checkoutCreate=document.querySelector('#checkout-create');
const paymentPanel=document.querySelector('#payment-panel');
const paymentQr=document.querySelector('#payment-qr');
const paymentQrFrame=document.querySelector('#payment-qr-frame');
const qrActions=document.querySelector('#qr-actions');
const openQr=document.querySelector('#open-qr');
const saveQr=document.querySelector('#save-qr');
const samePhoneTip=document.querySelector('#same-phone-tip');
const copyStatusLink=document.querySelector('#copy-status-link');
const paymentName=document.querySelector('#payment-name');
const paymentAmount=document.querySelector('#payment-amount');
const paymentReference=document.querySelector('#payment-reference');
const proofForm=document.querySelector('#proof-form');
const payerName=document.querySelector('#payer-name');
const transactionRef=document.querySelector('#transaction-ref');
const proofFile=document.querySelector('#proof-file');
const proofSubmit=document.querySelector('#proof-submit');
const paymentStatus=document.querySelector('#payment-status');

function money(c){return `RM${(Number(c||0)/100).toFixed(0)}`}
function saving(p){return Math.max(0,Number(p.compare_at_price_cents||0)-Number(p.price_cents||0))}
function labelFor(p){return labels[p.plan_key]||p.name||p.plan_key}
function escapeHtml(value){return String(value??'').replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]))}
async function copyValue(value,button,doneLabel){
  try{await navigator.clipboard.writeText(value);const before=button.textContent;button.textContent=doneLabel;setTimeout(()=>{button.textContent=before},1600)}
  catch(_){window.prompt('Salin:',value)}
}

function checkoutStatusUrl(){
  const url=new URL(location.href);
  if(state.checkoutToken)url.searchParams.set('checkout',state.checkoutToken);
  return url.toString();
}
function persistCheckoutToken(token){
  state.checkoutToken=token||state.checkoutToken;
  if(!state.checkoutToken)return;
  const url=new URL(location.href);
  url.searchParams.set('checkout',state.checkoutToken);
  history.replaceState(null,'',url);
}

async function api(path,options={}){
  const r=await fetch(`${API_BASE}${path}`,{headers:{Accept:'application/json',...(options.headers||{})},...options});
  const b=await r.json().catch(()=>({ok:false,error:'invalid_response'}));
  if(!r.ok||!b.ok){const e=new Error(b.error||`http_${r.status}`);e.code=b.error||`http_${r.status}`;throw e}
  return b;
}

function renderPlans(plans){
  plansEl.innerHTML='';
  renewPlanEl.innerHTML='';
  plans.forEach(plan=>{
    const label=labelFor(plan),card=document.createElement('article');
    card.className=`plan${plan.is_featured?' featured':''}`;
    card.innerHTML=`${plan.is_featured?'<div class="badge">PALING BERBALOI</div>':''}<div class="plan-name">${escapeHtml(label)}</div><div class="price-old">${money(plan.compare_at_price_cents)}</div><div class="price">${money(plan.price_cents)}</div><div class="saving">Harga pelancaran · Jimat ${money(saving(plan))}</div><ul><li>Sehingga 3 peranti</li><li>Homika Cloud Sync</li><li>Encrypted Cloud Backup</li><li>Semua fungsi Homika Pro</li></ul><button class="button ${plan.is_featured?'primary':'secondary'}" type="button">Pilih ${escapeHtml(label)}</button>`;
    card.querySelector('button').addEventListener('click',()=>choosePlan(plan));
    plansEl.appendChild(card);
    const o=document.createElement('option');o.value=plan.plan_key;o.textContent=`${label} · ${money(plan.price_cents)}`;renewPlanEl.appendChild(o);
  });
}

function resetCheckoutDialog(){
  dialogSummary.classList.add('hidden');
  checkoutResult.classList.add('hidden');
  paymentPanel.classList.add('hidden');
  checkoutForm.classList.remove('hidden');
  proofForm.reset();
  paymentStatus.innerHTML='';
}

function choosePlan(plan){
  state.selectedPlan=plan;
  dialogTitle.textContent=state.checkoutToken?'Upgrade / Renew Homika Pro':'Beli Homika Pro';
  dialogCopy.textContent=state.checkoutToken?'Lesen semasa akan dikekalkan. Masukkan email untuk rekod pembelian.':'Masukkan email dahulu, kemudian bayar melalui QR.';
  resetCheckoutDialog();
  dialog.showModal();
  checkoutEmail.focus();
}

function configureQr(config){
  const ok=Boolean(config?.enabled&&config?.qr_url);
  paymentQrFrame.classList.toggle('hidden',!ok);
  qrActions.classList.toggle('hidden',!ok);
  samePhoneTip.classList.toggle('hidden',!ok);
  if(!ok){
    paymentQr.removeAttribute('src');
    openQr.removeAttribute('href');
    saveQr.removeAttribute('href');
    return false;
  }
  paymentQr.src=config.qr_url;
  paymentQr.alt='Touch n Go / DuitNow QR pembayaran Homika Pro';
  openQr.href=config.qr_url;
  saveQr.href=config.qr_url;
  return true;
}


function telegramOptInHtml(c){
  const tg=c?.telegram_notification;
  if(!tg?.available)return '';
  if(tg.linked){
    return `<div class="telegram-optin connected"><strong>Telegram disambungkan ✓</strong><p>Anda boleh tutup halaman ini. Homika Bot akan mesej anda sebaik sahaja bayaran diluluskan atau ditolak.</p></div>`;
  }
  return `<div class="telegram-optin"><strong>Dapatkan keputusan terus di Telegram</strong><p>Email kekal sebagai backup. Sambungkan Homika Bot supaya anda terus dapat notifikasi walaupun halaman ini sudah ditutup.</p><button class="button telegram-button" type="button" data-connect-telegram>Aktifkan Notifikasi Telegram</button><small>Tekan butang di atas, kemudian tekan <b>START</b> dalam Telegram.</small></div>`;
}

function wireTelegramOptIn(c){
  const button=paymentStatus.querySelector('[data-connect-telegram]');
  if(!button)return;
  button.addEventListener('click',async()=>{
    button.disabled=true;
    const before=button.textContent;
    button.textContent='Menyambungkan…';
    try{
      const b=await api('/v1/store/telegram/link',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({checkout_token:state.checkoutToken})});
      if(b.telegram?.linked){
        const latest=await api(`/v1/store/checkout?token=${encodeURIComponent(state.checkoutToken)}`);
        renderCheckout(latest.checkout);
        return;
      }
      if(!b.telegram?.link_url)throw Object.assign(new Error('telegram_link_unavailable'),{code:'telegram_link_unavailable'});
      location.href=b.telegram.link_url;
    }catch(err){
      button.disabled=false;
      button.textContent=before;
      const note=document.createElement('p');
      note.className='telegram-error';
      note.textContent=`Telegram belum dapat disambungkan (${err.code||'error'}). Email masih akan digunakan sebagai backup.`;
      button.closest('.telegram-optin')?.appendChild(note);
    }
  });
}

function renderManualPayment(c){
  if(c.status==='completed'){
    paymentPanel.classList.add('hidden');
    return;
  }
  paymentPanel.classList.remove('hidden');
  paymentAmount.textContent=money(c.amount_cents);
  paymentReference.textContent=c.reference;
  const qrReady=configureQr(state.paymentConfig);

  if(qrReady){
    paymentName.textContent=state.paymentConfig.display_name?`Bayar kepada ${state.paymentConfig.display_name}`:'Touch n Go eWallet / DuitNow QR';
    proofForm.classList.remove('hidden');
  }else{
    paymentName.textContent='QR pembayaran belum tersedia.';
    proofForm.classList.add('hidden');
  }

  const submission=c.manual_payment;
  if(!submission){
    paymentStatus.innerHTML='<strong>Selepas bayar</strong><p>Upload screenshot resit sahaja. Pengesahan dibuat secara manual, kebiasaannya dalam beberapa jam. Jika admin tidak tersedia, ia mungkin disahkan pada hari berikutnya.</p>';
    return;
  }
  if(submission.status==='submitted'){
    proofForm.classList.add('hidden');
    paymentStatus.innerHTML=`<strong>Bayaran diterima ✓</strong><p>Bukti pembayaran sudah dihantar${submission.submitted_at?` pada ${escapeHtml(submission.submitted_at)}`:''}. Menunggu pengesahan manual. Tak perlu buat bayaran kali kedua.</p>${telegramOptInHtml(c)}<button class="status-link-button" type="button" data-copy-status-inline>Salin link semakan bayaran</button>`;
    paymentStatus.querySelector('[data-copy-status-inline]')?.addEventListener('click',e=>copyValue(checkoutStatusUrl(),e.currentTarget,'Link disalin ✓'));
    wireTelegramOptIn(c);
    return;
  }
  if(submission.status==='rejected'){
    proofForm.classList.add('hidden');
    paymentStatus.innerHTML=`<strong>Bayaran tidak dapat disahkan.</strong><p><b>Sebab:</b> ${escapeHtml(submission.admin_note||'Bukti pembayaran tidak dapat disahkan.')}</p><p>Order ini telah ditutup. Sila buat <b>order baru</b> dan muat naik resit/bukti pembayaran yang betul.</p>`;
    return;
  }
  if(submission.status==='approved'){
    proofForm.classList.add('hidden');
    paymentStatus.innerHTML='<strong>Bayaran diluluskan.</strong><p>Sedang mengemaskini lesen...</p>';
  }
}

function renderCheckout(c){
  state.checkout=c;
  persistCheckoutToken(c.token||state.checkoutToken);
  const plan=state.plans.find(p=>p.plan_key===c.plan_key);
  const planText=plan?labelFor(plan):'Pilih pelan';
  const amount=c.amount_cents==null?'Belum dipilih':money(c.amount_cents);
  const lic=c.license_hint?` · ${escapeHtml(c.license_hint)}`:'';
  dialogSummary.innerHTML=`<div><strong>${escapeHtml(c.reference)}</strong></div><div>${c.action==='renew'?'Renewal':'Pembelian'} · ${escapeHtml(planText)} · ${amount}${lic}</div><div>Status: <strong>${escapeHtml(c.status)}</strong></div>`;
  dialogSummary.classList.remove('hidden');
  checkoutResult.classList.remove('hidden');

  if(c.status==='completed'){
    checkoutForm.classList.add('hidden');
    paymentPanel.classList.add('hidden');
    const statusUrl=checkoutStatusUrl();
    if(c.action==='buy'&&c.license_key){
      checkoutResult.innerHTML=`<strong>Bayaran diluluskan ✓</strong><p>Homika Pro anda telah aktif. Ini Licence Key anda:</p><code class="customer-license-key">${escapeHtml(c.license_key)}</code><div class="checkout-result-actions"><button class="button primary" type="button" data-copy-license>Salin Licence Key</button><button class="button secondary" type="button" data-copy-status>Salin link semakan</button></div><p>Simpan Licence Key ini, kemudian buka Homika dan aktifkan lesen. Jika halaman ini ditutup, buka semula link semakan untuk melihat key ini lagi.</p>`;
      checkoutResult.querySelector('[data-copy-license]')?.addEventListener('click',e=>copyValue(c.license_key,e.currentTarget,'Licence Key disalin ✓'));
      checkoutResult.querySelector('[data-copy-status]')?.addEventListener('click',e=>copyValue(statusUrl,e.currentTarget,'Link disalin ✓'));
    }else{
      checkoutResult.innerHTML=`<strong>Bayaran diluluskan ✓</strong><p>Lesen Homika anda telah dikemas kini. <b>Tiada Licence Key baharu</b> kerana upgrade/renewal mengekalkan lesen yang sama.</p><p>Kembali ke Homika dan tekan <b>Verify Now / Semak sekarang</b> untuk mendapatkan tempoh dan pelan terkini.</p><div class="checkout-result-actions"><button class="button secondary" type="button" data-copy-status>Salin link semakan</button></div>`;
      checkoutResult.querySelector('[data-copy-status]')?.addEventListener('click',e=>copyValue(statusUrl,e.currentTarget,'Link disalin ✓'));
    }
    document.title='Bayaran diluluskan · Homika Pro';
    stopPolling();
  }else{
    checkoutResult.innerHTML='<strong>Bayaran melalui QR.</strong><p>Pengesahan dibuat secara manual. Kebiasaannya dalam beberapa jam, tetapi jika admin tidak tersedia pengesahan mungkin dibuat pada hari berikutnya.</p>';
    renderManualPayment(c);
  }
}

async function createSelectedCheckout(email){
  const plan=state.selectedPlan;if(!plan)return;
  if(state.checkoutToken){
    const b=await api('/v1/store/checkout/select-plan',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({checkout_token:state.checkoutToken,plan_key:plan.plan_key,email})});
    renderCheckout(b.checkout);startPolling();
  }else{
    const b=await api('/v1/store/checkout-intents',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({action:'buy',plan_key:plan.plan_key,email})});
    renderCheckout(b.checkout);startPolling();
  }
}

async function loadCatalog(){
  try{
    const b=await api('/v1/store/catalog');
    if(!Array.isArray(b.plans)||!b.plans.length)throw new Error();
    state.plans=b.plans;
    state.paymentConfig=b.manual_payment||{enabled:false};
    statusEl.textContent='Harga terkini dari Homika licence server.';
  }catch(_){
    state.plans=FALLBACK_PLANS;
    state.paymentConfig={enabled:false};
    statusEl.textContent='Menggunakan harga pelancaran yang disimpan pada halaman ini.';
  }
  renderPlans(state.plans);
}

async function loadCheckoutToken(){
  if(!state.checkoutToken)return;
  try{
    const b=await api(`/v1/store/checkout?token=${encodeURIComponent(state.checkoutToken)}`);
    state.checkout=b.checkout;
    if(b.checkout.status==='completed'||b.checkout.manual_payment){
      dialogTitle.textContent='Homika Pro';dialogCopy.textContent='Status checkout anda.';checkoutForm.classList.add('hidden');dialog.showModal();renderCheckout(b.checkout);
      if(b.checkout.status!=='completed'&&b.checkout.manual_payment?.status==='submitted')startPolling();
    }else{
      statusEl.textContent=b.checkout.license_hint?`Renewal dipaut kepada lesen ${b.checkout.license_hint}. Pilih pelan di bawah.`:'Sesi renewal aktif. Pilih pelan di bawah.';
      document.querySelector('#pricing').scrollIntoView();
    }
  }catch(_){state.checkoutToken=''}
}

function startPolling(){
  stopPolling();if(!state.checkoutToken)return;
  state.pollTimer=setInterval(async()=>{try{renderCheckout((await api(`/v1/store/checkout?token=${encodeURIComponent(state.checkoutToken)}`)).checkout)}catch(_){ }},8000);
}
function stopPolling(){if(state.pollTimer)clearInterval(state.pollTimer);state.pollTimer=null}

function fileToBase64(file){
  return new Promise((resolve,reject)=>{
    const reader=new FileReader();
    reader.onload=()=>resolve(String(reader.result||'').split(',').pop()||'');
    reader.onerror=()=>reject(new Error('file_read_failed'));
    reader.readAsDataURL(file);
  });
}

checkoutForm.addEventListener('submit',async e=>{
  e.preventDefault();
  const email=checkoutEmail.value.trim().toLowerCase();if(!email)return;
  checkoutCreate.disabled=true;checkoutCreate.textContent='Menyediakan…';
  try{await createSelectedCheckout(email);checkoutForm.classList.add('hidden')}
  catch(err){checkoutResult.classList.remove('hidden');checkoutResult.textContent=`Checkout tidak dapat disediakan (${err.code||'error'}).`}
  finally{checkoutCreate.disabled=false;checkoutCreate.textContent='Teruskan ke pembayaran QR'}
});

proofForm.addEventListener('submit',async e=>{
  e.preventDefault();
  if(!state.checkoutToken)return;
  const file=proofFile.files?.[0];
  if(!file)return;
  if(!['image/jpeg','image/png','image/webp'].includes(file.type)){paymentStatus.innerHTML='<strong>Format tidak disokong.</strong><p>Gunakan JPG, PNG atau WebP.</p>';return}
  if(file.size>2*1024*1024){paymentStatus.innerHTML='<strong>Fail terlalu besar.</strong><p>Maksimum 2 MB.</p>';return}
  proofSubmit.disabled=true;proofSubmit.textContent='Menghantar…';
  try{
    const proof=await fileToBase64(file);
    const b=await api('/v1/store/manual-payment/submit',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({checkout_token:state.checkoutToken,payer_name:payerName.value.trim(),payment_reference:transactionRef.value.trim(),proof_content_type:file.type,proof_base64:proof})});
    renderCheckout(b.checkout);startPolling();
  }catch(err){paymentStatus.innerHTML=`<strong>Bukti gagal dihantar.</strong><p>${escapeHtml(err.code||'error')}</p>`}
  finally{proofSubmit.disabled=false;proofSubmit.textContent='Saya dah bayar, hantar bukti'}
});

copyStatusLink.addEventListener('click',async()=>{
  try{
    await navigator.clipboard.writeText(checkoutStatusUrl());
    copyStatusLink.textContent='Link disalin ✓';
    setTimeout(()=>{copyStatusLink.textContent='Salin link semakan bayaran'},1800);
  }catch(_){
    window.prompt('Salin link ini untuk semak status bayaran:',checkoutStatusUrl());
  }
});

document.querySelector('#renew-form').addEventListener('submit',async e=>{
  e.preventDefault();
  const email=document.querySelector('#renew-email').value.trim().toLowerCase();
  const lic=document.querySelector('#renew-license').value.trim().toUpperCase();
  const plan=state.plans.find(i=>i.plan_key===renewPlanEl.value);
  if(!email||!lic||!plan)return;
  try{
    const b=await api('/v1/store/checkout-intents',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({action:'renew',email,license_key:lic,plan_key:plan.plan_key})});
    persistCheckoutToken(b.checkout.token);state.selectedPlan=plan;
    dialogTitle.textContent='Renew Homika Pro';dialogCopy.textContent='Kod lesen yang sama akan digunakan selepas bayaran disahkan.';
    checkoutForm.classList.add('hidden');dialog.showModal();renderCheckout(b.checkout);startPolling();
  }catch(err){alert(`Renewal tidak dapat disediakan (${err.code||'error'}).`)}
});

document.querySelector('.dialog-close').addEventListener('click',()=>dialog.close());
dialog.addEventListener('click',e=>{if(e.target===dialog)dialog.close()});

(async()=>{
  await loadCatalog();
  await loadCheckoutToken();
  const action=new URLSearchParams(location.search).get('action');
  if(action==='renew'&&!state.checkoutToken)document.querySelector('#renew').scrollIntoView();
  if(action==='buy')document.querySelector('#pricing').scrollIntoView();
})();
