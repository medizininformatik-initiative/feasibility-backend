package de.numcodex.feasibility_gui_backend.query.broker.dsf;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Wrapper for different DSF query library information separated by different mime types.
 */
final class DSFQueryData {
    private final Map<String, String> contentByType;

    public DSFQueryData() {
        contentByType = new HashMap<>();
    }

    public void addQueryContent(String mediaType, String content) {
        Objects.requireNonNull(mediaType);
        Objects.requireNonNull(content);
        contentByType.put(mediaType, content);
    }

    public Map<String, String> getContentByType() {
        return contentByType;
    }
}
