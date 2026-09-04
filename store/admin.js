const API_BASE='https://app-license-api.nudroids.workers.dev';
const state={secret:sessionStorage.getItem('homika_admin_secret')||'',status:'submitted'};
const loginPanel=document.querySelector('#login-panel');
const dashboard=document.querySelector('#dashboard');
const loginForm=document.querySelector('#login-form');
const secretInput=document.querySelector('#admin-secret');
const loginError=document.querySelector('#login-error');
const logout=document.querySelector('#logout');
const filter=document.querySelector('#status-filter');
const list=document.querySelector('#payment-list');
const statusEl=document.querySelector('#admin-status');
const resultDialog=document.querySelector('#review-result-dialog');
const resultTitle=document.querySelector('#review-result-title');
const resultCopy=document.querySelector('#review-result-copy');
const resultLicenseWrap=document.querySelector('#review-result-license-wrap');
const resultLicense=document.querySelector('#review-result-license');
const resultCopyLicense=document.querySelector('#review-result-copy-license');
const resultCopyStatus=document.querySelector('#review-result-copy-status');
const resultClose=document.querySelector('#review-result-close');

function money(c){return `RM${(Number(c||0)/100).toFixed(0)}`}
function esc(value){return String(value??'').replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]))}
function planLabel(k){return ({'1_month':'1 Bulan','3_month':'3 Bulan','6_month':'6 Bulan','1_year':'1 Tahun'})[k]||k||'-'}
function customerStatusUrl(token){const u=new URL('./',location.href);u.searchParams.set('checkout',token);return u.toString()}
async function copyText(value,button,doneLabel){
  try{await navigator.clipboard.writeText(value);const before=button.textContent;button.textContent=doneLabel;setTimeout(()=>{button.textContent=before},1600)}
  catch(_){window.prompt('Salin:',value)}
}

async function api(path,options={}){
  const headers={Accept:'application/json','x-homika-admin-secret':state.secret,...(options.headers||{})};
  const r=await fetch(`${API_BASE}${path}`,{...options,headers});
  const b=await r.json().catch(()=>({ok:false,error:'invalid_response'}));
  if(!r.ok||!b.ok){const e=new Error(b.error||`http_${r.status}`);e.code=b.error||`http_${r.status}`;throw e}
  return b;
}

function showLogin(message=''){
  dashboard.classList.add('hidden');loginPanel.classList.remove('hidden');logout.classList.add('hidden');loginError.textContent=message;document.title='Homika Payment Admin';
}
function showDashboard(){loginPanel.classList.add('hidden');dashboard.classList.remove('hidden');logout.classList.remove('hidden')}

async function loadPayments(){
  statusEl.textContent='Memuatkan…';list.innerHTML='';
  try{
    const b=await api(`/v1/admin/payments?status=${encodeURIComponent(state.status)}`);
    showDashboard();renderPayments(b.items||[]);statusEl.textContent=`${(b.items||[]).length} rekod.`;document.title=state.status==='submitted'&&b.items?.length?`(${b.items.length}) Homika Payment Admin`:'Homika Payment Admin';
  }catch(err){
    if(err.code==='unauthorized'||err.code==='admin_not_configured'){sessionStorage.removeItem('homika_admin_secret');state.secret='';showLogin(err.code==='admin_not_configured'?'Admin Secret belum dikonfigurasi pada Worker.':'Admin Secret salah.');return}
    statusEl.textContent=`Gagal memuatkan (${err.code||'error'}).`;
  }
}

function renderPayments(items){
  if(!items.length){list.innerHTML='<div class="empty-state">Tiada bayaran dalam status ini.</div>';return}
  list.innerHTML='';
  items.forEach(item=>{
    const card=document.createElement('article');card.className='admin-payment';
    const statusUrl=item.checkout_token?customerStatusUrl(item.checkout_token):'';
    const completion=item.status==='approved'
      ?`<div class="admin-completion"><strong>Bayaran telah diluluskan ✓</strong>${item.action==='buy'&&item.license_key?`<p>Licence Key customer:</p><code>${esc(item.license_key)}</code>`:'<p>Lesen sedia ada customer telah dikemas kini. Tiada key baharu diperlukan.</p>'}<div class="admin-completion-actions">${item.license_key?'<button class="button secondary history-copy-license" type="button">Salin Licence Key</button>':''}${statusUrl?'<button class="button secondary history-copy-status" type="button">Salin link customer</button>':''}</div></div>`
      :item.status==='rejected'
        ?`<div class="admin-completion"><strong>Bayaran ditolak</strong><p>${esc(item.admin_note||'Bukti pembayaran tidak dapat disahkan.')}</p></div>`
        :'';
    card.innerHTML=`
      <div class="admin-payment-head"><div><div class="admin-reference">${esc(item.checkout_reference)}</div><div class="status-pill">${esc(item.status)}</div></div><div class="admin-amount">${money(item.amount_cents)}</div></div>
      <div class="admin-meta">
        <div>Email: <strong>${esc(item.email)}</strong></div><div>Pelan: <strong>${esc(planLabel(item.plan_key))}</strong></div>
        <div>Pembayar: <strong>${esc(item.payer_name)}</strong></div><div>Transaction ID: <strong>${esc(item.payment_reference||'-')}</strong></div>
        <div>Jenis: <strong>${item.action==='renew'?'Renew / Upgrade':'Pembelian baru'}</strong></div><div>Lesen: <strong>${esc(item.license_hint||'-')}</strong></div>
        <div>Dihantar: <strong>${esc(item.submitted_at||'-')}</strong></div><div>Checkout: <strong>${esc(item.checkout_status)}</strong></div>
      </div>
      ${item.admin_note&&item.status!=='rejected'?`<div class="checkout-result"><strong>Nota admin</strong><p>${esc(item.admin_note)}</p></div>`:''}
      ${completion}
      <button class="button secondary proof-button" type="button">Lihat bukti</button>
      <div class="proof-slot"></div>
      ${item.status==='submitted'?`<div class="admin-actions"><input class="admin-note" type="text" maxlength="500" placeholder="Nota admin (optional)"><button class="button danger reject-button" type="button">Reject</button><button class="button primary approve-button" type="button">Approve</button></div>`:''}
    `;
    card.querySelector('.proof-button').addEventListener('click',()=>loadProof(item.id,card.querySelector('.proof-slot')));
    card.querySelector('.approve-button')?.addEventListener('click',()=>review(item,'approve',card));
    card.querySelector('.reject-button')?.addEventListener('click',()=>review(item,'reject',card));
    card.querySelector('.history-copy-license')?.addEventListener('click',e=>copyText(item.license_key,e.currentTarget,'Licence Key disalin ✓'));
    card.querySelector('.history-copy-status')?.addEventListener('click',e=>copyText(statusUrl,e.currentTarget,'Link disalin ✓'));
    list.appendChild(card);
  });
}

