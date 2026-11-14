import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";

const DATA_DIR = process.env.DATA_DIR || "./server/data";
const STORE_PATH = path.join(process.cwd(), DATA_DIR, "restaurants.json");

const MAX_LINKS = 7;
const MIN_LINKS = 1;

// Keep in sync with THEME_OPTIONS in src/lib/themes.js and THEMES in
// server/render/clientPage.js.
export const THEME_KEYS = ["simple", "restaurant", "cafe", "tearoom", "gym", "beauty"];
export const DEFAULT_THEME = "simple";

export function normalizeTheme(theme) {
  return THEME_KEYS.includes(theme) ? theme : DEFAULT_THEME;
}

function ensureStore() {
  const dir = path.dirname(STORE_PATH);
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
  if (!fs.existsSync(STORE_PATH)) fs.writeFileSync(STORE_PATH, "[]\n", "utf8");
}

function readAll() {
  ensureStore();
  try {
    const raw = fs.readFileSync(STORE_PATH, "utf8");
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch (err) {
    console.error(`Failed to read/parse ${STORE_PATH}:`, err.message);
    return [];
  }
}

function writeAll(list) {
  ensureStore();
  fs.writeFileSync(STORE_PATH, JSON.stringify(list, null, 2) + "\n", "utf8");
}

export function slugify(input) {
  return input
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 80);
}

export function isValidSlug(slug) {
  return typeof slug === "string" && /^[a-z0-9]+(-[a-z0-9]+)*$/.test(slug) && slug.length <= 80;
}

export function isValidUrl(value) {
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

export function normalizeLinks(links) {
  if (!Array.isArray(links)) return null;
  if (links.length < MIN_LINKS || links.length > MAX_LINKS) return null;

  const cleaned = [];
  for (const link of links) {
    const label = typeof link?.label === "string" ? link.label.trim().slice(0, 40) : "";
    let url = typeof link?.url === "string" ? link.url.trim() : "";
    const icon = typeof link?.icon === "string" ? link.icon : "generic";
    if (!label || !url) return null;
    if (!/^https?:\/\//i.test(url)) url = `https://${url}`;
    if (!isValidUrl(url)) return null;
    cleaned.push({ label, url, icon });
  }
  return cleaned;
}

export function getAll() {
  return readAll().sort((a, b) => b.createdAt.localeCompare(a.createdAt));
}

export function getBySlug(slug) {
  return readAll().find((r) => r.slug === slug) ?? null;
}

export function getById(id) {
  return readAll().find((r) => r.id === id) ?? null;
}

export function isSlugTaken(slug, excludeId = null) {
  return readAll().some((r) => r.slug === slug && r.id !== excludeId);
}

export function create({ name, slug, links, theme }) {
  const list = readAll();
  const now = new Date().toISOString();
  const record = {
    id: crypto.randomUUID(),
    name,
    slug,
    links,
    theme: normalizeTheme(theme),
    createdAt: now,
    updatedAt: now,
  };
  list.push(record);
  writeAll(list);
  return record;
}

// Slug is intentionally not editable here — it's engraved permanently once
// a coaster is made, so it's locked after creation at the route layer.
// Theme has no effect on the QR code itself, so it's always safe to change.
export function update(id, { name, links, theme }) {
  const list = readAll();
  const index = list.findIndex((r) => r.id === id);
  if (index === -1) return null;

  list[index] = {
    ...list[index],
    ...(name !== undefined ? { name } : {}),
    ...(links !== undefined ? { links } : {}),
    ...(theme !== undefined ? { theme: normalizeTheme(theme) } : {}),
    updatedAt: new Date().toISOString(),
  };
  writeAll(list);
  return list[index];
}

export function remove(id) {
  const list = readAll();
  const next = list.filter((r) => r.id !== id);
  if (next.length === list.length) return false;
  writeAll(next);
  return true;
}
