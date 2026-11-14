package ma.qlinkr.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Name and description of an item in one language: one row of ITEM_I18N. */
@Embeddable
public record ItemText(
        @Column(name = "name", nullable = false, length = 100) String name,
        @Column(name = "description", length = 500) String description) {
}
