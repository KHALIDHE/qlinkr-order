import WhatsAppButton from "../ui/WhatsAppButton";
import Stamp from "../ui/Stamp";
import Reveal from "../ui/Reveal";

export default function FinalCTA() {
  return (
    <section className="px-5 py-20 sm:px-8 sm:py-24">
      <Reveal className="relative mx-auto max-w-4xl overflow-hidden rounded-[2.5rem] bg-wood-800 px-8 py-16 text-center shadow-warm sm:px-16">
        <div
          aria-hidden="true"
          className="bg-wood-grain pointer-events-none absolute inset-0 opacity-40"
        />
        <Stamp className="absolute -top-4 -right-2 hidden border-cream/40 text-cream/70 sm:flex" />

        <div className="relative">
          <h2 className="text-balance font-display text-3xl font-semibold text-cream sm:text-4xl">
            Prêt à moderniser vos tables ?
          </h2>
          <p className="mx-auto mt-4 max-w-md text-wood-200">
            Écrivez-nous sur WhatsApp — on vous répond en quelques minutes et
            on s'occupe du reste.
          </p>
          <div className="mt-9 flex justify-center">
            <WhatsAppButton size="lg" variant="cream">
              Discuter sur WhatsApp
            </WhatsAppButton>
          </div>
        </div>
      </Reveal>
    </section>
  );
}
