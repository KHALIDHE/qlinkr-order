import { useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { orderApi, listen, money } from "../order/orderApi";

// Every text on this page, in the three languages of the platform.
const TEXT = {
  fr: { total: "Total", order: "Commander", sending: "Envoi...", pick: "Choisissez des articles",
        soldOut: "Épuisé", fewLeft: "Dernières pièces", status: "Statut de votre commande", unknown: "QR code inconnu",
        NEW: "Reçue", ACCEPTED: "Acceptée", PREPARING: "En préparation", READY: "Prête !",
        COMPLETED: "Terminée", CANCELLED: "Annulée", REJECTED: "Refusée" },
  en: { total: "Total", order: "Order", sending: "Sending...", pick: "Pick some items",
        soldOut: "Sold out", fewLeft: "Last ones", status: "Your order status", unknown: "Unknown QR code",
        NEW: "Received", ACCEPTED: "Accepted", PREPARING: "Preparing", READY: "Ready!",
        COMPLETED: "Completed", CANCELLED: "Cancelled", REJECTED: "Rejected" },
  ar: { total: "المجموع", order: "اطلب", sending: "جارٍ الإرسال...", pick: "اختر بعض العناصر",
        soldOut: "نفد", fewLeft: "الكمية الأخيرة", status: "حالة طلبك", unknown: "رمز QR غير معروف",
        NEW: "تم الاستلام", ACCEPTED: "تم القبول", PREPARING: "قيد التحضير", READY: "جاهز!",
        COMPLETED: "مكتمل", CANCELLED: "ملغى", REJECTED: "مرفوض" },
};

export default function ScanPage() {
  const { code } = useParams();
  const [lang, setLang] = useState(localStorage.getItem("qo_lang") || "");
  const [data, setData] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const [cart, setCart] = useState({}); // itemId -> quantity
  const [order, setOrder] = useState(null); // { token, status }
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");
  const key = useRef(crypto.randomUUID()); // Idempotency-Key of the current cart

  const load = () =>
    orderApi.scan(code, lang || undefined).then(setData).catch(() => setNotFound(true));

  useEffect(() => { load(); }, [code, lang]); // eslint-disable-line react-hooks/exhaustive-deps

  // Live status of my order, pushed by the server over WebSocket.
  useEffect(() => {
    if (!order?.token) return undefined;
    return listen(`/topic/orders.${order.token}`, (o) => setOrder((prev) => ({ ...prev, status: o.status })));
  }, [order?.token]);

  const t = TEXT[data?.lang] || TEXT.fr;
  const items = (data?.categories || []).flatMap((c) => c.items);
  const lines = Object.entries(cart).filter(([, q]) => q > 0).map(([itemId, quantity]) => ({ itemId: Number(itemId), quantity }));
  const total = lines.reduce((sum, l) => sum + l.quantity * items.find((i) => i.id === l.itemId).priceCents, 0);

  const change = (id, delta) => {
    key.current = crypto.randomUUID(); // a different cart is a different order
    setCart((c) => ({ ...c, [id]: Math.max(0, (c[id] || 0) + delta) }));
  };

  const send = async () => {
    setSending(true);
    setError("");
    try {
      const placed = await orderApi.placeOrder(code, key.current, lines);
      setOrder({ token: placed.token, status: placed.status });
      setCart({});
    } catch (e) {
      setError(e.message);
      load(); // stock may have changed
    } finally {
      setSending(false);
    }
  };

  if (notFound) return <p className="p-8 text-center text-espresso">{TEXT.fr.unknown}</p>;
  if (!data) return <p className="p-8 text-center text-espresso-soft">...</p>;

  return (
    <div dir={data.dir} className="mx-auto min-h-screen max-w-xl bg-cream pb-32 font-sans text-espresso">
      <header className="bg-espresso p-5 text-cream">
        <h1 className="font-display text-2xl">{data.business.name}</h1>
        <p className="text-sm text-brass-300">{data.labels.scanPoint}: {data.scanPoint.label}</p>
        <div className="mt-3 flex gap-2">
          {["fr", "en", "ar"].map((l) => (
            <button key={l} onClick={() => { localStorage.setItem("qo_lang", l); setLang(l); }}
              className={`rounded-full px-3 py-1 text-sm ${data.lang === l ? "bg-brass-400 text-espresso" : "bg-espresso-soft"}`}>
              {l.toUpperCase()}
            </button>
          ))}
        </div>
      </header>

      {order && (
        <div className="m-4 rounded-xl border-2 border-brass-400 bg-paper p-4 text-center">
          <p className="text-sm text-espresso-soft">{t.status}</p>
          <p className="font-display text-3xl">{t[order.status]}</p>
        </div>
      )}

      {data.categories.map((c) => (
        <section key={c.id} className="p-4">
          <h2 className="mb-2 font-display text-xl text-wood-700">{c.name}</h2>
          {c.items.map((i) => (
            <div key={i.id} className="mb-2 flex items-center justify-between rounded-xl bg-paper p-3 shadow-sm">
              <div>
                <p className="font-medium">{i.name}</p>
                <p className="text-sm text-espresso-soft">{money(i.priceCents)}</p>
                {i.fewLeft && <p className="text-xs text-red-700">{t.fewLeft}: {i.fewLeft}</p>}
              </div>
              {i.orderable ? (
                <div className="flex items-center gap-3">
                  <button onClick={() => change(i.id, -1)} className="h-9 w-9 rounded-full bg-cream-dim text-xl">−</button>
                  <span className="w-5 text-center">{cart[i.id] || 0}</span>
                  <button onClick={() => change(i.id, 1)} className="h-9 w-9 rounded-full bg-brass-400 text-xl">+</button>
                </div>
              ) : (
                <span className="text-sm text-red-700">{t.soldOut}</span>
              )}
            </div>
          ))}
        </section>
      ))}

      <div className="fixed inset-x-0 bottom-0 mx-auto max-w-xl border-t bg-paper p-4">
        {error && <p className="mb-2 text-center text-sm text-red-700">{error}</p>}
        <button disabled={!lines.length || sending} onClick={send}
          className="w-full rounded-xl bg-espresso py-3 text-lg text-cream disabled:opacity-40">
          {sending ? t.sending : lines.length ? `${t.order} · ${money(total)}` : t.pick}
        </button>
      </div>
    </div>
  );
}
