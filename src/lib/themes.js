// Keep these `value`s in sync with THEME_KEYS in server/db.js and the
// THEMES map in server/render/clientPage.js. `swatch` is just for the
// admin picker preview — the real colors live in the server template.
export const THEME_OPTIONS = [
  { value: "simple", label: "Simple", swatch: "#cba75c" },
  { value: "restaurant", label: "Restaurant", swatch: "#d8b26a" },
  { value: "cafe", label: "Café", swatch: "#c9713f" },
  { value: "tearoom", label: "Salon de thé", swatch: "#b98a8f" },
  { value: "gym", label: "Salle de sport", swatch: "#c8ff4d" },
  { value: "beauty", label: "Salon de beauté", swatch: "#c78fa0" },
];
