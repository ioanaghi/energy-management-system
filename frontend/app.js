const $ = (selector) => document.querySelector(selector);
let token = sessionStorage.getItem('energy-token');
let currentUser = null;
let users = [];
let devices = [];
let tab = 'devices';
let editingId = null;
let editingProfile = false;
const registerPage = window.location.pathname === '/register' || window.location.pathname === '/register/';
const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (char) => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));

function notify(message, error = false) {
  $('#notice').textContent = message;
  $('#notice').className = error ? 'error' : '';
  $('#notice').hidden = false;
}

async function api(path, method = 'GET', body) {
  const response = await fetch('/api/' + path, {
    method,
    headers: {'Content-Type': 'application/json', ...(token ? {Authorization: 'Bearer ' + token} : {})},
    ...(body ? {body: JSON.stringify(body)} : {})
  });
  const data = response.status === 204 ? null : await response.json();
  if (!response.ok) {
    if (response.status === 401 && currentUser) logout();
    let message = data?.detail || data?.message || 'The request could not be completed.';
    while (message && typeof message === 'object' && !Array.isArray(message)) message = message.detail || message.message || JSON.stringify(message);
    if (Array.isArray(message)) message = message.map((item) => item.msg).join('; ');
    throw new Error(message);
  }
  return data;
}

function logout() {
  token = null;
  currentUser = null;
  users = [];
  devices = [];
  sessionStorage.removeItem('energy-token');
  document.body.classList.add('auth-page');
  $('#dashboard').hidden = true;
  $('#login-view').hidden = false;
  $('#account').replaceChildren();
  $('#editor').close();
}

function showAuthPage() {
  $('#login-view').hidden = registerPage;
  $('#register-view').hidden = !registerPage;
}

async function submitAuth(event, register) {
  event.preventDefault();
  const button = event.submitter;
  button.disabled = true;
  try {
    const data = Object.fromEntries(new FormData(event.target));
    if (register) {
      await api('auth/register', 'POST', data);
      window.location.assign('/?created=1');
    } else {
      const result = await api('auth/login', 'POST', data);
      token = result.access_token;
      sessionStorage.setItem('energy-token', token);
      currentUser = result.user;
      event.target.reset();
      $('#notice').hidden = true;
      await loadDashboard();
    }
  } catch (error) { notify(error.message, true); }
  finally { button.disabled = false; }
}

$('#login-form').onsubmit = (event) => submitAuth(event, false);
$('#register-form').onsubmit = (event) => submitAuth(event, true);

async function loadDashboard() {
  currentUser = await api('auth/me');
  devices = await api('devices');
  users = currentUser.role === 'ADMIN' ? await api('users') : [];
  document.body.classList.remove('auth-page');
  $('#login-view').hidden = true;
  $('#register-view').hidden = true;
  $('#dashboard').hidden = false;
  $('#admin-tabs').hidden = currentUser.role !== 'ADMIN';
  $('#account').innerHTML = `<span>${escapeHtml(currentUser.name || currentUser.username)}</span><button id="my-profile" class="secondary">My profile</button><button id="logout" class="secondary">Sign out</button>`;
  $('#logout').onclick = logout;
  $('#my-profile').onclick = openProfile;
  $('#dashboard-title').textContent = currentUser.role === 'ADMIN' ? 'Overview' : 'My devices';
  $('#role-label').textContent = currentUser.role === 'ADMIN' ? 'ADMIN' : 'CLIENT';
  const assigned = devices.filter((device) => device.userId !== null).length;
  $('#stats').innerHTML = currentUser.role === 'ADMIN' ?
    `<div class="stat"><span>Devices</span><strong>${devices.length}</strong></div><div class="stat"><span>Users</span><strong>${users.length}</strong></div><div class="stat"><span>Unassigned</span><strong>${devices.length - assigned}</strong></div>` :
    `<div class="stat"><span>Devices assigned to you</span><strong>${devices.length}</strong></div>`;
  renderList();
}

function renderList() {
  const admin = currentUser.role === 'ADMIN';
  $('#add-item').hidden = !admin;
  $('#add-item').textContent = tab === 'users' ? 'Add user' : 'Add device';
  $('#list-title').textContent = admin && tab === 'users' ? 'User accounts' : 'Devices';
  $('#list-caption').textContent = admin ? (tab === 'users' ? 'Manage account access.' : 'Manage meters and assignments.') : 'Meters assigned to your account.';
  if (!admin) {
    $('#list').innerHTML = devices.length ? `<div class="device-grid">${devices.map((device) => `<article class="device-card"><div class="device-card-top"><span class="device-dot"></span><span>DEVICE ${device.id}</span></div><h3>${escapeHtml(device.name)}</h3><p>${escapeHtml(device.address || 'No address')}</p><div class="consumption"><strong>${escapeHtml(device.maxConsumption)}</strong><span>kWh max / hour</span></div></article>`).join('')}</div>` : '<div class="empty"><strong>No devices yet</strong><span>Your assigned devices will appear here.</span></div>';
    return;
  }
  const rows = tab === 'users' ? users : devices;
  if (!rows.length) { $('#list').innerHTML = `<div class="empty"><strong>No ${tab === 'users' ? 'users' : 'devices'} yet</strong><span>Use the button above to add one.</span></div>`; return; }
  const headings = tab === 'users' ? ['ID', 'Username', 'Name', 'Role', 'Address', 'Actions'] : ['ID', 'Device', 'Max. consumption', 'Assigned user', 'Address', 'Actions'];
  $('#list').innerHTML = `<div class="table-scroll"><table><thead><tr>${headings.map((heading) => `<th>${heading}</th>`).join('')}</tr></thead><tbody>${rows.map((row) => {
    const fields = tab === 'users' ? [row.id, row.username, row.name, row.role, row.address] : [row.id, row.name, row.maxConsumption + ' kWh', users.find((user) => user.id === row.userId)?.username || 'Unassigned', row.address];
    return `<tr>${fields.map((field) => `<td>${escapeHtml(field || '-')}</td>`).join('')}<td><button class="text-button" data-edit="${row.id}">Edit</button><button class="text-button danger" data-delete="${row.id}">Delete</button></td></tr>`;
  }).join('')}</tbody></table></div>`;
  document.querySelectorAll('[data-edit]').forEach((button) => button.onclick = () => openEditor(Number(button.dataset.edit)));
  document.querySelectorAll('[data-delete]').forEach((button) => button.onclick = async () => {
    const id = Number(button.dataset.delete);
    if (!confirm(`Delete this ${tab === 'users' ? 'user account' : 'device'}?${tab === 'users' ? ' Assigned devices will be kept and unassigned.' : ''}`)) return;
    button.disabled = true;
    try { await api(`${tab}/${id}`, 'DELETE'); await loadDashboard(); notify('Record deleted.'); }
    catch (error) { notify(error.message, true); button.disabled = false; }
  });
}

