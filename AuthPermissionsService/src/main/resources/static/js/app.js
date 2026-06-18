// ===== Token & Session =====

function getToken() {
  return sessionStorage.getItem('inv_jwt');
}

function getUser() {
  const raw = sessionStorage.getItem('inv_user');
  return raw ? JSON.parse(raw) : null;
}

async function logout() {
  const token = getToken();
  if (token) {
    // Best-effort server-side session end (clears lastSeenAt so the user
    // can immediately re-login elsewhere). Failures are ignored — client-side
    // logout still proceeds.
    try {
      await fetch('/auth/logout', {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + token }
      });
    } catch {}
  }
  sessionStorage.removeItem('inv_jwt');
  sessionStorage.removeItem('inv_user');
  window.location.href = '/login.html';
}

// ===== API Fetch =====

async function apiFetch(url, opts = {}) {
  const token = getToken();
  const res = await fetch(url, {
    ...opts,
    headers: {
      ...opts.headers,
      'Authorization': 'Bearer ' + token
    }
  });
  if (res.status === 401) {
    sessionStorage.clear();
    window.location.href = '/login.html';
    return null;
  }
  return res;
}

// ===== Auth Guard =====

function requireAuth() {
  const token = getToken();
  if (!token) {
    window.location.href = '/login.html';
    return false;
  }
  return true;
}

// ===== Identity from JWT =====
// The JWT carries everything the client needs to know about itself: roles,
// perms, manager flag. No round-trip needed. Decodes the middle segment
// (base64-url JSON) — signature isn't verified client-side, that's the
// server's job.

function decodeToken() {
  const token = getToken();
  if (!token) return {};
  try {
    return JSON.parse(atob(token.split('.')[1]));
  } catch {
    return {};
  }
}

function isManager() { return !!decodeToken().manager; }
function myRoles()   { return decodeToken().roles || []; }
function myPerms()   { return decodeToken().perms || []; }

// ===== Presence Heartbeat =====
// Pings /auth/me every 1s while a token exists, so the user's lastSeenAt
// stays fresh and they show as "online" in the users table.
// Paired with the 3s "online" threshold on auth-service — see AuthService.java.
// Idempotent — safe to call from multiple places.

let __heartbeatStarted = false;

function startHeartbeat(intervalMs = 1000) {
  if (__heartbeatStarted) return;
  __heartbeatStarted = true;
  const tick = async () => {
    try { await apiFetch('/auth/me'); } catch {}
  };
  tick();
  setInterval(tick, intervalMs);
}

// ===== Date Formatting =====
// "Today HH:MM" if today, "Yesterday HH:MM" if yesterday, locale date otherwise.

function formatLastSeen(ts) {
  if (!ts) return 'Never';

  const date = new Date(ts);
  const now = new Date();

  const dateOnly = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);

  const time = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  if (dateOnly.getTime() === today.getTime()) return `Today ${time}`;
  if (dateOnly.getTime() === yesterday.getTime()) return `Yesterday ${time}`;
  return date.toLocaleDateString();
}

// ===== Themed Danger Confirm =====
// Replaces window.confirm() with a themed modal. Reusable across pages.
//
//   confirmDanger({
//     title: 'Delete user?',
//     body: 'This cannot be undone.',
//     confirmLabel: 'DELETE',
//     onConfirm: async () => { ... }
//   });

function confirmDanger({ title, body, confirmLabel = 'CONFIRM', onConfirm }) {
  let modal = document.getElementById('__danger-modal');
  if (!modal) {
    modal = document.createElement('div');
    modal.id = '__danger-modal';
    modal.className = 'modal-back';
    modal.innerHTML = `
      <div class="modal modal-danger" onclick="event.stopPropagation()">
        <button class="modal-x" id="__danger-x">×</button>
        <div class="modal-tag">Confirm</div>
        <div class="modal-title" id="__danger-title"></div>
        <div class="modal-body" id="__danger-body"></div>
        <div class="modal-actions">
          <button class="btn-ghost" id="__danger-cancel">CANCEL</button>
          <button class="btn-danger" id="__danger-confirm"></button>
        </div>
      </div>
    `;
    document.body.appendChild(modal);
    modal.addEventListener('click', e => {
      if (e.target.id === '__danger-modal') closeDanger();
    });
    document.getElementById('__danger-x').addEventListener('click', closeDanger);
    document.getElementById('__danger-cancel').addEventListener('click', closeDanger);
    document.addEventListener('keydown', e => {
      if (e.key === 'Escape' && modal.classList.contains('show')) closeDanger();
    });
  }

  document.getElementById('__danger-title').textContent = title;
  document.getElementById('__danger-body').textContent = body;

  // Replace the confirm button (cloneNode wipes prior listeners cleanly).
  const oldBtn = document.getElementById('__danger-confirm');
  const newBtn = oldBtn.cloneNode(false);
  newBtn.id = '__danger-confirm';
  newBtn.className = 'btn-danger';
  newBtn.textContent = confirmLabel;
  newBtn.addEventListener('click', () => {
    closeDanger();
    if (typeof onConfirm === 'function') onConfirm();
  });
  oldBtn.parentNode.replaceChild(newBtn, oldBtn);

  modal.classList.add('show');
}

