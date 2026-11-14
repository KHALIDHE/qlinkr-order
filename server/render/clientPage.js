import { getIconSvg } from "./icons.js";

// Keep in sync with SITE_NAME in src/config/site.js — this file is Node-only
// server code so it can't import the client config directly.
const SITE_NAME = "QlinkR";

export function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

const FONTS_LINK =
  '<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin><link href="https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght@9..144,600;9..144,700&family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">';

// Faint noise texture, baked as a low-opacity rect inside the SVG itself
// (opacity="0.05") so it composites as a plain extra background layer —
// no blend-mode or stacking-context tricks needed, stays pure CSS/HTML.
const GRAIN_DATA_URI =
  "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.05'/%3E%3C/svg%3E\")";

// Keep these keys in sync with THEME_KEYS in server/db.js and
// THEME_OPTIONS in src/lib/themes.js. Every theme defines the same set of
// CSS custom properties — only the values change, so the rest of the
// stylesheet never needs to know which theme is active.
const THEMES = {
  simple: {
    "--bg": "#fbf6ee",
    "--ink": "#241b13",
    "--ink-soft": "#4a3c2e",
    "--accent": "#a97a45",
    "--accent-2": "#cba75c",
    "--circle-a": "#fffaf0",
    "--circle-b": "#f1e1c6",
    "--circle-c": "#e3cca0",
    "--circle-border": "rgba(107,72,38,.22)",
    "--circle-icon": "#6b4826",
    "--bloom-1": "rgba(203,167,92,.18)",
    "--bloom-2": "rgba(107,72,38,.10)",
    "--bloom-3": "rgba(201,166,102,.14)",
  },
  restaurant: {
    "--bg": "#1c1410",
    "--ink": "#f5ece0",
    "--ink-soft": "#cbb89a",
    "--accent": "#d8b26a",
    "--accent-2": "#e8c988",
    "--circle-a": "#3a2a1c",
    "--circle-b": "#2a1e14",
    "--circle-c": "#1c130c",
    "--circle-border": "rgba(216,178,106,.35)",
    "--circle-icon": "#e8c988",
    "--bloom-1": "rgba(216,178,106,.14)",
    "--bloom-2": "rgba(0,0,0,.35)",
    "--bloom-3": "rgba(216,178,106,.08)",
  },
  cafe: {
    "--bg": "#fbf1e6",
    "--ink": "#3a2418",
    "--ink-soft": "#6b4a34",
    "--accent": "#c9713f",
    "--accent-2": "#e0925f",
    "--circle-a": "#fff3e6",
    "--circle-b": "#f3d9bd",
    "--circle-c": "#e3b98a",
    "--circle-border": "rgba(159,90,45,.25)",
    "--circle-icon": "#8a4a26",
    "--bloom-1": "rgba(201,113,63,.16)",
    "--bloom-2": "rgba(139,69,19,.08)",
    "--bloom-3": "rgba(224,146,95,.14)",
  },
  tearoom: {
    "--bg": "#fdf2f2",
    "--ink": "#3d2b2e",
    "--ink-soft": "#7a5a5e",
    "--accent": "#b98a8f",
    "--accent-2": "#d6a8ac",
    "--circle-a": "#fff8f5",
    "--circle-b": "#f3dfe0",
    "--circle-c": "#e6c3c6",
    "--circle-border": "rgba(154,98,103,.22)",
    "--circle-icon": "#8a5a5e",
    "--bloom-1": "rgba(185,138,143,.14)",
    "--bloom-2": "rgba(150,170,130,.08)",
    "--bloom-3": "rgba(214,168,172,.12)",
  },
  gym: {
    "--bg": "#14161a",
    "--ink": "#f2f3f5",
    "--ink-soft": "#b7bcc4",
    "--accent": "#c8ff4d",
    "--accent-2": "#9fe62f",
    "--circle-a": "#24272c",
    "--circle-b": "#1a1c20",
    "--circle-c": "#101214",
    "--circle-border": "rgba(200,255,77,.35)",
    "--circle-icon": "#c8ff4d",
    "--bloom-1": "rgba(200,255,77,.10)",
    "--bloom-2": "rgba(0,0,0,.4)",
    "--bloom-3": "rgba(200,255,77,.06)",
  },
  beauty: {
    "--bg": "#fdf3f6",
    "--ink": "#3a2530",
    "--ink-soft": "#7a5866",
    "--accent": "#c78fa0",
    "--accent-2": "#e0aebb",
    "--circle-a": "#fff7fa",
    "--circle-b": "#f6dde5",
    "--circle-c": "#ecc3ce",
    "--circle-border": "rgba(180,110,130,.25)",
    "--circle-icon": "#9a5a70",
    "--bloom-1": "rgba(199,143,160,.15)",
    "--bloom-2": "rgba(224,174,187,.10)",
    "--bloom-3": "rgba(199,143,160,.10)",
  },
};

function themeVars(theme) {
  const vars = THEMES[theme] || THEMES.simple;
  return Object.entries(vars)
    .map(([k, v]) => `${k}: ${v};`)
    .join(" ");
}

