import { readFileSync } from 'node:fs';

const base = process.env.ENERGY_URL || 'http://localhost:8085';
const config = Object.fromEntries(readFileSync('.env', 'utf8').split('\n').filter(line => line.includes('=') && !line.startsWith('#')).map(line => { const i = line.indexOf('='); return [line.slice(0, i), line.slice(i + 1)]; }));
let token;
async function api(path, method = 'GET', body) {
  const response = await fetch(base + '/api/' + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? {Authorization: 'Bearer ' + token} : {}) },
    ...(body ? {body: JSON.stringify(body)} : {})
  });
  const data = response.status === 204 ? null : await response.json();
  if (!response.ok) throw new Error(JSON.stringify(data));
  return data;
}
token = (await api('auth/login', 'POST', {username: config.ADMIN_USERNAME, password: config.ADMIN_PASSWORD})).access_token;
const existingUsers = await api('users');
const userIds = [];
for (const [username, name] of [['client.demo', 'Ana Demo'], ['client2.demo', 'Mara Demo']]) {
  const user = existingUsers.find(user => user.username === username) || await api('users', 'POST', {username, password: 'Client123!', role: 'CLIENT', name, address: 'Cluj-Napoca'});
  userIds.push(user.id);
}
const existingDevices = await api('devices');
for (const [name, maxConsumption, address, userId] of [
  ['Living room meter', 3.5, 'Living room', userIds[0]],
  ['Kitchen meter', 5.25, 'Kitchen', userIds[0]],
  ['Home office meter', 2.75, 'Home office', userIds[1]],
  ['Bedroom meter', 1.8, 'Bedroom', null]
]) {
  if (!existingDevices.some(device => device.name === name)) await api('devices', 'POST', {name, maxConsumption, address, userId});
}
console.log('Demo accounts and meters are ready. Clients: client.demo and client2.demo. Password: Client123!');
