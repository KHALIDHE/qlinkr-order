import { SITE_NAME } from "../../config/site";
import WhatsAppButton from "../ui/WhatsAppButton";

const links = [
  { href: "#comment-ca-marche", label: "Comment ça marche" },
  { href: "#tarifs", label: "Tarifs" },
];

export default function Nav() {
  return (
    <header className="sticky top-0 z-40 border-b border-wood-200/60 bg-cream/85 backdrop-blur-md">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-5 py-4 sm:px-8">
        <a href="#top" className="font-display text-xl font-semibold tracking-tight text-espresso">
          {SITE_NAME}
        </a>

        <nav className="hidden items-center gap-8 md:flex">
          {links.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className="text-sm font-medium text-espresso-soft transition-colors hover:text-wood-600"
            >
              {link.label}
            </a>
          ))}
        </nav>

        <WhatsAppButton size="sm">Commander</WhatsAppButton>
      </div>
    </header>
  );
}
