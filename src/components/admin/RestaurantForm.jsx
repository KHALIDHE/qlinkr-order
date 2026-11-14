import { useState } from "react";
import { api } from "../../lib/api";
import { slugify } from "../../lib/slugify";
import { ICON_OPTIONS } from "../../lib/icons";
import { THEME_OPTIONS } from "../../lib/themes";

const MIN_LINKS = 1;
const MAX_LINKS = 7;
const SLUG_RE = /^[a-z0-9]+(-[a-z0-9]+)*$/;

function emptyLink() {
  return { label: "", url: "", icon: "generic" };
}

export default function RestaurantForm({ existing, existingSlugs, onSaved, onCancel }) {
  const isEdit = Boolean(existing);

  const [name, setName] = useState(existing?.name ?? "");
  const [slug, setSlug] = useState(existing?.slug ?? "");
  const [slugTouched, setSlugTouched] = useState(isEdit);
  const [links, setLinks] = useState(existing?.links?.length ? existing.links.map((l) => ({ ...l })) : [emptyLink()]);
  const [theme, setTheme] = useState(existing?.theme ?? "simple");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function handleNameChange(value) {
    setName(value);
    if (!isEdit && !slugTouched) setSlug(slugify(value));
  }

  function handleSlugChange(value) {
    setSlugTouched(true);
    setSlug(value.toLowerCase().replace(/[^a-z0-9-]/g, ""));
  }

  function addLink() {
    if (links.length >= MAX_LINKS) return;
    setLinks([...links, emptyLink()]);
  }

  function removeLink(index) {
    if (links.length <= MIN_LINKS) return;
    setLinks(links.filter((_, i) => i !== index));
  }

  function updateLink(index, field, value) {
    setLinks(links.map((l, i) => (i === index ? { ...l, [field]: value } : l)));
  }

  const nameValid = name.trim().length > 0;
  const slugValid = SLUG_RE.test(slug);
  const slugConflict = !isEdit && slug.length > 0 && existingSlugs.includes(slug);
  const linksValid = links.every((l) => l.label.trim() && l.url.trim());
  const canSubmit = nameValid && linksValid && (isEdit || (slugValid && !slugConflict)) && !submitting;

  async function handleSubmit(e) {
    e.preventDefault();
    if (!canSubmit) return;
    setError("");
    setSubmitting(true);
    try {
      const payload = {
        name: name.trim(),
        links: links.map((l) => ({ ...l, label: l.label.trim(), url: l.url.trim() })),
        theme,
      };
      const record = isEdit ? await api.update(existing.id, payload) : await api.create({ ...payload, slug });
      onSaved(record);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="rounded-2xl border border-wood-200 bg-paper p-6 shadow-soft sm:p-8">
      <h2 className="font-display text-xl font-semibold text-espresso">
        {isEdit ? `Modifier « ${existing.name} »` : "Nouvel établissement"}
      </h2>

      <div className="mt-6 grid gap-5 sm:grid-cols-2">
        <div>
          <label className="block text-sm font-medium text-espresso" htmlFor="name">
            Nom de l'établissement
          </label>
          <input
            id="name"
            value={name}
            onChange={(e) => handleNameChange(e.target.value)}
            placeholder="Le Petit Bistro"
            className="mt-1.5 w-full rounded-lg border border-wood-300 bg-white px-3.5 py-2.5 text-sm outline-none focus:border-wood-500"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-espresso" htmlFor="slug">
            URL publique
          </label>
          <div className="mt-1.5 flex items-center rounded-lg border border-wood-300 bg-white px-3.5 py-2.5 text-sm">
            <span className="text-espresso-soft/60 select-none">/r/</span>
            <input
              id="slug"
              value={slug}
              disabled={isEdit}
              onChange={(e) => handleSlugChange(e.target.value)}
              placeholder="le-petit-bistro"
              className="w-full outline-none disabled:text-espresso-soft/60"
            />
          </div>
          {isEdit ? (
            <p className="mt-1.5 text-xs text-espresso-soft/70">Verrouillé — déjà gravé sur un QR code.</p>
          ) : slugConflict ? (
            <p className="mt-1.5 text-xs text-red-600">Cette URL est déjà utilisée.</p>
          ) : slug && !slugValid ? (
            <p className="mt-1.5 text-xs text-red-600">Lettres minuscules, chiffres et tirets uniquement.</p>
          ) : null}
        </div>
      </div>

      <div className="mt-7">
        <label className="block text-sm font-medium text-espresso">Style de la page</label>
        <p className="mt-0.5 text-xs text-espresso-soft/70">
          Choisissez le thème qui correspond le mieux au type d'établissement — change juste les couleurs, pas le contenu. Modifiable à tout moment, sans impact sur le QR code.
        </p>
        <div className="mt-3 flex flex-wrap gap-2">
          {THEME_OPTIONS.map((opt) => {
            const selected = theme === opt.value;
            return (
              <button
                key={opt.value}
                type="button"
                onClick={() => setTheme(opt.value)}
                className={`flex items-center gap-2 rounded-full border px-3.5 py-2 text-sm font-medium transition-colors ${
                  selected
                    ? "border-wood-700 bg-wood-800 text-cream"
                    : "border-wood-300 bg-white text-espresso hover:bg-wood-50"
                }`}
              >
                <span
                  className="h-3 w-3 shrink-0 rounded-full border border-black/10"
                  style={{ backgroundColor: opt.swatch }}
                />
                {opt.label}
              </button>
            );
          })}
        </div>
      </div>

      <div className="mt-7">
        <div className="flex items-center justify-between">
          <label className="block text-sm font-medium text-espresso">Liens ({links.length}/{MAX_LINKS})</label>
          <button
            type="button"
            onClick={addLink}
            disabled={links.length >= MAX_LINKS}
            className="text-sm font-semibold text-wood-700 hover:text-wood-900 disabled:opacity-40"
          >
            + Ajouter un lien
          </button>
        </div>

        <div className="mt-3 space-y-3">
          {links.map((link, index) => (
            <div key={index} className="flex flex-wrap items-start gap-2 rounded-lg border border-wood-200 bg-white p-3 sm:flex-nowrap">
              <select
                value={link.icon}
                onChange={(e) => updateLink(index, "icon", e.target.value)}
                className="w-full shrink-0 rounded-md border border-wood-300 bg-white px-2 py-2 text-sm sm:w-36"
              >
                {ICON_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
              <input
                value={link.label}
                onChange={(e) => updateLink(index, "label", e.target.value)}
                placeholder="Libellé (ex: Menu)"
                maxLength={40}
                className="min-w-[9rem] flex-1 rounded-md border border-wood-300 bg-white px-3 py-2 text-sm outline-none focus:border-wood-500"
              />
              <input
                value={link.url}
                onChange={(e) => updateLink(index, "url", e.target.value)}
                placeholder="https://..."
                className="min-w-[12rem] flex-[2] rounded-md border border-wood-300 bg-white px-3 py-2 text-sm outline-none focus:border-wood-500"
              />
              <button
                type="button"
                onClick={() => removeLink(index)}
                disabled={links.length <= MIN_LINKS}
                aria-label="Supprimer ce lien"
                className="shrink-0 rounded-md px-2.5 py-2 text-espresso-soft hover:bg-wood-50 disabled:opacity-30"
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      </div>

      {error && <p className="mt-4 text-sm text-red-600">{error}</p>}

      <div className="mt-7 flex items-center gap-3">
        <button
          type="submit"
          disabled={!canSubmit}
          className="rounded-lg bg-wood-800 px-5 py-2.5 text-sm font-semibold text-cream transition-colors hover:bg-wood-900 disabled:opacity-40"
        >
          {submitting ? "Enregistrement…" : isEdit ? "Enregistrer les modifications" : "Créer la page"}
        </button>
        {isEdit && (
          <button type="button" onClick={onCancel} className="text-sm font-medium text-espresso-soft hover:text-espresso">
            Annuler
          </button>
        )}
      </div>
    </form>
  );
}
