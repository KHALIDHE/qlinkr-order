import { useEffect, useState } from "react";
import QRCode from "qrcode";

export default function QrPreview({ url, filename }) {
  const [svg, setSvg] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    setSvg(null);
    setError("");

    QRCode.toString(url, {
      type: "svg",
      errorCorrectionLevel: "H",
      color: { dark: "#000000", light: "#FFFFFF" },
    })
      .then((result) => {
        if (!cancelled) setSvg(result);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      });

    return () => {
      cancelled = true;
    };
  }, [url]);

  function download() {
    if (!svg) return;
    const blob = new Blob([svg], { type: "image/svg+xml" });
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = objectUrl;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(objectUrl);
  }

  if (error) return <p className="text-sm text-red-600">Erreur QR : {error}</p>;
  if (!svg) return <p className="text-sm text-espresso-soft">Génération du QR code…</p>;

  return (
    <div className="flex flex-col items-center gap-3">
      {/* Trusted, deterministic markup from the `qrcode` library — not raw user input. */}
      <div
        className="h-40 w-40 rounded-lg border border-wood-200 bg-white p-2 [&_svg]:h-full [&_svg]:w-full"
        dangerouslySetInnerHTML={{ __html: svg }}
      />
      <button
        type="button"
        onClick={download}
        className="rounded-lg bg-wood-800 px-4 py-2 text-sm font-semibold text-cream hover:bg-wood-900"
      >
        Télécharger le SVG
      </button>
    </div>
  );
}
