// Inline SVG bodies (no wrapping <svg> tag) for the circular link buttons.
// Kept as simple single-color line art to match the engraved-wood aesthetic
// — never brand-colored logos, which would clash with the warm/craft style.

export const ICON_OPTIONS = [
  { value: "menu", label: "Menu" },
  { value: "website", label: "Site web" },
  { value: "instagram", label: "Instagram" },
  { value: "facebook", label: "Facebook" },
  { value: "tiktok", label: "TikTok" },
  { value: "whatsapp", label: "WhatsApp" },
  { value: "reviews", label: "Avis / Reviews" },
  { value: "delivery", label: "Livraison" },
  { value: "phone", label: "Téléphone" },
  { value: "email", label: "Email" },
  { value: "generic", label: "Autre" },
];

const ICONS = {
  menu: `<path d="M7 3v18M7 3a2 2 0 0 0-2 2v5a2 2 0 0 0 2 2M7 3a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2M17 3c-1.7 0-3 1.8-3 5s1.3 5 3 5v9" stroke="currentColor" stroke-width="1.6" fill="none" stroke-linecap="round" stroke-linejoin="round"/>`,

  website: `<circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6" fill="none"/><path d="M3 12h18M12 3c2.4 2.6 3.7 5.7 3.7 9s-1.3 6.4-3.7 9c-2.4-2.6-3.7-5.7-3.7-9S9.6 5.6 12 3Z" stroke="currentColor" stroke-width="1.6" fill="none" stroke-linecap="round" stroke-linejoin="round"/>`,

  instagram: `<rect x="3" y="3" width="18" height="18" rx="5" stroke="currentColor" stroke-width="1.6" fill="none"/><circle cx="12" cy="12" r="4.2" stroke="currentColor" stroke-width="1.6" fill="none"/><circle cx="17.2" cy="6.8" r="1.1" fill="currentColor"/>`,

  facebook: `<path d="M14.5 21v-7.5h2.4l.4-3H14.5V8.4c0-.87.24-1.46 1.5-1.46h1.6V4.3c-.28-.04-1.23-.12-2.34-.12-2.31 0-3.9 1.41-3.9 4V10.5H9v3h2.36V21h3.14Z" fill="currentColor"/>`,

  tiktok: `<path d="M14 4v10.6a3.4 3.4 0 1 1-2.4-3.25V13.1a1.6 1.6 0 1 0 1.1 1.5V4h1.3Z" fill="currentColor"/><path d="M14 4c.3 2.1 1.7 3.6 3.7 3.8V9.5c-1.4-.05-2.7-.5-3.7-1.3" fill="currentColor"/>`,

  whatsapp: `<path d="M17.47 14.38c-.3-.15-1.76-.87-2.03-.97-.27-.1-.47-.15-.67.15-.2.3-.77.97-.94 1.16-.17.2-.35.22-.64.08-.3-.15-1.26-.47-2.39-1.48-.88-.79-1.48-1.76-1.65-2.06-.17-.3-.02-.46.13-.6.13-.14.3-.35.44-.52.15-.17.2-.3.3-.5.1-.2.05-.37-.02-.52-.08-.15-.67-1.61-.92-2.21-.24-.58-.49-.5-.67-.51-.17-.01-.37-.01-.57-.01-.2 0-.52.07-.79.37-.27.3-1.04 1.01-1.04 2.48s1.07 2.87 1.21 3.07c.15.2 2.1 3.2 5.08 4.49.71.3 1.26.49 1.7.63.71.22 1.36.19 1.87.12.57-.09 1.76-.72 2-1.42.25-.69.25-1.28.17-1.41-.07-.12-.27-.2-.57-.35Z" fill="currentColor"/><path d="M12 2C6.48 2 2 6.48 2 12c0 1.87.5 3.62 1.4 5.12L2 22l5.04-1.38A9.94 9.94 0 0 0 12 22c5.52 0 10-4.48 10-10S17.52 2 12 2Zm0 18.09a8.06 8.06 0 0 1-4.28-1.23l-.3-.18-3.18.87.85-3.09-.2-.32A8.05 8.05 0 0 1 3.91 12c0-4.46 3.63-8.09 8.09-8.09 4.46 0 8.09 3.63 8.09 8.09 0 4.46-3.63 8.09-8.09 8.09Z" fill="currentColor"/>`,

  reviews: `<path d="M12 3.5l2.6 5.3 5.85.85-4.23 4.12 1 5.83L12 16.8l-5.22 2.8 1-5.83L3.55 9.65l5.85-.85L12 3.5Z" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linejoin="round"/>`,

  delivery: `<rect x="3.5" y="8" width="17" height="12" rx="1.5" stroke="currentColor" stroke-width="1.6" fill="none"/><path d="M3.5 12h17M8 8V6a4 4 0 0 1 8 0v2" stroke="currentColor" stroke-width="1.6" fill="none" stroke-linecap="round"/>`,

  phone: `<path d="M6.6 10.8c1.3 2.6 3.4 4.7 6 6l2-2c.25-.25.6-.33.9-.2 1 .35 2.1.55 3.2.55.5 0 .9.4.9.9v3.1c0 .5-.4.9-.9.9C9.9 20.05 3.95 14.1 3.95 6.3c0-.5.4-.9.9-.9H8c.5 0 .9.4.9.9 0 1.1.2 2.2.55 3.2.13.3.05.65-.2.9l-2.05 2Z" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linejoin="round" stroke-linecap="round"/>`,

  email: `<rect x="3" y="5" width="18" height="14" rx="2" stroke="currentColor" stroke-width="1.6" fill="none"/><path d="M4 7l8 6 8-6" stroke="currentColor" stroke-width="1.6" fill="none" stroke-linecap="round" stroke-linejoin="round"/>`,

  generic: `<path d="M9.5 14.5l5-5M10 8h5.5c1.1 0 2 .9 2 2v5.5M14 16H8.5c-1.1 0-2-.9-2-2V8.5" stroke="currentColor" stroke-width="1.6" fill="none" stroke-linecap="round" stroke-linejoin="round"/>`,
};

export function getIconSvg(key) {
  return ICONS[key] || ICONS.generic;
}
