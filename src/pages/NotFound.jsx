import { Link } from "react-router-dom";
import { SITE_NAME, buildWhatsAppLink } from "../config/site";

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-cream px-6 py-16 text-center">
      <span className="text-xs font-semibold tracking-widest text-wood-500 uppercase">
        404
      </span>
      <h1 className="mt-3 font-display text-3xl font-semibold text-espresso">
        Cette page n'existe pas
      </h1>
      <p className="mt-3 max-w-sm text-espresso-soft">
        Le lien scanné ne correspond à aucun établissement. Si vous êtes
        propriétaire d'un commerce, contactez-nous pour configurer votre page.
      </p>

      <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
        <Link
          to="/"
          className="rounded-full border-2 border-wood-300 px-6 py-3 text-sm font-semibold text-espresso hover:bg-wood-50"
        >
          Retour à l'accueil
        </Link>
        <a
          href={buildWhatsAppLink()}
          target="_blank"
          rel="noopener noreferrer"
          className="rounded-full bg-whatsapp px-6 py-3 text-sm font-semibold text-white hover:bg-whatsapp-dark"
        >
          Nous contacter
        </a>
      </div>

      <p className="mt-16 text-xs text-espresso-soft/70">{SITE_NAME}</p>
    </div>
  );
}
