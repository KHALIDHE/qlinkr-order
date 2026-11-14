import { buildWhatsAppLink } from "../../config/site";
import WhatsAppIcon from "../ui/WhatsAppIcon";
import Reveal from "../ui/Reveal";

// TODO: replace with your real packages and prices (MAD).
const packages = [
  {
    name: "Essentiel",
    swatch: "bg-wood-200",
    price: "349",
    unit: "MAD",
    description: "Pour tester sur quelques tables.",
    features: ["Jusqu'à 10 cartes QR", "1 lien (menu, Instagram ou site)", "Gravure sur bois clair", "Livraison à Casablanca"],
    highlighted: false,
  },
  {
    name: "Standard",
    swatch: "bg-brass-400",
    price: "690",
    unit: "MAD",
    description: "Le plus choisi par nos clients.",
    features: [
      "Jusqu'à 25 cartes QR",
      "3 liens : menu, Instagram, site web",
      "Design personnalisé (logo, texte)",
      "Livraison à Casablanca",
      "Page de liens incluse",
    ],
    highlighted: true,
  },
  {
    name: "Sur mesure",
    swatch: "bg-wood-700",
    price: "Sur devis",
    unit: "",
    description: "Plusieurs adresses ou grands volumes.",
    features: [
      "Cartes illimitées",
      "Plusieurs établissements",
      "Finitions premium (laiton, bois foncé)",
      "Accompagnement dédié",
    ],
    highlighted: false,
  },
];

export default function Pricing() {
  return (
    <section id="tarifs" className="py-20 sm:py-28">
      <div className="mx-auto max-w-6xl px-5 sm:px-8">
        <Reveal className="mx-auto max-w-xl text-center">
          <span className="text-xs font-semibold tracking-widest text-wood-600 uppercase">
            Tarifs
          </span>
          <h2 className="mt-3 font-display text-3xl font-semibold text-espresso sm:text-4xl">
            Des forfaits simples
          </h2>
          <p className="mt-4 text-espresso-soft">
            Prix indicatifs — écrivez-nous pour un devis adapté à votre
            établissement.
          </p>
        </Reveal>

        <div className="mt-14 grid gap-6 lg:grid-cols-3">
          {packages.map((pkg, index) => (
            <Reveal key={pkg.name} delay={index * 100}>
              <div
                className={`relative flex h-full flex-col overflow-hidden rounded-3xl border ${
                  pkg.highlighted
                    ? "border-wood-500 bg-wood-800 text-cream shadow-warm lg:-translate-y-3"
                    : "border-wood-200/70 bg-paper shadow-soft"
                }`}
              >
                <span className={`block h-2.5 w-full ${pkg.swatch}`} aria-hidden="true" />

                <div className="flex flex-1 flex-col p-8">
                  {pkg.highlighted && (
                    <span className="mb-4 inline-flex w-fit -rotate-2 items-center rounded-full border border-dashed border-brass-300 px-3 py-1 font-script text-base text-brass-300">
                      le plus populaire
                    </span>
                  )}

                  <h3 className={`font-display text-2xl font-semibold ${pkg.highlighted ? "text-cream" : "text-espresso"}`}>
                    {pkg.name}
                  </h3>
                  <p className={`mt-1 text-sm ${pkg.highlighted ? "text-wood-200" : "text-espresso-soft"}`}>
                    {pkg.description}
                  </p>

                  <div className="mt-6 flex items-baseline gap-1">
                    <span className="font-display text-4xl font-semibold">{pkg.price}</span>
                    {pkg.unit && (
                      <span className={pkg.highlighted ? "text-wood-200" : "text-espresso-soft"}>
                        {pkg.unit}
                      </span>
                    )}
                  </div>

                  <ul className="mt-7 flex-1 space-y-3 text-sm">
                    {pkg.features.map((feature) => (
                      <li key={feature} className="flex items-start gap-2.5">
                        <svg
                          viewBox="0 0 20 20"
                          fill="none"
                          className={`mt-0.5 h-5 w-5 shrink-0 ${pkg.highlighted ? "text-brass-300" : "text-wood-500"}`}
                        >
                          <path
                            d="M5 10.5l3 3 7-7"
                            stroke="currentColor"
                            strokeWidth="2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          />
                        </svg>
                        <span className={pkg.highlighted ? "text-wood-100" : "text-espresso-soft"}>
                          {feature}
                        </span>
                      </li>
                    ))}
                  </ul>

                  <a
                    href={buildWhatsAppLink(`Bonjour ! Je suis intéressé(e) par le forfait "${pkg.name}".`)}
                    target="_blank"
                    rel="noopener noreferrer"
                    className={`mt-8 inline-flex items-center justify-center gap-2 rounded-full px-6 py-3.5 text-sm font-semibold transition-transform hover:scale-[1.02] active:scale-[0.98] ${
                      pkg.highlighted
                        ? "bg-cream text-wood-800 hover:bg-white"
                        : "bg-wood-800 text-cream hover:bg-wood-900"
                    }`}
                  >
                    <WhatsAppIcon className="h-4 w-4" />
                    Choisir ce forfait
                  </a>
                </div>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
