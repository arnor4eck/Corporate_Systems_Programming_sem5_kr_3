const fs = require('fs');
const assert = require('assert/strict');
const env = Object.fromEntries(fs.readFileSync('.verification/compose.env', 'utf8').split(/\r?\n/).filter(Boolean).map(line => [line.slice(0, line.indexOf('=')), line.slice(line.indexOf('=')+1)]));
const base = 'http://127.0.0.1:18081';
let token;
async function call(method, path, body, expected = 200, authorized = true) {
  const headers = {};
  if (body !== undefined && !(body instanceof FormData)) headers['Content-Type'] = 'application/json';
  if (authorized && token) headers.Authorization = 'Bearer ' + token;
  const response = await fetch(base + path, {method, headers, body: body === undefined ? undefined : body instanceof FormData ? body : JSON.stringify(body)});
  const text = await response.text();
  // Never echo response bodies: login responses contain tokens.
  assert.equal(response.status, expected, `${method} ${path}: expected ${expected}, got ${response.status}`);
  return text ? JSON.parse(text) : null;
}
async function csv(path, text, mode, previewToken) {
  const form = new FormData(); form.set('file', new Blob([text], {type:'text/csv'}), 'requests.csv'); form.set('mode',mode);
  if (previewToken) form.set('previewToken',previewToken);
  return call('POST',path,form);
}
async function main() {
  assert.equal((await call('GET','/actuator/health',undefined,200,false)).status,'UP');
  let loginResponse = await fetch(base+'/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({login:'verify-admin',password:env.KR3_VERIFY_ADMIN_PASSWORD})});
  let login;
  if (loginResponse.status === 200) {
    login = await loginResponse.json(); token = login.accessToken;
    if (login.user.passwordChangeRequired) await call('POST','/api/auth/change-password',{currentPassword:env.KR3_VERIFY_ADMIN_PASSWORD,newPassword:env.KR3_VERIFY_NEW_PASSWORD},204);
  }
  login = await call('POST','/api/auth/login',{login:'verify-admin',password:env.KR3_VERIFY_NEW_PASSWORD},200,false); token=login.accessToken;
  const suffix = Date.now();
  const sector = await call('POST','/api/sectors',{name:'Acceptance '+suffix},201);
  const customer = await call('POST','/api/customers',{fullName:'Acceptance customer',phone:'123'},201);
  const plot = await call('POST','/api/plots',{sectorId:sector.id,rowNumber:1,plotNumber:1,plotStatus:'FREE',lengthCm:200,widthCm:100},201);
  const payload={customerId:customer.id,plotId:plot.id,requestStatus:'NEW',requestDate:'2026-11-14T12:00:00',totalCost:2500,title:'Проверка заявки',code:'VERIFY-'+suffix,urgent:true};
  const request=await call('POST','/api/requests',payload,201);
  assert.equal(request.priority,'URGENT');
  let changed=await call('PUT','/api/requests/'+request.id,{...payload,version:request.version,totalCost:3000});
  await call('PUT','/api/requests/'+request.id,{...payload,version:request.version},409);
  const tag=await call('POST','/api/tags',{name:'verify-'+suffix},201);
  changed=await call('POST','/api/requests/'+request.id+'/tags',{tagId:tag.id,version:changed.version});
  const notes=await call('POST','/api/requests/'+request.id+'/notes',{content:'Проверка комментария',pinned:true},201);
  assert.equal(notes.authorId,login.user.id);
  const found=await call('GET','/api/requests?tagId='+tag.id+'&urgent=true&priority=URGENT&page=0&size=1');
  assert.equal(found.totalElements,1);
  const bulk=await call('POST','/api/requests/bulk',{operation:'SET_STATUS',requestStatus:'PROCESSING',items:[{id:request.id,version:changed.version},{id:'11111111-1111-4111-8111-111111111111',version:0}]});
  assert.equal(bulk.succeeded,1); assert.equal(bulk.failed,1); changed=bulk.results[0].record;
  const text='code,version,customerId,plotId,requestStatus,requestDate,totalCost,title,urgent\n'+[payload.code,changed.version,customer.id,plot.id,'PROCESSING',payload.requestDate,3500,'CSV external code',true].join(',')+'\n';
  const preview=await csv('/api/requests/import/preview',text,'UPSERT'); assert.equal(preview.invalidRows,0);
  const imported=await csv('/api/requests/import/apply',text,'UPSERT',preview.previewToken); assert.equal(imported.succeeded,1); assert.equal(imported.results[0].id,request.id);
  const stats=await call('GET','/api/analytics/summary'); assert.ok(stats.requestsByPriority.some(group=>group.key==='URGENT'&&group.count>0));
  const events=await call('GET','/api/admin/audit?entityId='+request.id); assert.ok(events.totalElements>=4);
  const spec=await fetch(base+'/v3/api-docs'); assert.equal(spec.status,200); assert.ok((await spec.json()).paths['/api/requests/bulk']);
  const rotated=await call('POST','/api/auth/refresh',{refreshToken:login.refreshToken},200,false);
  await call('GET','/api/auth/me',undefined,401);
  token=rotated.accessToken;
  await call('POST','/api/auth/logout',{refreshToken:rotated.refreshToken},204,false);
  await call('GET','/api/auth/me',undefined,401);
  fs.writeFileSync('.verification/record.json',JSON.stringify({id:request.id,code:payload.code}));
  console.log('PostgreSQL/Docker acceptance passed: health, authentication, version conflict, tags, comments, filtering, bulk, external-code CSV, analytics, audit and OpenAPI.');
}
main().catch(error=>{console.error(error.message);process.exitCode=1;});
