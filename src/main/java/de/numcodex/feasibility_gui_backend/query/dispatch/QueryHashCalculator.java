package de.numcodex.feasibility_gui_backend.query.dispatch;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Provides functionality for calculating the hash value of a serialized query body.
 */
@RequiredArgsConstructor
class QueryHashCalculator {

    @NonNull
    private MessageDigest hashFn;

    /**
     * Given a serialized query body returns its hash value in hex format.
     *
     * @param queryBody The serialized query whose hash value shall be calculated.
     * @return The calculated hash value in hex format.
     */
    String calculateSerializedQueryBodyHash(@NotNull  String queryBody) {
        if (queryBody == null) {
            throw new IllegalArgumentException("query body must not be null");
        }

        var hashByteRepresentation = hashFn.digest(queryBody.getBytes(StandardCharsets.UTF_8));
        return byteToHex(hashByteRepresentation);
    }

    private String byteToHex(byte[] hash) {
        var hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
