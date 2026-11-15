import { useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { orderApi, listen, money } from "../order/orderApi";

// Every text on this page, in the three languages of the platform.
const TEXT = {
  fr: { orderN: "Commande", myOrders: "Mes commandes", total: "Total", order: "Commander", sending: "Envoi...", pick: "Choisissez des articles",
        soldOut: "Épuisé", fewLeft: "Dernières pièces", status: "Statut de votre commande", unknown: "QR code inconnu",
        NEW: "Reçue", ACCEPTED: "Acceptée", PREPARING: "En préparation", READY: "Prête !",
        COMPLETED: "Terminée", CANCELLED: "Annulée", REJECTED: "Refusée" },
  en: { orderN: "Order", myOrders: "My orders", total: "Total", order: "Order", sending: "Sending...", pick: "Pick some items",
        soldOut: "Sold out", fewLeft: "Last ones", status: "Your order status", unknown: "Unknown QR code",
        NEW: "Received", ACCEPTED: "Accepted", PREPARING: "Preparing", READY: "Ready!",
        COMPLETED: "Completed", CANCELLED: "Cancelled", REJECTED: "Rejected" },
  ar: { orderN: "الطلب", myOrders: "طلباتي", total: "المجموع", order: "اطلب", sending: "جارٍ الإرسال...", pick: "اختر بعض العناصر",
        soldOut: "نفد", fewLeft: "الكمية الأخيرة", status: "حالة طلبك", unknown: "رمز QR غير معروف",
        NEW: "تم الاستلام", ACCEPTED: "تم القبول", PREPARING: "قيد التحضير", READY: "جاهز!",
        COMPLETED: "مكتمل", CANCELLED: "ملغى", REJECTED: "مرفوض" },
};

const STEPS = ["NEW", "ACCEPTED", "PREPARING", "READY"];

// The four steps of an order, with the current one highlighted. Cancelled and refused orders show in red.
function Progress({ status, t }) {
  if (status === "CANCELLED" || status === "REJECTED") {
    return <p className="mt-2 rounded-lg bg-red-50 px-3 py-2 text-sm font-medium text-red-800">{t[status]}</p>;
  }
  const at = status === "COMPLETED" ? STEPS.length : STEPS.indexOf(status);
  return (
    <ol className="mt-3 flex items-start">
      {STEPS.map((s, i) => (
        <li key={s} className="flex flex-1 flex-col items-center text-center">
          <div className="flex w-full items-center">
            <span className={`h-0.5 flex-1 ${i === 0 ? "bg-transparent" : i <= at ? "bg-brass-400" : "bg-cream-dim"}`} />
            <span className={`h-4 w-4 rounded-full ${i <= at ? "bg-brass-400" : "bg-cream-dim"} ${i === at ? "ring-4 ring-brass-300/60" : ""}`} />
            <span className={`h-0.5 flex-1 ${i === STEPS.length - 1 ? "bg-transparent" : i < at ? "bg-brass-400" : "bg-cream-dim"}`} />
          </div>
          <span className={`mt-1 text-xs leading-tight ${i <= at ? "font-medium text-espresso" : "text-espresso-soft/60"}`}>{t[s]}</span>
        </li>
      ))}
    </ol>
  );
}

// One card per order. It loads the current status once, then follows that order live,
// so every order keeps its own status, whatever the other orders do.
function OrderCard({ order, number, t, onStatus }) {
  useEffect(() => {
    orderApi.order(order.token).then((o) => onStatus(order.token, o.status)).catch(() => {});
    return listen(`/topic/orders.${order.token}`, (o) => onStatus(order.token, o.status));
  }, [order.token]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="mb-3 rounded-2xl border border-wood-200 bg-paper p-4 shadow-sm">
      <div className="flex items-center justify-between">
        <b className="font-display text-lg">{t.orderN} #{number}</b>
        <span className="rounded-full bg-cream-dim px-3 py-1 text-sm font-medium text-wood-800">{t[order.status]}</span>
      </div>
      <Progress status={order.status} t={t} />
      <ul className="my-2 text-sm text-espresso-soft">
        {order.lines.map((l) => <li key={l.itemId}>{l.quantity} × {l.name}</li>)}
      </ul>
      <p className="text-sm">{money(order.totalCents)}</p>
    </div>
  );
}

export default function ScanPage() {
  const { code } = useParams();
  const [lang, setLang] = useState(localStorage.getItem("qo_lang") || "");
  const [data, setData] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const [cart, setCart] = useState({}); // itemId -> quantity
  const storageKey = `qo_orders_${code}`;
  const [orders, setOrders] = useState(() => {
    try { return JSON.parse(localStorage.getItem(storageKey) || "[]"); } catch { return []; }
  });
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");
  const key = useRef(crypto.randomUUID()); // Idempotency-Key of the current cart

  const load = () =>
    orderApi.scan(code, lang || undefined).then(setData).catch(() => setNotFound(true));

  useEffect(() => { load(); }, [code, lang]); // eslint-disable-line react-hooks/exhaustive-deps

  // Keep my orders in the browser, so a refresh does not lose them.
  useEffect(() => { localStorage.setItem(storageKey, JSON.stringify(orders)); }, [orders, storageKey]);

  const setStatus = (token, status) =>
    setOrders((list) => list.map((o) => (o.token === token ? { ...o, status } : o)));

  const t = TEXT[data?.lang] || TEXT.fr;
  const items = (data?.categories || []).flatMap((c) => c.items);
  const lines = Object.entries(cart).filter(([, q]) => q > 0).map(([itemId, quantity]) => ({ itemId: Number(itemId), quantity }));
  const total = lines.reduce((sum, l) => sum + l.quantity * items.find((i) => i.id === l.itemId).priceCents, 0);
  const count = lines.reduce((sum, l) => sum + l.quantity, 0);

  const change = (id, delta) => {
    key.current = crypto.randomUUID(); // a different cart is a different order
    setCart((c) => ({ ...c, [id]: Math.max(0, (c[id] || 0) + delta) }));
  };

  const send = async () => {
    setSending(true);
    setError("");
    try {
      const placed = await orderApi.placeOrder(code, key.current, lines);
      setOrders((list) => [placed, ...list]); // newest first
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

      {orders.length > 0 && (
        <section className="p-4 pb-0">
          <h2 className="mb-2 font-display text-xl text-wood-700">{t.myOrders}</h2>
          {orders.map((o, i) => (
            <OrderCard key={o.token} order={o} number={orders.length - i} t={t} onStatus={setStatus} />
          ))}
        </section>
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
          {sending ? t.sending : lines.length ? `${t.order} · ${count} · ${money(total)}` : t.pick}
        </button>
      </div>
    </div>
  );
}
