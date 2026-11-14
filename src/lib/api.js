async function request(path, options = {}) {
  const res = await fetch(path, {
    ...options,
    headers: { "Content-Type": "application/json", ...options.headers },
  });

  let data = null;
  try {
    data = await res.json();
  } catch {
    // empty body (e.g. some 204s) — fine to ignore
  }

  if (!res.ok) {
    throw new Error(data?.error || `Erreur ${res.status}`);
  }
  return data;
}

export const api = {
  me: () => request("/api/admin/me"),
  login: (password) => request("/api/admin/login", { method: "POST", body: JSON.stringify({ password }) }),
  logout: () => request("/api/admin/logout", { method: "POST" }),

  list: () => request("/api/restaurants"),
  create: (payload) => request("/api/restaurants", { method: "POST", body: JSON.stringify(payload) }),
  update: (id, payload) =>
    request(`/api/restaurants/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
  remove: (id) => request(`/api/restaurants/${id}`, { method: "DELETE" }),
  regenerateAll: () => request("/api/restaurants/regenerate", { method: "POST" }),
};
