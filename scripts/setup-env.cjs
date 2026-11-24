const fs = require('fs');
const crypto = require('crypto');
const path = require('path');
const root = path.resolve(__dirname, '..');
const target = path.join(root, '.env');
const existed = fs.existsSync(target);
let contents = existed ? fs.readFileSync(target, 'utf8') : fs.readFileSync(path.join(root, '.env.example'), 'utf8');
const defaults = {
  DB_NAME: 'pks_3', DB_USERNAME: 'pks_3', DB_PASSWORD: crypto.randomBytes(24).toString('hex'),
  DB_BIND_ADDRESS: '127.0.0.1', DB_PORT: '5432', SERVER_PORT: '8080',
  ADMIN_LOGIN: 'admin', ADMIN_PASSWORD: crypto.randomBytes(24).toString('base64url'),
  JWT_SECRET: crypto.randomBytes(32).toString('base64')
};
for (const [key, generated] of Object.entries(defaults)) {
  const pattern = new RegExp('^' + key + '=(.*)$', 'm');
  const match = contents.match(pattern);
  const value = match ? match[1].trim() : '';
  const placeholder = /^(replace_with_|change_me_|your-|your_|\s*$)/.test(value);
  if (!match) contents += '\n' + key + '=' + generated + '\n';
  else if (!value || (placeholder && (['ADMIN_PASSWORD', 'JWT_SECRET'].includes(key) || (!existed && key === 'DB_PASSWORD')))) contents = contents.replace(pattern, key + '=' + generated);
}
fs.writeFileSync(target, contents);
console.log('Local .env prepared. Existing database settings were preserved; secrets were not printed.');