const BASE_STYLE = `
  * { box-sizing: border-box; }
  body {
    margin: 0; min-height: 100svh; display: flex; flex-direction: column;
    align-items: center; justify-content: center; padding: 48px 24px 32px;
    background-color: var(--bg);
    background-image:
      radial-gradient(circle at 18% 6%, var(--bloom-1), transparent 45%),
      radial-gradient(circle at 88% 94%, var(--bloom-2), transparent 50%),
      radial-gradient(circle at 50% 0%, var(--bloom-3), transparent 60%),
      ${GRAIN_DATA_URI};
    background-size: auto, auto, auto, 160px 160px;
    font-family: 'Inter', system-ui, -apple-system, sans-serif;
    color: var(--ink); text-align: center; -webkit-font-smoothing: antialiased;
  }
  .eyebrow {
    font-size: 12px; font-weight: 600; letter-spacing: .14em; text-transform: uppercase;
    color: var(--accent);
    opacity: 0; animation: riseIn .55s ease forwards;
  }
  h1 {
    font-family: 'Fraunces', Georgia, serif; font-size: 2.2rem; font-weight: 600;
    margin: 12px 0 0; line-height: 1.15; max-width: 380px; text-wrap: balance;
    opacity: 0; animation: riseIn .55s ease .08s forwards;
  }
  .divider {
    width: 52px; height: 3px; margin: 18px auto 0; border-radius: 3px;
    background: linear-gradient(90deg, transparent, var(--accent-2), transparent);
    opacity: 0; animation: riseIn .55s ease .16s forwards;
  }
  p.sub { max-width: 320px; margin: 14px auto 0; color: var(--ink-soft); font-size: 15px; }
  .footer-tag {
    margin-top: 56px; font-size: 11px; color: var(--ink-soft); opacity: 0.7; text-decoration: none;
    opacity: 0; animation: riseIn .55s ease .5s forwards;
  }
  .footer-tag:hover { color: var(--accent); }
  @keyframes riseIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
  }
  @media (prefers-reduced-motion: reduce) {
    .eyebrow, h1, .divider, .link-btn, .footer-tag { animation: none !important; opacity: 1 !important; transform: none !important; }
  }
`;

function shell({ title, description, theme, bodyStyle = "", body }) {
  return `<!doctype html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="description" content="${escapeHtml(description)}">
<title>${escapeHtml(title)}</title>
${FONTS_LINK}
<style>
:root { ${themeVars(theme)} }
${BASE_STYLE}${bodyStyle}
</style>
</head>
<body>
${body}
</body>
</html>`;
}

export function renderClientPage(restaurant) {
  const { name, links, theme = "simple" } = restaurant;
  const few = links.length <= 2;

  const linksStyle = `
    .links {
      display: flex; flex-wrap: wrap; justify-content: center;
      gap: ${few ? "26px 24px" : "22px 18px"}; max-width: 360px; margin-top: 36px;
    }
    .link-btn {
      display: flex; flex-direction: column; align-items: center; gap: 10px;
      width: ${few ? "96px" : "84px"}; text-decoration: none; color: inherit;
      opacity: 0; animation: riseIn .5s ease forwards;
    }
    .link-circle {
      width: ${few ? "88px" : "72px"}; height: ${few ? "88px" : "72px"}; border-radius: 50%;
      background: radial-gradient(circle at 32% 26%, var(--circle-a) 0%, var(--circle-b) 55%, var(--circle-c) 100%);
      border: 1px solid var(--circle-border);
      display: flex; align-items: center; justify-content: center;
      box-shadow: 0 10px 22px -10px rgba(0,0,0,.35), inset 0 1px 1px rgba(255,255,255,.35);
      color: var(--circle-icon);
      transition: transform .15s ease, box-shadow .15s ease;
    }
    .link-btn:active .link-circle {
      transform: scale(.93);
      box-shadow: 0 4px 10px -6px rgba(0,0,0,.3), inset 0 1px 1px rgba(255,255,255,.25);
    }
    .link-icon { width: ${few ? "34px" : "28px"}; height: ${few ? "34px" : "28px"}; }
    .link-label { font-size: ${few ? "13.5px" : "12.5px"}; font-weight: 600; line-height: 1.25; max-width: 90px; }
  `;

  const linksHtml = links
    .map(
      (link, i) => `
      <a class="link-btn" style="animation-delay:${(0.22 + i * 0.07).toFixed(2)}s" href="${escapeHtml(link.url)}" target="_blank" rel="noopener noreferrer">
        <span class="link-circle"><svg viewBox="0 0 24 24" class="link-icon">${getIconSvg(link.icon)}</svg></span>
        <span class="link-label">${escapeHtml(link.label)}</span>
      </a>`,
    )
    .join("");

  const body = `
    <span class="eyebrow">Bienvenue</span>
    <h1>${escapeHtml(name)}</h1>
    <div class="divider"></div>
    <div class="links">${linksHtml}</div>
    <a class="footer-tag" href="/">Propulsé par ${SITE_NAME}</a>
  `;

  return shell({
    title: `${name} — ${SITE_NAME}`,
    description: `Menu, réseaux et liens de ${name}, accessibles en un scan.`,
    theme,
    bodyStyle: linksStyle,
    body,
  });
}

export function renderNotFoundPage() {
  const style = `
    .actions { margin-top: 28px; display: flex; flex-wrap: wrap; gap: 14px; justify-content: center; }
    .btn {
      display: inline-flex; align-items: center; justify-content: center;
      border-radius: 999px; padding: 12px 24px; font-size: 14px; font-weight: 600;
      text-decoration: none;
      opacity: 0; animation: riseIn .5s ease .2s forwards;
    }
    .btn-outline { border: 2px solid var(--accent-2); color: var(--ink); }
  `;

  const body = `
    <span class="eyebrow">404</span>
    <h1 style="font-size:1.75rem;">Cette page n'existe pas</h1>
    <div class="divider"></div>
    <p class="sub">Le lien scanné ne correspond à aucun établissement.</p>
    <div class="actions">
      <a class="btn btn-outline" href="/">Retour à l'accueil</a>
    </div>
    <a class="footer-tag" href="/">${SITE_NAME}</a>
  `;

  return shell({
    title: `Page introuvable — ${SITE_NAME}`,
    description: "Cette page n'existe pas.",
    theme: "simple",
    bodyStyle: style,
    body,
  });
}
