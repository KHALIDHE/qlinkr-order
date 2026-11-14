import { useState } from "react";
import { api } from "../../lib/api";

export default function AdminLogin({ onSuccess }) {
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await api.login(password);
      onSuccess();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-cream px-6">
      <form onSubmit={handleSubmit} className="w-full max-w-sm rounded-2xl border border-wood-200 bg-paper p-8 shadow-soft">
        <h1 className="font-display text-2xl font-semibold text-espresso">Admin</h1>
        <p className="mt-1 text-sm text-espresso-soft">Accès réservé.</p>

        <label className="mt-6 block text-sm font-medium text-espresso" htmlFor="admin-password">
          Mot de passe
        </label>
        <input
          id="admin-password"
          type="password"
          autoFocus
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="mt-2 w-full rounded-lg border border-wood-300 bg-white px-3.5 py-2.5 text-sm text-espresso outline-none focus:border-wood-500"
        />

        {error && <p className="mt-3 text-sm text-red-600">{error}</p>}

        <button
          type="submit"
          disabled={loading || !password}
          className="mt-6 w-full rounded-lg bg-wood-800 px-4 py-2.5 text-sm font-semibold text-cream transition-colors hover:bg-wood-900 disabled:opacity-50"
        >
          {loading ? "Connexion…" : "Se connecter"}
        </button>
      </form>
    </div>
  );
}
