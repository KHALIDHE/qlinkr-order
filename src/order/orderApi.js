// Small helpers for the ordering pages: REST calls and the live WebSocket (STOMP).
import { Client } from "@stomp/stompjs";

async function request(path, options = {}) {
  const res = await fetch(path, {
    ...options,
    headers: { "Content-Type": "application/json", ...options.headers },
  });
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const error = new Error(data?.detail || `Error ${res.status}`);
    error.status = res.status;
    throw error;
  }
  return data;
}

const bearer = (token) => ({ Authorization: `Bearer ${token}` });

export const orderApi = {
  scan: (code, lang) => request(`/api/public/scan/${code}${lang ? `?lang=${lang}` : ""}`),

  // The key is created once per cart: a double tap sends the same key and gets the same order.
  placeOrder: (code, key, lines) =>
    request(`/api/public/scan/${code}/orders`, {
      method: "POST",
      headers: { "Idempotency-Key": key },
      body: JSON.stringify({ lines }),
    }),

  order: (token) => request(`/api/public/orders/${token}`),

  login: (email, password) =>
    request("/api/auth/login", { method: "POST", body: JSON.stringify({ email, password }) }),
  me: (token) => request("/api/auth/me", { headers: bearer(token) }),
  openOrders: (token) => request("/api/orders", { headers: bearer(token) }),
  setStatus: (token, id, to) =>
    request(`/api/orders/${id}/status`, {
      method: "POST",
      headers: bearer(token),
      body: JSON.stringify({ to }),
    }),
};

// Calls for the owner page: menu, tables with their QR codes, and staff accounts.
const call = (token, path, method = "GET", body) =>
  request(path, { method, headers: bearer(token), body: body ? JSON.stringify(body) : undefined });

// Images and the print sheet need the login token, so we download them and show them from memory.
export const ownerApi = {
  categories: (t) => call(t, "/api/catalog/categories"),
  addCategory: (t, name) => call(t, "/api/catalog/categories", "POST", { names: { fr: name, en: name, ar: name } }),
  items: (t) => call(t, "/api/catalog/items"),
  addItem: (t, categoryId, name, priceCents) =>
    call(t, "/api/catalog/items", "POST", {
      categoryId, priceCents,
      texts: { fr: { name }, en: { name }, ar: { name } },
    }),
  patchItem: (t, id, patch) => call(t, `/api/catalog/items/${id}`, "PATCH", patch),
  deleteItem: (t, id) => call(t, `/api/catalog/items/${id}`, "DELETE"),
  scanPoints: (t) => call(t, "/api/scan-points"),
  addScanPoint: (t, label, kind) => call(t, "/api/scan-points", "POST", { label, kind }),
  staff: (t) => call(t, "/api/business/staff"),
  addStaff: (t, email, password) => call(t, "/api/business/staff", "POST", { email, password }),
  removeStaff: (t, id) => call(t, `/api/business/staff/${id}`, "DELETE"),
  file: async (t, path) => {
    const res = await fetch(path, { headers: bearer(t) });
    if (!res.ok) throw new Error(`Error ${res.status}`);
    return res.blob();
  },
};

// Opens a WebSocket, listens to one topic, calls onMessage for each update. Returns a "stop" function.
export function listen(topic, onMessage, token) {
  const scheme = window.location.protocol === "https:" ? "wss" : "ws";
  const client = new Client({
    brokerURL: `${scheme}://${window.location.host}/ws`,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 3000,
    onConnect: () => client.subscribe(topic, (frame) => onMessage(JSON.parse(frame.body))),
  });
  client.activate();
  return () => client.deactivate();
}

export const money = (cents) => `${(cents / 100).toFixed(2)} MAD`;
