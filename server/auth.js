import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";

const SESSION_COOKIE = "tably_admin";
const SESSION_TTL_MS = 30 * 24 * 60 * 60 * 1000; // 30 days
const SECRET_FILE = path.join(process.cwd(), ".session-secret");

function getSessionSecret() {
  if (process.env.SESSION_SECRET) return process.env.SESSION_SECRET;

  if (fs.existsSync(SECRET_FILE)) {
    return fs.readFileSync(SECRET_FILE, "utf8").trim();
  }

  const generated = crypto.randomBytes(32).toString("hex");
  fs.writeFileSync(SECRET_FILE, generated, "utf8");
  return generated;
}

const SECRET = getSessionSecret();

function sign(payload) {
  const body = Buffer.from(JSON.stringify(payload)).toString("base64url");
  const signature = crypto.createHmac("sha256", SECRET).update(body).digest("base64url");
  return `${body}.${signature}`;
}

function verify(token) {
  if (typeof token !== "string" || !token.includes(".")) return null;
  const [body, signature] = token.split(".");
  const expected = crypto.createHmac("sha256", SECRET).update(body).digest("base64url");

  const sigBuf = Buffer.from(signature);
  const expectedBuf = Buffer.from(expected);
  if (sigBuf.length !== expectedBuf.length || !crypto.timingSafeEqual(sigBuf, expectedBuf)) {
    return null;
  }

  try {
    const payload = JSON.parse(Buffer.from(body, "base64url").toString("utf8"));
    if (typeof payload.exp !== "number" || Date.now() > payload.exp) return null;
    return payload;
  } catch {
    return null;
  }
}

export function isAdminPasswordConfigured() {
  return Boolean(process.env.ADMIN_PASSWORD) && process.env.ADMIN_PASSWORD !== "change-me";
}

export function checkPassword(candidate) {
  const expected = process.env.ADMIN_PASSWORD || "";
  if (!expected || typeof candidate !== "string") return false;

  const candidateBuf = Buffer.from(candidate);
  const expectedBuf = Buffer.from(expected);
  // Pad to equal length before comparing so the check itself doesn't leak
  // the real password's length via timing.
  const maxLen = Math.max(candidateBuf.length, expectedBuf.length, 1);
  const a = Buffer.alloc(maxLen);
  const b = Buffer.alloc(maxLen);
  candidateBuf.copy(a);
  expectedBuf.copy(b);

  return crypto.timingSafeEqual(a, b) && candidateBuf.length === expectedBuf.length;
}

export function issueSession(res) {
  const token = sign({ exp: Date.now() + SESSION_TTL_MS });
  res.cookie(SESSION_COOKIE, token, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    maxAge: SESSION_TTL_MS,
    path: "/",
  });
}

export function clearSession(res) {
  res.clearCookie(SESSION_COOKIE, { path: "/" });
}

export function isAuthenticated(req) {
  return verify(req.cookies?.[SESSION_COOKIE]) !== null;
}

export function requireAdmin(req, res, next) {
  if (!isAuthenticated(req)) {
    return res.status(401).json({ error: "Non authentifié." });
  }
  return next();
}
