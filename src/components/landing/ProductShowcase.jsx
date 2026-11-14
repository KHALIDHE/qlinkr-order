import brassPlate from "../../assets/mockups/mockup-brass-plate-1.avif";
import standCards from "../../assets/mockups/mockup-3.avif";
import roundStandee from "../../assets/mockups/mockup-round-standee.webp";
import detail1 from "../../assets/mockups/mockup-5.avif";
import detail2 from "../../assets/mockups/mockup-6.avif";
import detail3 from "../../assets/mockups/mockup-7.avif";
import Reveal from "../ui/Reveal";

const images = [
  {
    src: standCards,
    alt: "Cartes en bois sur socle : Scan for Menu, Enjoy the WiFi, Scan to Connect",
    caption: "Menu, wifi, réseaux — au choix",
    rotate: "-rotate-2",
    span: "sm:col-span-2 sm:row-span-2",
  },
  {
    src: brassPlate,
    alt: "Plaque gravée dorée fixée sur bois clair",
    caption: "Finition laiton gravé",
    rotate: "rotate-3",
    span: "",
  },
  {
    src: roundStandee,
    alt: "Support rond en bois avec QR code gravé et logo",
    caption: "Avis Google en un scan",
    rotate: "-rotate-1",
    span: "",
  },
  {
    src: detail1,
    alt: "Carte QR en bois tenue à la main",
    caption: "Format carte, léger et solide",
    rotate: "rotate-2",
    span: "",
  },
  {
    src: detail2,
    alt: "Présentoirs en bois gravés au nom de l'établissement",
    caption: "Gravé à votre nom",
    rotate: "-rotate-2",
    span: "sm:col-span-2",
  },
  {
    src: detail3,
    alt: "Plaque QR posée sur une table, logo du restaurant",
    caption: "Une identité, pas un gadget",
    rotate: "rotate-1",
    span: "",
  },
];

export default function ProductShowcase() {
  return (
    <section className="py-20 sm:py-28">
      <div className="mx-auto max-w-6xl px-5 sm:px-8">
        <Reveal className="mx-auto max-w-xl text-center">
          <span className="text-xs font-semibold tracking-widest text-wood-600 uppercase">
            Le produit
          </span>
          <h2 className="mt-3 font-display text-3xl font-semibold text-espresso sm:text-4xl">
            Du bois, une gravure, un QR code
          </h2>
          <p className="mt-4 text-espresso-soft">
            Chaque carte est fabriquée et gravée à la main. Format, texte et
            finition s'adaptent à l'identité de votre établissement.
          </p>
        </Reveal>

        <div className="mt-16 grid grid-cols-2 gap-x-4 gap-y-10 sm:grid-cols-3 sm:gap-x-6 sm:gap-y-14">
          {images.map((image, index) => (
            <Reveal
              key={image.src}
              delay={index * 90}
              className={`group ${image.span}`}
            >
              <div
                className={`overflow-hidden rounded-sm border-[10px] border-paper bg-paper shadow-warm transition-transform duration-300 group-hover:rotate-0 group-hover:scale-[1.03] ${image.rotate}`}
              >
                <div className="overflow-hidden bg-wood-100">
                  <img
                    src={image.src}
                    alt={image.alt}
                    loading="lazy"
                    className="aspect-square w-full object-cover"
                  />
                </div>
              </div>
              <p
                className={`mt-3 text-center font-script text-lg text-espresso-soft ${image.rotate}`}
              >
                {image.caption}
              </p>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
