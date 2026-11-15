import { useEffect, useState } from "react";
import { orderApi, listen, money } from "../order/orderApi";

const FINAL = ["COMPLETED", "CANCELLED", "REJECTED"];

export default function StaffPage() {
  const [token, setToken] = useState(sessionStorage.getItem("qo_token") || "");
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState("");

  const logout = () => { sessionStorage.removeItem("qo_token"); setToken(""); setOrders([]); };

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
      <form onSubmit={login} className="mx-auto mt-24 max-w-sm space-y-3 rounded-xl bg-paper p-6 shadow">
        <h1 className="font-display text-2xl">Staff</h1>
        <input name="email" type="email" placeholder="Email" required className="w-full rounded border p-2" />
        <input name="password" type="password" placeholder="Password" required className="w-full rounded border p-2" />
        {error && <p className="text-sm text-red-700">{error}</p>}
        <button className="w-full rounded bg-espresso py-2 text-cream">Login</button>
      </form>
    );
  }

  return (
    <div className="mx-auto max-w-3xl p-4 font-sans text-espresso">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="font-display text-2xl">Open orders ({orders.length})</h1>
        <button onClick={logout} className="text-sm underline">Logout</button>
      </div>
      {error && <p className="mb-2 text-sm text-red-700">{error}</p>}
      {orders.length === 0 && <p className="text-espresso-soft">No open order.</p>}
            {[...new Set(orders.map((o) => o.scanPointLabel))].map((table) => (
        <div key={table} className="mb-4 rounded-xl border-2 border-brass-400 p-4">
          <h2 className="mb-3 font-display text-xl">{table}</h2>
          {orders.filter((o) => o.scanPointLabel === table).map((o) => (
            <div key={o.id} className="mb-3 rounded-xl border bg-paper p-4">
              <div className="flex justify-between">
                <b>#{o.id}</b>
                <span className="rounded bg-cream-dim px-2 text-sm">{o.status}</span>
              </div>
              <ul className="my-2 text-sm">
                {o.lines.map((l) => <li key={l.itemId}>{l.quantity} × {l.name}</li>)}
              </ul>
              <p className="mb-2 text-sm text-espresso-soft">{money(o.totalCents)}</p>
              <div className="flex flex-wrap gap-2">
                {o.allowedNext.map((to) => (
                  <button key={to} onClick={() => move(o.id, to)}
                    className="rounded bg-espresso px-3 py-1 text-sm text-cream">{to}</button>
                ))}
              </div>
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}