function closeDanger() {
  const modal = document.getElementById('__danger-modal');
  if (modal) modal.classList.remove('show');
}

// ===== Themed Alert (replaces window.alert) =====
// Single-button informational modal. Same look as confirmDanger but
// no destructive action — just CLOSE.
//
//   alertModal({ title: 'Failed to load users' });
//   alertModal({ title: 'Sign-in failed', body: 'Wrong email or password' });

function alertModal({ title, body, tag = 'Error' }) {
  let modal = document.getElementById('__alert-modal');
  if (!modal) {
    modal = document.createElement('div');
    modal.id = '__alert-modal';
    modal.className = 'modal-back';
    modal.innerHTML = `
      <div class="modal modal-danger" onclick="event.stopPropagation()">
        <button class="modal-x" id="__alert-x">×</button>
        <div class="modal-tag" id="__alert-tag"></div>
        <div class="modal-title" id="__alert-title"></div>
        <div class="modal-body" id="__alert-body"></div>
        <div class="modal-actions">
          <button class="btn-ghost" id="__alert-close">CLOSE</button>
        </div>
      </div>
    `;
    document.body.appendChild(modal);
    modal.addEventListener('click', e => {
      if (e.target.id === '__alert-modal') closeAlert();
    });
    document.getElementById('__alert-x').addEventListener('click', closeAlert);
    document.getElementById('__alert-close').addEventListener('click', closeAlert);
    document.addEventListener('keydown', e => {
      if (e.key === 'Escape' && modal.classList.contains('show')) closeAlert();
    });
  }
  document.getElementById('__alert-tag').textContent = tag;
  document.getElementById('__alert-title').textContent = title;
  const bodyEl = document.getElementById('__alert-body');
  bodyEl.textContent = body || '';
  bodyEl.style.display = body ? '' : 'none';
  modal.classList.add('show');
}

function closeAlert() {
  const modal = document.getElementById('__alert-modal');
  if (modal) modal.classList.remove('show');
}

// ===== App Shell =====
// Renders a consistent header on every inner service page.
// Call once at the top of each page's <script> block.
//
//   renderAppShell({
//     service: 'Auth Service',
//     page: 'Users',
//     tabs: [
//       { href: '/ui/auth-service/index.html', label: 'Overview' },
//       { href: '/ui/auth-service/users.html', label: 'Users' }
//     ]
//   });

function renderAppShell({ service, page, tabs = [] }) {
  if (!requireAuth()) return;
  startHeartbeat();

  const user = getUser();
  const email = user ? user.email : '';

  const tabHtml = tabs.map(t => {
    const active = t.label === page ? ' class="active"' : '';
    return `<a href="${t.href}"${active}>${t.label}</a>`;
  }).join('');

  const subNav = tabs.length ? `<div class="sub-nav">${tabHtml}</div>` : '';

  document.body.insertAdjacentHTML('afterbegin', `
    <header class="app-shell">
      <div class="shell-top">
        <a href="/menu.html" class="back-link">MENU</a>
        <div class="crumb">${service}<span class="crumb-sep">/</span><span class="crumb-page">${page}</span></div>
        <div class="shell-spacer"></div>
        <div class="user-text">Signed in as <b>${email}</b></div>
        <button class="logout" onclick="logout()">LOGOUT</button>
      </div>
      ${subNav}
    </header>
  `);
}