async function loadProof(id,slot){
  slot.innerHTML='<div class="catalog-status">Memuatkan bukti…</div>';
  try{
    const r=await fetch(`${API_BASE}/v1/admin/payments/proof?id=${encodeURIComponent(id)}`,{headers:{'x-homika-admin-secret':state.secret}});
    if(!r.ok)throw new Error(`http_${r.status}`);
    const blob=await r.blob();const url=URL.createObjectURL(blob);
    slot.innerHTML='';const img=document.createElement('img');img.className='admin-proof';img.alt='Bukti pembayaran';img.src=url;img.onload=()=>setTimeout(()=>URL.revokeObjectURL(url),60000);slot.appendChild(img);
  }catch(err){slot.innerHTML=`<div class="admin-error">Bukti gagal dimuatkan (${esc(err.message)}).</div>`}
}

function showReviewResult(item,action,checkout){
  const approved=action==='approve';
  const statusUrl=checkout?.token?customerStatusUrl(checkout.token):(item.checkout_token?customerStatusUrl(item.checkout_token):'');
  const licenseKey=approved&&checkout?.action==='buy'?checkout.license_key||'':'';
  resultDialog.classList.toggle('is-rejected',!approved);
  document.querySelector('#review-result-icon').textContent=approved?'✓':'×';
  resultTitle.textContent=approved?'Bayaran berjaya diluluskan ✓':'Bayaran telah ditolak';
  if(approved&&checkout?.action==='buy'){
    resultCopy.textContent='Lesen Homika Pro baharu telah diaktifkan. Customer boleh ambil Licence Key melalui link semakan bayaran.';
  }else if(approved){
    resultCopy.textContent='Lesen sedia ada customer telah dikemas kini. Customer hanya perlu kembali ke Homika dan tekan Verify Now.';
  }else{
    resultCopy.textContent='Customer boleh buka semula link semakan dan upload bukti pembayaran yang betul.';
  }
  resultLicenseWrap.classList.toggle('hidden',!licenseKey);
  resultCopyLicense.classList.toggle('hidden',!licenseKey);
  resultLicense.textContent=licenseKey||'';
  resultCopyStatus.classList.toggle('hidden',!statusUrl);
  resultCopyLicense.onclick=licenseKey?()=>copyText(licenseKey,resultCopyLicense,'Licence Key disalin ✓'):null;
  resultCopyStatus.onclick=statusUrl?()=>copyText(statusUrl,resultCopyStatus,'Link customer disalin ✓'):null;
  if(!resultDialog.open)resultDialog.showModal();
}

async function review(item,action,card){
  const note=card.querySelector('.admin-note')?.value.trim()||'';
  const buttons=[...card.querySelectorAll('button')];buttons.forEach(b=>b.disabled=true);
  try{
    const response=await api('/v1/admin/payments/review',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({submission_id:item.id,action,admin_note:note})});
    await loadPayments();
    showReviewResult(item,action,response.checkout||null);
  }catch(err){
    if(err.code==='unauthorized'||err.code==='admin_not_configured'){
      sessionStorage.removeItem('homika_admin_secret');state.secret='';secretInput.value='';showLogin(err.code==='admin_not_configured'?'Admin Secret belum dikonfigurasi pada Worker.':'Sesi admin tamat atau Admin Secret tidak sah. Sila log masuk semula.');return;
    }
    alert(`Tindakan gagal (${err.code||'error'}).`);buttons.forEach(b=>b.disabled=false)
  }
}

resultClose.addEventListener('click',()=>resultDialog.close());
resultDialog.addEventListener('click',e=>{if(e.target===resultDialog)resultDialog.close()});

loginForm.addEventListener('submit',async e=>{e.preventDefault();state.secret=secretInput.value.trim();if(!state.secret)return;sessionStorage.setItem('homika_admin_secret',state.secret);await loadPayments()});
logout.addEventListener('click',()=>{sessionStorage.removeItem('homika_admin_secret');state.secret='';secretInput.value='';showLogin()});
filter.addEventListener('change',()=>{state.status=filter.value;loadPayments()});
document.querySelector('#refresh').addEventListener('click',loadPayments);

setInterval(()=>{if(state.secret&&!document.hidden&&state.status==='submitted')loadPayments()},30000);
if(state.secret)loadPayments();else showLogin();
