// ---------------------------------------------------------------------------
// SITE-WIDE CONFIG — edit everything here, nothing else needs to change.
// ---------------------------------------------------------------------------

export const SITE_NAME = "QlinkR";

export const TAGLINE = "Cartes QR en bois pour votre commerce";

// TODO: replace with your real WhatsApp number in international format,
// digits only, no "+", no spaces. Example for Morocco: "212612345678".
export const WHATSAPP_NUMBER = "212652204482";

// Default message pre-filled when someone taps a WhatsApp button.
// {package} is replaced automatically when a pricing card is clicked.
export const WHATSAPP_DEFAULT_MESSAGE =
  "Bonjour ! Je souhaite commander des cartes QR pour mon établissement.";

export function buildWhatsAppLink(message = WHATSAPP_DEFAULT_MESSAGE) {
  const encoded = encodeURIComponent(message);
  return `https://wa.me/${WHATSAPP_NUMBER}?text=${encoded}`;
}

// TODO: replace with your real Instagram handle, or set to null to hide it
// from the footer.
export const INSTAGRAM_URL = null;

// TODO: replace with your real production domain (no trailing slash). This
// is what gets encoded into every QR code — get it right before ordering
// any physical coasters, since it can never be changed after engraving.
export const SITE_URL = "https://qlinkr-cards.pages.dev";

export function clientPageUrl(slug) {
  return `${SITE_URL}/r/${slug}`;
}
