import Reveal from "../ui/Reveal";

export default function StatementBand() {
  return (
    <section className="relative overflow-hidden bg-wood-900 py-24 sm:py-32">
      <div
        aria-hidden="true"
        className="bg-wood-grain pointer-events-none absolute inset-0 opacity-40"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute top-1/2 left-1/2 h-72 w-72 -translate-x-1/2 -translate-y-1/2 rounded-full bg-brass-500/10 blur-3xl"
      />

      <Reveal className="relative mx-auto max-w-3xl px-6 text-center sm:px-8">
        <p className="text-balance font-display text-[1.7rem] leading-snug font-medium text-cream italic sm:text-4xl">
          Vos clients ne devraient pas chercher votre Instagram, télécharger
          votre menu en PDF illisible ou taper votre nom sur Google.
        </p>
        <p className="mt-8 text-balance font-display text-2xl font-semibold text-brass-300 sm:text-3xl">
          Un scan. Trois liens. Zéro friction.
        </p>
      </Reveal>
    </section>
  );
}
