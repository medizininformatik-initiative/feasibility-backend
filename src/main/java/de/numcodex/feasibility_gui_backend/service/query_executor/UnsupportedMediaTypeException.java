package de.numcodex.feasibility_gui_backend.service.query_executor;

import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Indicates that a media type for a query is not supported.
 */
public class UnsupportedMediaTypeException extends Exception {
    public UnsupportedMediaTypeException(String mediaType, List<String> supportedMediaTypes) {
        super("Media type '" + mediaType + "' is not supported. The following media types are supported: "
                + StringUtils.join(supportedMediaTypes, ", "));
    }
}
