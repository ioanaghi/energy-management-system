import { readFileSync } from 'node:fs';
import { randomUUID, createHmac } from 'node:crypto';

const base = process.env.ENERGY_URL || 'http://localhost:8085';
let config = {};
try { config = Object.fromEntries(readFileSync('.env', 'utf8').split('\n').filter(line => line.includes('=') && !line.startsWith('#')).map(line => { const i = line.indexOf('='); return [line.slice(0, i), line.slice(i + 1)]; })); } catch {}
let checks = 0;

async function call(method, path, body, token, expected = 200) {
  const response = await fetch(base + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {})
  });
  const raw = await response.text();
  if (response.status !== expected) throw new Error(`${method} ${path}: expected ${expected}, got ${response.status}: ${raw.slice(0, 300)}`);
  checks++;
  await new Promise(resolve => setTimeout(resolve, 225));
  try { return JSON.parse(raw); } catch { return raw || null; }
}

function assert(value, message) { if (!value) throw new Error(message); }
const login = await call('POST', '/api/auth/login', {username: config.ADMIN_USERNAME || 'admin', password: process.env.ADMIN_PASSWORD || config.ADMIN_PASSWORD || 'Admin123!'});
const admin = login.access_token;
const prefix = 'test_' + randomUUID().slice(0, 8);
const accounts = [];
const meters = [];
try {
  for (const [asset, type] of [['/', 'text/html'], ['/app.js', 'application/javascript'], ['/style.css', 'text/css']]) {
    const response = await fetch(base + asset);
    assert(response.headers.get('content-type')?.includes(type), 'Wrong MIME type for ' + asset);
  }
  await call('GET', '/api/users', undefined, undefined, 401);
  await call('GET', '/api/devices', undefined, undefined, 401);
  await call('PUT', '/api/auth/me', {name: 'Forbidden', address: 'Forbidden'}, undefined, 401);
  await call('POST', '/api/auth/login', {username: prefix, password: 'Wrong123!'}, undefined, 401);
  for (const suffix of ['a', 'b']) {
    const user = await call('POST', '/api/auth/register', {username: prefix + suffix, password: 'Client123!', role: 'ADMIN', name: 'Test client'}, undefined, 201);
    accounts.push(user.id);
    assert(user.role === 'CLIENT', 'Registration must not grant administrator access');
  }
  await call('POST', '/api/auth/register', {username: prefix + 'a', password: 'Client123!'}, undefined, 409);
  await call('POST', '/api/auth/register', {username: prefix + 'x', password: 'short'}, undefined, 400);
  const client = (await call('POST', '/api/auth/login', {username: prefix + 'a', password: 'Client123!'})).access_token;
  await call('GET', '/api/users', undefined, client, 403);
  const updatedProfile = await call('PUT', '/api/auth/me', {name: 'Updated client', address: 'New address'}, client);
  assert(updatedProfile.name === 'Updated client' && updatedProfile.address === 'New address', 'Client profile update must persist');
  assert(updatedProfile.role === 'CLIENT' && updatedProfile.username === prefix + 'a', 'Profile update must preserve role and username');
  const savedProfile = await call('GET', '/api/auth/me', undefined, client);
  assert(savedProfile.name === 'Updated client' && savedProfile.address === 'New address', 'Updated profile must survive a reload');
  for (const field of ['role', 'username', 'password', 'id']) {
    await call('PUT', '/api/auth/me', {name: 'Updated client', address: 'New address', [field]: field === 'id' ? accounts[1] : 'ADMIN'}, client, 400);
  }
  await call('PUT', '/api/auth/me', {name: 'x'.repeat(151), address: 'New address'}, client, 400);
  await call('PUT', '/api/auth/me', {name: 123, address: 'New address'}, client, 400);
  await call('PUT', '/api/auth/me', {name: 'Updated client', address: 'x'.repeat(251)}, client, 400);
  await call('POST', '/api/auth/login', {username: prefix + 'a', password: 'Client123!'});
  await call('PUT', '/api/users/' + accounts[1], {name: 'Forbidden', address: 'Forbidden'}, client, 403);
  await call('DELETE', '/api/devices/999999', undefined, client, 403);
  await call('POST', '/api/devices', {name: 'Forbidden', maxConsumption: 1}, client, 403);
  await call('POST', '/api/users', {username: prefix + 'c', role: 'CLIENT'}, admin, 400);
  for (const [index, userId] of accounts.entries()) {
    const meter = await call('POST', '/api/devices', {name: 'Test meter ' + index, maxConsumption: 4.5, address: 'Test address', userId}, admin, 201);
    meters.push(meter.id);
  }
  const mine = await call('GET', '/api/devices', undefined, client);
  assert(mine.length === 1 && mine[0].id === meters[0], 'A client must see only assigned devices');
  await call('GET', '/api/devices/' + meters[0], undefined, client);
  await call('GET', '/api/devices/' + meters[1], undefined, client, 403);
  await call('GET', '/api/devices/by-user/' + accounts[1], undefined, client, 403);
  for (const maxConsumption of [-1, 0, 0.0001, 1000000000]) await call('POST', '/api/devices', {name: 'Invalid', maxConsumption}, admin, 400);
  await call('PUT', `/api/devices/${meters[0]}/assignment/999999999`, undefined, admin, 404);
  await call('PUT', `/api/devices/${meters[0]}`, {name: 'Updated meter', maxConsumption: 8.25, address: 'Updated', userId: accounts[0]}, admin);
  await call('DELETE', `/api/devices/${meters[0]}/assignment`, undefined, admin);
  assert((await call('GET', '/api/devices', undefined, client)).length === 0, 'Unassigned device must disappear from client list');
  await call('PUT', `/api/devices/${meters[0]}/assignment/${accounts[0]}`, undefined, admin);
  await call('GET', '/api/devices', undefined, admin.slice(0, -8) + 'xxxxxxxx', 401);
  if (config.JWT_SECRET) {
    const claims = JSON.parse(Buffer.from(admin.split('.')[1], 'base64url').toString());
    const encoded = Buffer.from(JSON.stringify({...claims, exp: Math.floor(Date.now() / 1000) - 120, iat: Math.floor(Date.now() / 1000) - 3700})).toString('base64url');
    const unsigned = admin.split('.')[0] + '.' + encoded;
    const signature = createHmac('sha256', config.JWT_SECRET).update(unsigned).digest('base64url');
    await call('GET', '/api/devices', undefined, unsigned + '.' + signature, 401);
  }
  await call('GET', '/user-service/api/users', undefined, undefined, 403);
  await call('GET', '/device-service/api/devices', undefined, undefined, 403);
  await call('POST', '/auth-service/internal/users', {}, undefined, 403);
  for (const service of ['user-service', 'device-service', 'auth-service']) {
    const spec = await call('GET', `/${service}/api-docs`);
    assert(spec.openapi && Object.keys(spec.paths).length > 0, 'Swagger paths are missing for ' + service);
    const swaggerConfig = await call('GET', `/${service}/api-docs/swagger-config`);
    assert(swaggerConfig.url === `/${service}/api-docs`, 'Swagger must load the correct proxied specification');
  }
  await call('PUT', `/api/users/${accounts[0]}`, {username: prefix + 'renamed', password: 'Changed123!', role: 'CLIENT', name: 'Renamed', address: 'Updated'}, admin);
  await call('POST', '/api/auth/login', {username: prefix + 'a', password: 'Client123!'}, undefined, 401);
  await call('POST', '/api/auth/login', {username: prefix + 'renamed', password: 'Changed123!'});
  await call('PUT', `/api/users/${accounts[0]}`, {username: prefix + 'renamed', role: 'ADMIN', name: 'Renamed'}, admin);
  await call('GET', '/api/users', undefined, client);
  await call('PUT', `/api/users/${accounts[0]}`, {username: prefix + 'renamed', role: 'CLIENT', name: 'Renamed'}, admin);
  await call('GET', '/api/users', undefined, client, 403);
  await call('DELETE', `/api/users/${accounts[0]}`, undefined, admin, 204);
  accounts.shift();
  assert((await call('GET', `/api/devices/${meters[0]}`, undefined, admin)).userId === null, 'Deleting a user must unassign devices');
  await call('GET', '/api/devices', undefined, client, 401);
  await call('POST', '/api/auth/login', {username: prefix + 'renamed', password: 'Changed123!'}, undefined, 401);
  const me = await call('GET', '/api/auth/me', undefined, admin);
  await call('DELETE', '/api/users/' + me.id, undefined, admin, 409);
  console.log(`PASS: ${checks} HTTP checks, JWT validation, roles, client isolation, CRUD, assignments and Swagger.`);
} finally {
  for (const id of meters) await call('DELETE', '/api/devices/' + id, undefined, admin, 204);
  for (const id of accounts) await call('DELETE', '/api/users/' + id, undefined, admin, 204);
}
