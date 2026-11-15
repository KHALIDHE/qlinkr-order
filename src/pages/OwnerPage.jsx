import { useEffect, useState } from "react";
import { orderApi, ownerApi, money } from "../order/orderApi";

const itemName = (i) => i.texts?.fr?.name || Object.values(i.texts || {})[0]?.name || "";
const catName = (c) => c.names?.fr || Object.values(c.names || {})[0] || "";
const box = "rounded-xl border-2 border-brass-400 bg-paper p-4";
const input = "rounded border p-2";
const btn = "rounded bg-espresso px-3 py-1 text-sm text-cream";

// A QR code image. The server wants the login token, so we download it and show it.
function QrImage({ token, id }) {
  const [src, setSrc] = useState("");
  useEffect(() => {
    let url = "";
    ownerApi.file(token, `/api/scan-points/${id}/qr.png?size=300`).then((b) => setSrc((url = URL.createObjectURL(b))));
    return () => url && URL.revokeObjectURL(url);
  }, [token, id]);
  return src ? <img src={src} alt="QR code" className="h-32 w-32" /> : <div className="h-32 w-32" />;
}

function MenuTab({ token, fail }) {
  const [cats, setCats] = useState([]);
  const [items, setItems] = useState([]);
  const load = () => {
    ownerApi.categories(token).then(setCats).catch((e) => fail(e.message));
    ownerApi.items(token).then(setItems).catch((e) => fail(e.message));
  };
  useEffect(load, [token]); // eslint-disable-line react-hooks/exhaustive-deps
  const run = (p) => p.then(load).catch((e) => fail(e.message));

  const addCat = (e) => {
    e.preventDefault();
    const name = new FormData(e.target).get("name").trim();
    e.target.reset();
    run(ownerApi.addCategory(token, name));
  };
  const addItem = (e) => {
    e.preventDefault();
    const f = new FormData(e.target);
    e.target.reset();
    run(ownerApi.addItem(token, Number(f.get("cat")), f.get("name").trim(), Math.round(Number(f.get("price")) * 100)));
  };
  const price = (i) => {
    const v = window.prompt(`New price for ${itemName(i)} (MAD)`, i.priceCents / 100);
    if (v !== null && !Number.isNaN(Number(v))) run(ownerApi.patchItem(token, i.id, { priceCents: Math.round(Number(v) * 100) }));
  };

  return (
    <div>
      <form onSubmit={addCat} className="mb-4 flex gap-2">
        <input name="name" placeholder="New category (Drinks, Food...)" required className={`${input} flex-1`} />
        <button className={btn}>Add category</button>
      </form>
      {cats.length > 0 && (
        <form onSubmit={addItem} className="mb-4 flex flex-wrap gap-2">
          <select name="cat" className={input}>{cats.map((c) => <option key={c.id} value={c.id}>{catName(c)}</option>)}</select>
          <input name="name" placeholder="Item name" required className={`${input} flex-1`} />
          <input name="price" type="number" step="0.5" min="0" placeholder="Price MAD" required className={`${input} w-28`} />
          <button className={btn}>Add item</button>
        </form>
      )}
      {cats.map((c) => (
        <div key={c.id} className={`${box} mb-3`}>
          <h2 className="mb-2 font-display text-xl">{catName(c)}</h2>
          {items.filter((i) => i.categoryId === c.id).map((i) => (
            <div key={i.id} className="mb-2 flex flex-wrap items-center justify-between gap-2 border-b pb-2 last:border-0">
              <span className={i.available ? "" : "text-espresso-soft line-through"}>{itemName(i)} · {money(i.priceCents)}</span>
              <span className="flex gap-2">
                <button onClick={() => price(i)} className={btn}>Price</button>
                <button onClick={() => run(ownerApi.patchItem(token, i.id, { available: !i.available }))} className={btn}>
                  {i.available ? "Sold out" : "Available"}
                </button>
                <button onClick={() => window.confirm(`Delete ${itemName(i)}?`) && run(ownerApi.deleteItem(token, i.id))} className={btn}>Delete</button>
              </span>
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}

function TablesTab({ token, fail }) {
  const [points, setPoints] = useState([]);
  const load = () => ownerApi.scanPoints(token).then(setPoints).catch((e) => fail(e.message));
  useEffect(() => { load(); }, [token]); // eslint-disable-line react-hooks/exhaustive-deps

  const add = (e) => {
    e.preventDefault();
    const label = new FormData(e.target).get("label").trim();
    e.target.reset();
    ownerApi.addScanPoint(token, label, "TABLE").then(load).catch((er) => fail(er.message));
  };
  const download = async (p) => {
    const blob = await ownerApi.file(token, `/api/scan-points/${p.id}/qr.png`);
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = `qr-${p.label}.png`;
    a.click();
  };
  const printAll = async () => {
    const html = await ownerApi.file(token, "/api/scan-points/print-sheet");
    window.open(URL.createObjectURL(html), "_blank");
  };

  return (
    <div>
      <form onSubmit={add} className="mb-4 flex gap-2">
        <input name="label" placeholder="Table name (Table 5)" required className={`${input} flex-1`} />
        <button className={btn}>Add table</button>
      </form>
      {points.length > 0 && <button onClick={() => printAll().catch((e) => fail(e.message))} className={`${btn} mb-4`}>Print all QR codes</button>}
      <div className="grid gap-3 sm:grid-cols-2">
        {points.map((p) => (
          <div key={p.id} className={`${box} flex items-center gap-4`}>
            <QrImage token={token} id={p.id} />
            <div>
              <b>{p.label}</b>
              <p className="my-2 break-all text-xs text-espresso-soft">{p.url}</p>
              <button onClick={() => download(p).catch((e) => fail(e.message))} className={btn}>Download</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function StaffTab({ token, fail }) {
  const [members, setMembers] = useState([]);
  const load = () => ownerApi.staff(token).then(setMembers).catch((e) => fail(e.message));
  useEffect(() => { load(); }, [token]); // eslint-disable-line react-hooks/exhaustive-deps

  const add = (e) => {
    e.preventDefault();
    const f = new FormData(e.target);
    ownerApi.addStaff(token, f.get("email"), f.get("password"))
      .then(() => { e.target.reset(); load(); })
      .catch((er) => fail(er.message));
  };

  return (
    <div>
      <form onSubmit={add} className="mb-4 flex flex-wrap gap-2">
        <input name="email" type="email" placeholder="Staff email" required className={`${input} flex-1`} />
        <input name="password" type="password" placeholder="Password (10+ characters)" minLength={10} required className={`${input} flex-1`} />
        <button className={btn}>Add staff</button>
      </form>
      <div className={box}>
        {members.map((m) => (
          <div key={m.id} className="flex items-center justify-between border-b py-2 last:border-0">
            <span>{m.email} <span className="text-sm text-espresso-soft">({m.role})</span></span>
            {m.role === "STAFF" && (
              <button onClick={() => window.confirm(`Remove ${m.email}?`) && ownerApi.removeStaff(token, m.id).then(load).catch((e) => fail(e.message))} className={btn}>Remove</button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

const TABS = { Menu: MenuTab, "Tables and QR": TablesTab, Staff: StaffTab };

export default function OwnerPage() {
  const [token, setToken] = useState(sessionStorage.getItem("qo_owner_token") || "");
  const [tab, setTab] = useState("Menu");
  const [error, setError] = useState("");

  const logout = () => { sessionStorage.removeItem("qo_owner_token"); setToken(""); };

  const login = async (e) => {
    e.preventDefault();
    const f = new FormData(e.target);
    try {
      const res = await orderApi.login(f.get("email"), f.get("password"));
      sessionStorage.setItem("qo_owner_token", res.accessToken);
      setToken(res.accessToken);
      setError("");
    } catch (err) {
      setError(err.message);
    }
  };

  if (!token) {
    return (
      <form onSubmit={login} className="mx-auto mt-24 max-w-sm space-y-3 rounded-xl bg-paper p-6 shadow">
        <h1 className="font-display text-2xl">Owner</h1>
        <input name="email" type="email" placeholder="Email" required className="w-full rounded border p-2" />
        <input name="password" type="password" placeholder="Password" required className="w-full rounded border p-2" />
        {error && <p className="text-sm text-red-700">{error}</p>}
        <button className="w-full rounded bg-espresso py-2 text-cream">Login</button>
      </form>
    );
  }

  const Tab = TABS[tab];
  return (
    <div className="mx-auto max-w-3xl p-4 font-sans text-espresso">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="font-display text-2xl">My business</h1>
        <button onClick={logout} className="text-sm underline">Logout</button>
      </div>
      <div className="mb-4 flex gap-2">
        {Object.keys(TABS).map((name) => (
          <button key={name} onClick={() => { setTab(name); setError(""); }}
            className={`rounded px-3 py-1 ${tab === name ? "bg-espresso text-cream" : "bg-cream-dim"}`}>{name}</button>
        ))}
      </div>
      {error && <p className="mb-3 text-sm text-red-700">{error}</p>}
      <Tab token={token} fail={setError} />
    </div>
  );
}
