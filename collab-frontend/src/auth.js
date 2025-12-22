const KEY = "collab_auth_v1";

export function saveAuth(auth) {
  localStorage.setItem(KEY, JSON.stringify(auth));
}

export function loadAuth() {
  try { return JSON.parse(localStorage.getItem(KEY)); }
  catch { return null; }
}

export function clearAuth() {
  localStorage.removeItem(KEY);
}
