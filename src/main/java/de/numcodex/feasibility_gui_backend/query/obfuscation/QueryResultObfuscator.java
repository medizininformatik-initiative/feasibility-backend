package de.numcodex.feasibility_gui_backend.query.obfuscation;

import com.google.common.hash.HashFunction;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;

/**
 * Provides functionality for obfuscating a site using a tokenization technique.
 * This allows for replacing real data (site name) with some kind of placeholder without
 * this process being reversible.
 */
@RequiredArgsConstructor
public class QueryResultObfuscator {

    /**
     * Collision resistance is mainly based on the birthday paradox.
     * For a desired collision probability <50% this can be defined as:
     * <p><br>
     * p ~ n^2 / 2m
     * <p><br>
     * Where n is the number of items and m is the number of possibilities per item.
     * Since this obfuscator uses a hash function the output of this function will
     * always be in hex format. For a hex string the number of possibilities is
     * 16^c where c is the number of characters in this string, i.e. its length.
     * <p><br>
     * Assuming that we have around 2000 hospitals in Germany and all of them would
     * participate, then given the truncation output length below the collision
     * probability would be:
     * <p><br>
     * p ~ 2000^2 / 2 * 16^10<br>
     * p ~ 0.00000181...
     * <p><br>
     * This should be more than sufficient for what we are trying to accomplish here.
     */
    private static final int TRUNCATION_OUTPUT_LENGTH = 10;

    @NonNull
    private HashFunction hashFn;

    /**
     * Tokenizes the site name of the given query id and site name. That is, replaces it with a
     * placeholder in order to obfuscate it.
     *
     * @param queryId The query id for which the site name shall be tokenized.
     * @param siteName The site name that shall be tokenized
     * @return The tokenized site name.
     */
    public String tokenizeSiteName(Long queryId, @NotNull String siteName) {
        if (siteName == null) {
            throw new IllegalArgumentException("siteName must not be null");
        }

        var siteNameHash = hashFn.newHasher()
            .putLong(queryId)
            .putString(siteName, StandardCharsets.UTF_8)
            .hash()
            .toString();

        return truncateHashValue(siteNameHash);
    }

    private String truncateHashValue(String hashValue) {
        return hashValue.substring(0, TRUNCATION_OUTPUT_LENGTH);
    }
}
