import { useEffect, useState } from "react";
import { orderApi, listen, money } from "../order/orderApi";

const FINAL = ["COMPLETED", "CANCELLED", "REJECTED"];

// Colour of each status: the bar on the left of an order and its label.
const LOOK = {
  NEW: { bar: "border-l-amber-500", chip: "bg-amber-100 text-amber-900", name: "New" },
  ACCEPTED: { bar: "border-l-sky-500", chip: "bg-sky-100 text-sky-900", name: "Accepted" },
  PREPARING: { bar: "border-l-orange-500", chip: "bg-orange-100 text-orange-900", name: "Preparing" },
  READY: { bar: "border-l-green-600", chip: "bg-green-100 text-green-900", name: "Ready" },
};
const BAD = ["CANCELLED", "REJECTED"];
const ACTION = { ACCEPTED: "Accept", PREPARING: "Start preparing", READY: "Mark ready", COMPLETED: "Served", CANCELLED: "Cancel", REJECTED: "Reject" };

function age(createdAt, now) {
  const minutes = Math.max(0, Math.round((now - new Date(createdAt).getTime()) / 60000));
  return minutes < 1 ? "just now" : minutes < 60 ? `${minutes} min` : `${Math.floor(minutes / 60)} h ${minutes % 60} min`;
}

export default function StaffPage() {
  const [token, setToken] = useState(sessionStorage.getItem("qo_token") || "");
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState("");
  const [now, setNow] = useState(Date.now());

  const logout = () => { sessionStorage.removeItem("qo_token"); setToken(""); setOrders([]); };

  // The time on each order moves forward by itself.
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 30000);
    return () => clearInterval(id);
  }, []);

  // Once logged in: load the open orders, then listen for live changes of my business.
  useEffect(() => {
    if (!token) return undefined;
    let stop = () => {};
    orderApi.me(token).then((me) => {
      orderApi.openOrders(token).then(setOrders);
      stop = listen(`/topic/business.${me.businessId}.orders`, (o) =>
        setOrders((list) => {
          const rest = list.filter((x) => x.id !== o.id);
          return FINAL.includes(o.status) ? rest : [...rest, o].sort((a, b) => a.id - b.id);
        }), token);
    }).catch(logout);
    return () => stop();
  }, [token]);

  const login = async (e) => {
    e.preventDefault();
    const form = new FormData(e.target);
    try {
      const res = await orderApi.login(form.get("email"), form.get("password"));
      sessionStorage.setItem("qo_token", res.accessToken);
      setToken(res.accessToken);
      setError("");
    } catch (err) {
      setError(err.message);
    }
  };

  const move = (id, to) => orderApi.setStatus(token, id, to).catch((e) => setError(e.message));

  if (!token) {
    return (
      <div className="min-h-screen bg-cream px-4 pt-24 font-sans text-espresso">
        <form onSubmit={login} className="mx-auto max-w-sm space-y-3 rounded-2xl border border-wood-200 bg-paper p-6 shadow-sm">
          <h1 className="font-display text-3xl">Staff</h1>
          <p className="text-sm text-espresso-soft">Sign in to see the orders of your tables.</p>
          <input name="email" type="email" placeholder="Email" required className="w-full rounded-lg border border-wood-200 bg-white p-2.5" />
          <input name="password" type="password" placeholder="Password" required className="w-full rounded-lg border border-wood-200 bg-white p-2.5" />
          {error && <p className="text-sm text-red-700">{error}</p>}
          <button className="w-full rounded-lg bg-espresso py-2.5 font-medium text-cream hover:bg-espresso-soft">Login</button>
        </form>
      </div>
    );
  }

  const count = (status) => orders.filter((o) => o.status === status).length;
  const tables = [...new Set(orders.map((o) => o.scanPointLabel))];

  return (
    <div className="min-h-screen bg-cream font-sans text-espresso">
      <div className="mx-auto max-w-3xl p-4">
        <div className="mb-3 flex items-center justify-between">
          <h1 className="font-display text-3xl">Open orders</h1>
          <button onClick={logout} className="text-sm underline">Logout</button>
        </div>
        <div className="mb-4 flex flex-wrap gap-2">
          {Object.entries(LOOK).map(([status, look]) => (
            <span key={status} className={`rounded-full px-3 py-1 text-sm font-medium ${look.chip}`}>{look.name} · {count(status)}</span>
          ))}
        </div>
        {error && <p className="mb-2 text-sm text-red-700">{error}</p>}
        {orders.length === 0 && <p className="rounded-2xl border border-dashed border-wood-300 p-8 text-center text-espresso-soft">No open order. New orders appear here by themselves.</p>}
        {tables.map((table) => (
          <section key={table} className="mb-4 rounded-2xl border border-wood-200 bg-wood-50 p-4">
            <h2 className="mb-3 font-display text-2xl">{table}</h2>
            <div className="space-y-3">
              {orders.filter((o) => o.scanPointLabel === table).map((o) => {
                const look = LOOK[o.status] || LOOK.NEW;
                return (
                  <div key={o.id} className={`rounded-xl border border-wood-200 border-l-8 bg-paper p-4 shadow-sm ${look.bar}`}>
                    <div className="flex items-center justify-between">
                      <b className="text-lg">#{o.id}</b>
                      <span className="flex items-center gap-2">
                        <span className="text-sm text-espresso-soft">{age(o.createdAt, now)}</span>
                        <span className={`rounded-full px-3 py-0.5 text-sm font-medium ${look.chip}`}>{look.name}</span>
                      </span>
                    </div>
                    <ul className="my-2 space-y-0.5">
                      {o.lines.map((l) => <li key={l.itemId}><b>{l.quantity} ×</b> {l.name}</li>)}
                    </ul>
                    {o.note && <p className="mb-2 rounded-lg bg-cream-dim px-3 py-1.5 text-sm">{o.note}</p>}
                    <p className="mb-3 text-sm text-espresso-soft">{money(o.totalCents)}</p>
                    <div className="flex flex-wrap gap-2">
                      {o.allowedNext.map((to) => (
                        <button key={to} onClick={() => move(o.id, to)}
                          className={BAD.includes(to)
                            ? "rounded-lg border border-red-300 px-4 py-2 text-sm font-medium text-red-800 hover:bg-red-50"
                            : "rounded-lg bg-espresso px-4 py-2 text-sm font-medium text-cream hover:bg-espresso-soft"}>
                          {ACTION[to] || to}
                        </button>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          </section>
        ))}
      </div>
    </div>
  );
}
