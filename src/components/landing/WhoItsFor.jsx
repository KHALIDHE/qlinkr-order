import Reveal from "../ui/Reveal";

const segments = [
  {
    label: "Restaurants",
    rotate: "-rotate-2",
    icon: (
      <path
        d="M6 3v7a2 2 0 0 0 2 2v9M6 3a2 2 0 0 0-2 2v5a2 2 0 0 0 2 2M6 3a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2M18 3c-1.5 0-3 1.5-3 4.5S16.5 12 18 12v9"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    ),
  },
  {
    label: "Cafés",
    rotate: "rotate-1",
    icon: (
      <path
        d="M4 8h13a3 3 0 0 1 0 6h-1M17 8v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8ZM8 3c0 1-1 1-1 2M12 3c0 1-1 1-1 2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    ),
  },
  {
    label: "Salons de thé",
    rotate: "-rotate-1",
    icon: (
      <path
        d="M4 12h16M4 12c0 4.4 3.6 8 8 8s8-3.6 8-8M4 12l1-6h14l1 6M9 6V4M15 6V4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    ),
  },
  {
    label: "Salles de sport",
    rotate: "rotate-2",
    icon: (
      <path
        d="M6.5 6.5l11 11M4 9l3-3M17 20l3-3M2.5 12l3.5-3.5M18 8l3.5-3.5M8 22l3.5-3.5M15.5 4l3.5-3.5M16 15.5l3.5 3.5M4.5 4.5L8 8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    ),
  },
  {
    label: "Salons de beauté",
    rotate: "-rotate-2",
    icon: (
      <path
        d="M12 3a3 3 0 0 1 3 3c0 1.5-1.2 2.5-2 3.5-.5.6-1 1.2-1 2M12 3a3 3 0 0 0-3 3c0 1.5 1.2 2.5 2 3.5.5.6 1 1.2 1 2M12 11.5V21M8 21h8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    ),
  },
];

export default function WhoItsFor() {
  return (
    <section className="border-y border-wood-200/60 bg-cream-dim/60 py-20 sm:py-28">
      <div className="mx-auto max-w-6xl px-5 sm:px-8">
        <Reveal className="mx-auto max-w-xl text-center">
          <span className="text-xs font-semibold tracking-widest text-wood-600 uppercase">
            Pour qui
          </span>
          <h2 className="mt-3 text-balance font-display text-3xl font-semibold text-espresso sm:text-4xl">
            Pensé pour les commerces qui reçoivent du monde
          </h2>
          <p className="mt-4 text-espresso-soft">
            Si vos clients s'assoient à une table ou attendent quelques
            minutes, une carte QR leur donne accès à tout, instantanément.
          </p>
        </Reveal>

        <div className="mt-16 flex flex-wrap justify-center gap-x-6 gap-y-12 sm:gap-x-8">
          {segments.map((segment, index) => (
            <Reveal
              key={segment.label}
              delay={index * 80}
              className={`group relative w-32 shrink-0 pt-4 transition-transform duration-300 hover:-translate-y-1 sm:w-36 ${segment.rotate} hover:rotate-0`}
            >
              {/* grommet / hanging hole */}
              <span className="absolute top-0 left-1/2 z-10 h-4 w-4 -translate-x-1/2 rounded-full border-2 border-wood-400 bg-cream-dim shadow-[inset_0_1px_2px_rgba(74,51,28,0.25)]" />

              <div className="flex flex-col items-center gap-3 rounded-2xl border border-wood-300/80 bg-wood-50 px-4 pt-8 pb-6 text-center shadow-soft">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  className="h-7 w-7 text-wood-700"
                >
                  {segment.icon}
                </svg>
                <span className="font-display text-sm font-semibold text-espresso">
                  {segment.label}
                </span>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
