package de.numcodex.feasibility_gui_backend.query.obfuscation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("query")
@Tag("obfuscation")
public class QueryResultObfuscationTest {

    public static QueryResultObfuscator queryResultObfuscator;

    @BeforeAll
    public static void setUp() throws NoSuchAlgorithmException {
        var hashFn = MessageDigest.getInstance("SHA3-256");
        queryResultObfuscator = new QueryResultObfuscator(hashFn);
    }

    @Test
    public void testTokenizeSiteName_NullResultThrows() {
        assertThrows(IllegalArgumentException.class, () -> queryResultObfuscator.tokenizeSiteName(null));
    }
}
