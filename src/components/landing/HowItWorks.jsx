import WhatsAppButton from "../ui/WhatsAppButton";
import Reveal from "../ui/Reveal";

const steps = [
  {
    number: "01",
    title: "Vous commandez",
    text: "Envoyez-nous sur WhatsApp le nom de votre établissement et vos liens : menu, Instagram, site web.",
  },
  {
    number: "02",
    title: "On design & on fabrique",
    text: "Nous créons vos cartes gravées sur bois, avec un QR code unique par table ou par établissement, prêtes en quelques jours.",
  },
  {
    number: "03",
    title: "Vous les installez",
    text: "Posez-les sur vos tables ou votre comptoir. Vos clients scannent et accèdent à tout, instantanément.",
  },
];

export default function HowItWorks() {
  return (
    <section id="comment-ca-marche" className="bg-cream-dim/60 py-20 sm:py-28">
      <div className="mx-auto max-w-4xl px-5 sm:px-8">
        <Reveal className="mx-auto max-w-xl text-center">
          <span className="text-xs font-semibold tracking-widest text-wood-600 uppercase">
            Simple, du début à la fin
          </span>
          <h2 className="mt-3 font-display text-3xl font-semibold text-espresso sm:text-4xl">
            Comment ça marche
          </h2>
        </Reveal>

        <div className="mt-16 divide-y divide-wood-300/60 border-y border-wood-300/60">
          {steps.map((step, index) => (
            <Reveal key={step.number} delay={index * 100}>
              <div className="group relative flex flex-col gap-2 py-9 sm:flex-row sm:items-center sm:gap-10 sm:py-11">
                <span className="font-display text-6xl leading-none font-semibold text-wood-300 transition-colors duration-300 group-hover:text-brass-400 sm:w-28 sm:shrink-0 sm:text-7xl">
                  {step.number}
                </span>
                <div>
                  <h3 className="font-display text-xl font-semibold text-espresso sm:text-2xl">
                    {step.title}
                  </h3>
                  <p className="mt-2 max-w-lg leading-relaxed text-espresso-soft">
                    {step.text}
                  </p>
                </div>
              </div>
            </Reveal>
          ))}
        </div>

        <div className="mt-14 flex justify-center">
          <WhatsAppButton size="lg">Démarrer ma commande</WhatsAppButton>
        </div>
      </div>
    </section>
  );
}
