import { Router } from "express";
import { requireAdmin } from "../auth.js";
import * as db from "../db.js";
import { generatePage, removePage, regenerateAll } from "../staticGen.js";

const router = Router();
router.use(requireAdmin);

function validateName(name) {
  if (typeof name !== "string") return null;
  const trimmed = name.trim();
  if (trimmed.length < 1 || trimmed.length > 120) return null;
  return trimmed;
}

router.get("/", (req, res) => {
  res.json(db.getAll());
});

router.post("/", async (req, res) => {
  const { name: rawName, slug: rawSlug, links: rawLinks, theme } = req.body ?? {};

  const name = validateName(rawName);
  if (!name) {
    return res.status(400).json({ error: "Le nom de l'établissement est requis (max 120 caractères)." });
  }

  const slug = typeof rawSlug === "string" ? rawSlug.trim().toLowerCase() : "";
  if (!db.isValidSlug(slug)) {
    return res.status(400).json({
      error: "Le slug doit contenir uniquement des lettres minuscules, chiffres et tirets.",
    });
  }
  if (db.isSlugTaken(slug)) {
    return res.status(409).json({ error: `L'URL "/r/${slug}" est déjà utilisée.` });
  }

  const links = db.normalizeLinks(rawLinks);
  if (!links) {
    return res.status(400).json({
      error: "Chaque lien doit avoir un libellé et une URL valide (entre 1 et 7 liens).",
    });
  }

  const record = db.create({ name, slug, links, theme });
  await generatePage(record);
  res.status(201).json(record);
});

router.put("/:id", async (req, res) => {
  const existing = db.getById(req.params.id);
  if (!existing) {
    return res.status(404).json({ error: "Établissement introuvable." });
  }

  const patch = {};

  if (req.body?.name !== undefined) {
    const name = validateName(req.body.name);
    if (!name) {
      return res.status(400).json({ error: "Le nom de l'établissement est requis (max 120 caractères)." });
    }
    patch.name = name;
  }

  if (req.body?.links !== undefined) {
    const links = db.normalizeLinks(req.body.links);
    if (!links) {
      return res.status(400).json({
        error: "Chaque lien doit avoir un libellé et une URL valide (entre 1 et 7 liens).",
      });
    }
    patch.links = links;
  }

  if (req.body?.theme !== undefined) {
    patch.theme = req.body.theme;
  }

  // Slug is intentionally ignored here — it's permanent once engraved.
  const record = db.update(req.params.id, patch);
  await generatePage(record);
  res.json(record);
});

router.delete("/:id", async (req, res) => {
  const existing = db.getById(req.params.id);
  if (!existing) {
    return res.status(404).json({ error: "Établissement introuvable." });
  }
  db.remove(req.params.id);
  await removePage(existing.slug);
  res.json({ ok: true });
});

// Regenerate all static pages (admin-only) — useful when the render
// template changes, or as a manual "rebuild everything" action.
router.post("/regenerate", async (req, res) => {
  const count = await regenerateAll();
  res.json({ ok: true, count });
});

export default router;
