import fs from "node:fs";
import path from "node:path";
import { renderClientPage, renderNotFoundPage } from "./render/clientPage.js";
import * as db from "./db.js";

const OUTPUT_DIR = path.join(process.cwd(), "public");
const PAGES_DIR = path.join(OUTPUT_DIR, "r");

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
}

function pagePath(slug) {
  return path.join(PAGES_DIR, `${slug}.html`);
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

/**
 * Generate (or overwrite) the static HTML file for a single restaurant.
 * Safe to call from a route handler — never throws.
 */
export async function generatePage(restaurant) {
  try {
    ensureDir(PAGES_DIR);
    const html = renderClientPage(restaurant);
    fs.writeFileSync(pagePath(restaurant.slug), html, "utf8");
  } catch (err) {
    console.error(`[staticGen] Failed to generate page for "${restaurant.slug}":`, err.message);
  }
}

/**
 * Remove the static HTML file for a given slug.
 * Safe to call from a route handler — never throws.
 */
export async function removePage(slug) {
  try {
    const target = pagePath(slug);
    if (fs.existsSync(target)) fs.unlinkSync(target);
  } catch (err) {
    console.error(`[staticGen] Failed to remove page for "${slug}":`, err.message);
  }
}

/**
 * Regenerate every restaurant page + 404.html.
 * Returns the number of pages generated.
 */
export async function regenerateAll() {
  // Start fresh
  if (fs.existsSync(PAGES_DIR)) {
    fs.rmSync(PAGES_DIR, { recursive: true, force: true });
  }
  ensureDir(PAGES_DIR);

  const restaurants = db.getAll();

  for (const restaurant of restaurants) {
    const html = renderClientPage(restaurant);
    fs.writeFileSync(pagePath(restaurant.slug), html, "utf8");
  }

  // Root 404 page
  fs.writeFileSync(path.join(OUTPUT_DIR, "404.html"), renderNotFoundPage(), "utf8");

  return restaurants.length;
}
