import "dotenv/config";
import fs from "node:fs";
import path from "node:path";
import express from "express";
import cookieParser from "cookie-parser";

import adminRoutes from "./routes/admin.js";
import restaurantRoutes from "./routes/restaurants.js";
import { isAdminPasswordConfigured } from "./auth.js";
import * as db from "./db.js";
import { renderClientPage, renderNotFoundPage } from "./render/clientPage.js";

const PORT = process.env.PORT || 3001;
const DIST_DIR = path.join(process.cwd(), "dist");
const HAS_BUILD = fs.existsSync(path.join(DIST_DIR, "index.html"));

const app = express();
app.disable("x-powered-by");

app.use(express.json());
app.use(cookieParser());

app.use((req, res, next) => {
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("X-Frame-Options", "SAMEORIGIN");
  res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
  next();
});

app.use("/api/admin", adminRoutes);
app.use("/api/restaurants", restaurantRoutes);

// Public, server-rendered client link pages — no client JS required, so
// they stay fast and reliable for a QR code that can never be reprinted.
app.get("/r/:slug", (req, res) => {
  const restaurant = db.getBySlug(req.params.slug.toLowerCase());
  if (!restaurant) {
    return res.status(404).type("html").send(renderNotFoundPage());
  }
  res.type("html").send(renderClientPage(restaurant));
});

if (HAS_BUILD) {
  app.use(express.static(DIST_DIR));
  // SPA fallback for client-routed paths (/, /admin, ...). Registered last,
  // after /api and /r, and with no path pattern so it can't collide with
  // Express 5's stricter wildcard route syntax.
  app.use((req, res) => {
    res.sendFile(path.join(DIST_DIR, "index.html"));
  });
} else {
  app.get("/", (req, res) => {
    res.type("text").send(
      "Build not found. Run `npm run build` first, or use `npm run dev` for local development (Vite serves the frontend on its own port).",
    );
  });
}

app.listen(PORT, () => {
  console.log(`\n  QlinkR server running → http://localhost:${PORT}`);
  console.log(`  Mode: ${HAS_BUILD ? "production (serving dist/)" : "API only (run the Vite dev server separately)"}`);
  if (!isAdminPasswordConfigured()) {
    console.log(
      "\n  ⚠ ADMIN_PASSWORD is not set (or still the default). /admin is effectively locked out.\n  Copy .env.example to .env and set a real password.\n",
    );
  }
});
