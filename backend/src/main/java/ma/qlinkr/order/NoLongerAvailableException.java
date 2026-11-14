package ma.qlinkr.order;

import java.util.List;

/** One or more items ran out (or were switched off) between the scan and the order. */
public class NoLongerAvailableException extends RuntimeException {

    private final List<Long> itemIds;

    public NoLongerAvailableException(List<Long> itemIds) {
        super("No longer available: " + itemIds);
        this.itemIds = List.copyOf(itemIds);
    }

    public List<Long> itemIds() {
        return itemIds;
    }
}
