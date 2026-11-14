import { buildWhatsAppLink, WHATSAPP_DEFAULT_MESSAGE } from "../../config/site";
import WhatsAppIcon from "./WhatsAppIcon";

const sizes = {
  lg: "px-8 py-4 text-base gap-3",
  md: "px-6 py-3.5 text-sm gap-2.5",
  sm: "px-5 py-2.5 text-sm gap-2",
};

export default function WhatsAppButton({
  message = WHATSAPP_DEFAULT_MESSAGE,
  size = "md",
  variant = "solid",
  className = "",
  children = "Commander sur WhatsApp",
}) {
  const base =
    "inline-flex items-center justify-center rounded-full font-semibold transition-transform duration-150 hover:scale-[1.03] active:scale-[0.98] whitespace-nowrap";

  const variants = {
    solid: "bg-whatsapp text-white shadow-soft hover:bg-whatsapp-dark",
    outline: "border-2 border-whatsapp text-whatsapp bg-transparent hover:bg-whatsapp/10",
    cream: "bg-cream text-wood-800 shadow-soft hover:bg-white",
  };

  return (
    <a
      href={buildWhatsAppLink(message)}
      target="_blank"
      rel="noopener noreferrer"
      className={`${base} ${variants[variant]} ${sizes[size]} ${className}`}
    >
      <WhatsAppIcon className={size === "lg" ? "w-6 h-6" : "w-5 h-5"} />
      {children}
    </a>
  );
}
