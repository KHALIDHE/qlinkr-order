import { useState } from "react";
import { clientPageUrl } from "../../config/site";
import QrPreview from "./QrPreview";

export default function RestaurantList({ restaurants, onEdit, onDelete }) {
  const [qrOpenId, setQrOpenId] = useState(null);

  if (restaurants.length === 0) {
    return <p className="text-sm text-espresso-soft">Aucun établissement pour le moment.</p>;
  }

  return (
    <div className="space-y-3">
      {restaurants.map((r) => {
        const url = clientPageUrl(r.slug);
        return (
          <div key={r.id} className="rounded-xl border border-wood-200 bg-paper p-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="font-semibold text-espresso">{r.name}</p>
                <a href={url} target="_blank" rel="noopener noreferrer" className="text-sm text-wood-600 hover:underline">
                  /r/{r.slug}
                </a>
                <span className="ml-2 text-xs text-espresso-soft/70">
                  {r.links.length} lien{r.links.length > 1 ? "s" : ""} · créé le{" "}
                  {new Date(r.createdAt).toLocaleDateString("fr-FR")}
                </span>
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setQrOpenId(qrOpenId === r.id ? null : r.id)}
                  className="rounded-md border border-wood-300 px-3 py-1.5 text-xs font-semibold text-espresso hover:bg-wood-50"
                >
                  QR
                </button>
                <button
                  type="button"
                  onClick={() => onEdit(r)}
                  className="rounded-md border border-wood-300 px-3 py-1.5 text-xs font-semibold text-espresso hover:bg-wood-50"
                >
                  Modifier
                </button>
                <button
                  type="button"
                  onClick={() => {
                    if (window.confirm(`Supprimer « ${r.name} » ? Cette action est définitive.`)) {
                      onDelete(r.id);
                    }
                  }}
                  className="rounded-md border border-red-200 px-3 py-1.5 text-xs font-semibold text-red-600 hover:bg-red-50"
                >
                  Supprimer
                </button>
              </div>
            </div>

            {qrOpenId === r.id && (
              <div className="mt-4 border-t border-wood-200 pt-4">
                <QrPreview url={url} filename={`${r.slug}-qr.svg`} />
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