function input(label, name, value = '', type = 'text', attributes = '') {
  return `<label>${label}<input name="${name}" type="${type}" value="${escapeHtml(value)}" ${attributes}></label>`;
}

function openEditor(id = null) {
  editingProfile = false;
  editingId = id;
  $('#editor-error').hidden = true;
  const row = (tab === 'users' ? users : devices).find((item) => item.id === id) || {};
  $('#edit-title').textContent = `${id ? 'Edit' : 'Add'} ${tab === 'users' ? 'user account' : 'device'}`;
  $('#save-item').textContent = id ? 'Save changes' : `Create ${tab === 'users' ? 'user' : 'device'}`;
  $('#edit-fields').innerHTML = tab === 'users' ?
    input('Username', 'username', row.username, 'text', 'required maxlength="100" pattern="\\S+" autocomplete="off"') +
    input(id ? 'New password (leave blank to keep)' : 'Password', 'password', '', 'password', `${id ? '' : 'required'} minlength="8" maxlength="128" autocomplete="new-password"`) +
    input('Full name', 'name', row.name, 'text', 'maxlength="150"') +
    input('Address', 'address', row.address, 'text', 'maxlength="250"') +
    `<label>Role<select name="role"><option value="CLIENT">Client</option><option value="ADMIN" ${row.role === 'ADMIN' ? 'selected' : ''}>Administrator</option></select></label>` :
    input('Device name', 'name', row.name, 'text', 'required maxlength="150"') +
    input('Maximum hourly consumption (kWh)', 'maxConsumption', row.maxConsumption, 'number', 'required min="0.001" max="999999999.999" step="0.001"') +
    input('Installation address', 'address', row.address, 'text', 'maxlength="250"') +
    `<label>Assigned user<select name="userId"><option value="">Unassigned</option>${users.map((user) => `<option value="${user.id}" ${row.userId === user.id ? 'selected' : ''}>${escapeHtml(user.username)} (${escapeHtml(user.role)})</option>`).join('')}</select></label>`;
  $('#editor').showModal();
}

function openProfile() {
  editingProfile = true;
  $('#editor-error').hidden = true;
  $('#edit-title').textContent = 'My profile';
  $('#save-item').textContent = 'Save changes';
  $('#edit-fields').innerHTML =
    input('Username', 'username', currentUser.username, 'text', 'disabled') +
    input('Full name', 'name', currentUser.name, 'text', 'maxlength="150" autocomplete="name"') +
    input('Address', 'address', currentUser.address, 'text', 'maxlength="250" autocomplete="street-address"') +
    input('Role', 'role', currentUser.role, 'text', 'disabled');
  $('#editor').showModal();
}

$('#edit-form').onsubmit = async (event) => {
  event.preventDefault();
  const submit = event.submitter;
  submit.disabled = true;
  try {
    const body = Object.fromEntries(new FormData(event.target));
    if (!editingProfile && tab === 'devices') {
      body.maxConsumption = Number(body.maxConsumption);
      body.userId = body.userId ? Number(body.userId) : null;
    } else if (!body.password) delete body.password;
    await api(editingProfile ? 'auth/me' : tab + (editingId ? '/' + editingId : ''), editingProfile || editingId ? 'PUT' : 'POST', body);
    $('#editor').close();
    await loadDashboard();
    notify('Changes saved.');
  } catch (error) { $('#editor-error').textContent = error.message; $('#editor-error').hidden = false; }
  finally { submit.disabled = false; }
};
$('#add-item').onclick = () => openEditor();
$('#close-editor').onclick = $('#cancel-editor').onclick = () => $('#editor').close();
$('#refresh').onclick = async () => { try { await loadDashboard(); notify('Overview updated.'); } catch (error) { notify(error.message, true); } };
document.querySelectorAll('[data-tab]').forEach((button) => button.onclick = () => {
  tab = button.dataset.tab;
  document.querySelectorAll('[data-tab]').forEach((item) => item.classList.toggle('active', item === button));
  renderList();
});
if (token) loadDashboard().catch((error) => { logout(); notify(error.message, true); });
else {
  showAuthPage();
  if (new URLSearchParams(window.location.search).has('created')) notify('Account created. Sign in to continue.');
}
