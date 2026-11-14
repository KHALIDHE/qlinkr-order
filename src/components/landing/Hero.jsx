import heroImage from "../../assets/mockups/mockup-4.avif";
import sideImage from "../../assets/mockups/mockup-brass-plate-1.avif";
import WhatsAppButton from "../ui/WhatsAppButton";
import Stamp from "../ui/Stamp";
import Reveal from "../ui/Reveal";

export default function Hero() {
  return (
    <section id="top" className="relative overflow-hidden pt-14 pb-24 sm:pt-20 sm:pb-32">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -top-24 -right-32 h-96 w-96 rounded-full bg-wood-200/40 blur-3xl"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute top-40 -left-40 h-80 w-80 rounded-full bg-brass-300/30 blur-3xl"
      />

      <div className="relative mx-auto grid max-w-6xl items-center gap-16 px-5 sm:px-8 lg:grid-cols-[1.05fr_0.95fr] lg:gap-10">
        <Reveal>
          <span className="inline-flex items-center rounded-full border border-wood-300 bg-wood-50 px-4 py-1.5 text-xs font-semibold tracking-wide text-wood-700 uppercase">
            Pour commerces à Casablanca
          </span>

          <h1 className="mt-7 text-balance font-display text-[2.75rem] leading-[1.04] font-semibold text-espresso sm:text-6xl lg:text-[3.75rem]">
            Un QR code en bois.
            <br />
            Menu, Instagram, site —
            <span className="relative ml-3 inline-block">
              <span className="relative z-10 font-script text-[1.15em] leading-none font-semibold text-wood-600">
                en un scan.
              </span>
              <svg
                viewBox="0 0 220 20"
                preserveAspectRatio="none"
                className="absolute -bottom-1 left-0 h-4 w-full text-brass-500"
                aria-hidden="true"
              >
                <path
                  d="M2 14C40 6 100 4 130 10C160 16 195 6 218 12"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="3"
                  strokeLinecap="round"
                />
              </svg>
            </span>
          </h1>

          <p className="mt-7 max-w-md text-lg leading-relaxed text-espresso-soft">
            Des cartes QR gravées à la main pour vos tables. Vos clients
            scannent une fois et retrouvent tout : menu, Instagram, site web.
            Fini les affiches qui s'accumulent.
          </p>

          <div className="mt-9 flex flex-wrap items-center gap-4">
            <WhatsAppButton size="lg">Commander sur WhatsApp</WhatsAppButton>
            <a
              href="#tarifs"
              className="inline-flex items-center rounded-full px-6 py-4 text-sm font-semibold text-espresso underline decoration-wood-400 decoration-2 underline-offset-4 transition-colors hover:text-wood-600"
            >
              Voir les tarifs
            </a>
          </div>

          <div className="mt-11 flex items-center gap-6 text-sm text-espresso-soft">
            <div className="flex items-center gap-2">
              <span className="h-1.5 w-1.5 rounded-full bg-wood-500" />
              Fabriqué au Maroc
            </div>
            <div className="flex items-center gap-2">
              <span className="h-1.5 w-1.5 rounded-full bg-wood-500" />
              Prêt en quelques jours
            </div>
          </div>
        </Reveal>

        <Reveal delay={150} className="relative mx-auto w-full max-w-md py-6 lg:max-w-none lg:py-0">
          <img
            src={sideImage}
            alt=""
            aria-hidden="true"
            className="absolute top-0 right-2 z-0 w-40 rotate-[9deg] rounded-2xl border-8 border-paper object-cover shadow-warm sm:w-48 lg:-top-6 lg:right-0"
          />
          <img
            src={heroImage}
            alt="Cartes QR gravées sur bois, posées sur une table de restaurant"
            className="relative z-10 mt-10 w-[88%] -rotate-3 rounded-2xl border-8 border-paper object-cover shadow-warm sm:mt-14"
          />
          <Stamp className="absolute -bottom-6 left-2 z-20 bg-cream sm:-bottom-8 sm:left-0" />
        </Reveal>
      </div>
    </section>
  );
}
