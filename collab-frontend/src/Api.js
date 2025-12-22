export const API_BASE = "http://localhost:8080";

/* =========================
   Low-level request helper
========================= */
async function request(path, { method = "GET", body, token } = {}) {
  let res;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      method,
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch (e) {
    return { ok: false, message: "Network error: cannot reach server" };
  }

  let payload = null;
  try {
    payload = await res.json();
  } catch {
    payload = null;
  }

  if (!res.ok) {
    return {
      ok: false,
      message: payload?.message || payload?.error || `Request failed (HTTP ${res.status})`,
    };
  }

  return { ok: true, data: payload, message: payload?.message || "Success" };
}

/* =========================
   User Service Functions
========================= */
export async function register({ username, email, password }) {
  return request("/users/register", {
    method: "POST",
    body: { username, email, password },
  });
}

export async function login({ username, password }) {
  return request("/users/login", {
    method: "POST",
    body: { username, password },
  });
}

export async function me(token) {
  return request("/users/me", { token });
}

/* =========================
   Doc Service Functions
========================= */
export async function getDocuments(token) {
  return request("/docs", { token });
}

export async function createDocument(token, { title, content }) {
  console.log("API: Calling createDocument with title:", title); // Debug log
  return request("/docs", {
    method: "POST",
    token: token,
    body: { title, content },
  });
}

export async function shareDocument(token, docId, collaboratorUsername) {
  return request(`/docs/${docId}/share`, {
    method: "POST",
    token,
    body: { collaboratorUsername },
  });
}

export async function changePassword({
  token,
  currentPassword,
  newPassword,
}) {
  return request("/users/me/password", {
    method: "PUT",
    token, // Send the JWT token
    body: { currentPassword, newPassword },
  });
}

/* =========================
   Version Service Functions
========================= */
export async function getDocVersionHistory(token, docId) {
  return request(`/docs/${docId}/versions`, { token });
}


export async function deleteDocument(token, docId) {
  return request(`/docs/${docId}`, {
    method: "DELETE",
    token,
  });
}

export async function revokeDocumentAccess(token, docId, collaboratorUsername) {
  return request(`/docs/${docId}/share/${collaboratorUsername}`, {
    method: "DELETE",
    token,
  });
}
