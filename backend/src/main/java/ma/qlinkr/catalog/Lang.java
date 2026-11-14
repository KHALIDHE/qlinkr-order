package ma.qlinkr.catalog;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The three supported languages, and how to pick a text when one is missing. */
public final class Lang {

    public static final List<String> ALL = List.of("ar", "fr", "en");

    private Lang() {
    }

    public static boolean isSupported(String lang) {
        return lang != null && ALL.contains(lang);
    }

    public static String direction(String lang) {
        return "ar".equals(lang) ? "rtl" : "ltr";
    }

    /**
     * Requested language first, then the business's default language, then
     * any language that exists, in a fixed order. A customer never sees an
     * empty name because one translation is missing.
     */
    public static <T> Optional<T> pick(Map<String, T> texts, String requested, String businessDefault) {
        if (texts.containsKey(requested)) {
            return Optional.of(texts.get(requested));
        }
        if (texts.containsKey(businessDefault)) {
            return Optional.of(texts.get(businessDefault));
        }
        return ALL.stream().filter(texts::containsKey).findFirst().map(texts::get);
    }
}
