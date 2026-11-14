import { INSTAGRAM_URL, SITE_NAME, TAGLINE, buildWhatsAppLink } from "../../config/site";

export default function Footer() {
  return (
    <footer className="border-t border-wood-200/60 bg-cream-dim/60 py-12">
      <div className="mx-auto flex max-w-6xl flex-col items-center gap-4 px-5 text-center sm:px-8">
        <span className="font-display text-xl font-semibold text-espresso">{SITE_NAME}</span>
        <p className="max-w-xs text-sm text-espresso-soft">{TAGLINE}</p>

        <div className="mt-2 flex items-center gap-6 text-sm text-espresso-soft">
          <a href={buildWhatsAppLink()} target="_blank" rel="noopener noreferrer" className="hover:text-wood-600">
            WhatsApp
          </a>
          {INSTAGRAM_URL && (
            <a href={INSTAGRAM_URL} target="_blank" rel="noopener noreferrer" className="hover:text-wood-600">
              Instagram
            </a>
          )}
        </div>

        <p className="mt-6 text-xs text-espresso-soft/70">
          © {new Date().getFullYear()} {SITE_NAME}. Fabriqué avec soin à Casablanca.
        </p>
      </div>
    </footer>
  );
}
