import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { clientPageUrl } from "../config/site";
import AdminLogin from "../components/admin/AdminLogin";
import RestaurantForm from "../components/admin/RestaurantForm";
import RestaurantList from "../components/admin/RestaurantList";
import QrPreview from "../components/admin/QrPreview";

export default function Admin() {
  const [authenticated, setAuthenticated] = useState(null); // null = checking
  const [restaurants, setRestaurants] = useState([]);
  const [editing, setEditing] = useState(null); // restaurant being edited, or null for create mode
  const [justCreated, setJustCreated] = useState(null);
  const [listError, setListError] = useState("");
  const [regenerating, setRegenerating] = useState(false);
  const [regenMessage, setRegenMessage] = useState("");

  useEffect(() => {
    const meta = document.createElement("meta");
    meta.name = "robots";
    meta.content = "noindex, nofollow";
    document.head.appendChild(meta);
    return () => document.head.removeChild(meta);
  }, []);

  useEffect(() => {
    api
      .me()
      .then((res) => setAuthenticated(res.authenticated))
      .catch(() => setAuthenticated(false));
  }, []);

  useEffect(() => {
    if (authenticated) refreshList();
  }, [authenticated]);

  function refreshList() {
    setListError("");
    api
      .list()
      .then(setRestaurants)
      .catch((err) => setListError(err.message));
  }

  function handleSaved(record) {
    const wasCreate = !editing;
    setEditing(null);
    refreshList();
    if (wasCreate) setJustCreated(record);
  }

  async function handleDelete(id) {
    try {
      await api.remove(id);
      refreshList();
    } catch (err) {
      setListError(err.message);
    }
  }

  async function handleLogout() {
    await api.logout();
    setAuthenticated(false);
  }

  async function handleRegenerate() {
    setRegenerating(true);
    setRegenMessage("");
    try {
      const data = await api.regenerateAll();
      setRegenMessage(`✔ ${data.count} page(s) régénérée(s)`);
      setTimeout(() => setRegenMessage(""), 4000);
    } catch (err) {
      setRegenMessage(`Erreur : ${err.message}`);
    } finally {
      setRegenerating(false);
    }
  }

  if (authenticated === null) {
    return <div className="min-h-screen bg-cream" />;
  }

  if (!authenticated) {
    return <AdminLogin onSuccess={() => setAuthenticated(true)} />;
  }

  const existingSlugs = restaurants.map((r) => r.slug);

  return (
    <div className="min-h-screen bg-cream px-5 py-10 sm:px-8">
      <div className="mx-auto max-w-3xl">
        <div className="flex items-center justify-between">
          <h1 className="font-display text-2xl font-semibold text-espresso">Admin — QlinkR</h1>
          <div className="flex items-center gap-4">
            <button
              type="button"
              onClick={handleRegenerate}
              disabled={regenerating}
              className="rounded-lg border border-wood-300 px-3 py-1.5 text-xs font-semibold text-espresso hover:bg-wood-100 disabled:opacity-50"
            >
              {regenerating ? "Régénération…" : "Régénérer les pages"}
            </button>
            <button type="button" onClick={handleLogout} className="text-sm font-medium text-espresso-soft hover:text-espresso">
              Se déconnecter
            </button>
          </div>
        </div>
        {regenMessage && (
          <p className={`mt-2 text-xs font-medium ${regenMessage.startsWith("✔") ? "text-green-700" : "text-red-600"}`}>
            {regenMessage}
          </p>
        )}

        {justCreated ? (
          <div className="mt-8 rounded-2xl border border-wood-300 bg-wood-50 p-6 sm:p-8">
            <p className="text-sm font-semibold text-wood-700 uppercase tracking-wide">Page créée</p>
            <h2 className="mt-1 font-display text-2xl font-semibold text-espresso">{justCreated.name}</h2>
            <a
              href={clientPageUrl(justCreated.slug)}
              target="_blank"
              rel="noopener noreferrer"
              className="mt-1 inline-block text-sm text-wood-700 hover:underline"
            >
              {clientPageUrl(justCreated.slug)}
            </a>

            <div className="mt-6">
              <QrPreview url={clientPageUrl(justCreated.slug)} filename={`${justCreated.slug}-qr.svg`} />
            </div>

            <button
              type="button"
              onClick={() => setJustCreated(null)}
              className="mt-6 rounded-lg border border-wood-300 px-4 py-2 text-sm font-semibold text-espresso hover:bg-wood-100"
            >
              Créer un autre établissement
            </button>
          </div>
        ) : (
          <div className="mt-8">
            <RestaurantForm
              key={editing?.id ?? "new"}
              existing={editing}
              existingSlugs={existingSlugs.filter((s) => s !== editing?.slug)}
              onSaved={handleSaved}
              onCancel={() => setEditing(null)}
            />
          </div>
        )}

        <div className="mt-10">
          <h2 className="text-sm font-semibold tracking-wide text-espresso-soft uppercase">
            Établissements ({restaurants.length})
          </h2>
          {listError && <p className="mt-2 text-sm text-red-600">{listError}</p>}
          <div className="mt-3">
            <RestaurantList
              restaurants={restaurants}
              onEdit={(r) => {
                setJustCreated(null);
                setEditing(r);
              }}
              onDelete={handleDelete}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
