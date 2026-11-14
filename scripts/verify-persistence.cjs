const fs=require('fs'),assert=require('assert/strict');
const values=Object.fromEntries(fs.readFileSync('.verification/compose.env','utf8').split(/\r?\n/).filter(Boolean).map(line=>[line.slice(0,line.indexOf('=')),line.slice(line.indexOf('=')+1)]));
async function main(){
 const login=await fetch('http://127.0.0.1:18081/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({login:'verify-admin',password:values.KR3_VERIFY_NEW_PASSWORD})});
 assert.equal(login.status,200,'Persisted administrator must retain the changed password');
 const token=(await login.json()).accessToken,record=JSON.parse(fs.readFileSync('.verification/record.json','utf8'));
 const result=await fetch('http://127.0.0.1:18081/api/requests/'+record.id,{headers:{Authorization:'Bearer '+token}});
 assert.equal(result.status,200,'Request must survive ordinary compose down/up');assert.equal((await result.json()).code,record.code);
 console.log('Persistence passed: administrator password and request survived ordinary compose down/up.');
}
main().catch(error=>{console.error(error.message);process.exitCode=1;});
