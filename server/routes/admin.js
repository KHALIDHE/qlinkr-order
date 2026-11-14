import { Router } from "express";
import rateLimit from "express-rate-limit";
import { checkPassword, isAdminPasswordConfigured, issueSession, clearSession, isAuthenticated } from "../auth.js";

const router = Router();

const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 10,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: "Trop de tentatives. Réessayez dans quelques minutes." },
});

router.post("/login", loginLimiter, (req, res) => {
  if (!isAdminPasswordConfigured()) {
    return res.status(500).json({
      error: "ADMIN_PASSWORD n'est pas configuré sur le serveur. Voir .env.example.",
    });
  }

  const { password } = req.body ?? {};
  if (!checkPassword(password)) {
    return res.status(401).json({ error: "Mot de passe incorrect." });
  }

  issueSession(res);
  return res.json({ ok: true });
});

router.post("/logout", (req, res) => {
  clearSession(res);
  res.json({ ok: true });
});

router.get("/me", (req, res) => {
  res.json({ authenticated: isAuthenticated(req) });
});

export default router;
