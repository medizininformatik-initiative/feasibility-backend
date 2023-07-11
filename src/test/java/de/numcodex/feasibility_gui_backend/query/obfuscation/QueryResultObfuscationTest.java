package de.numcodex.feasibility_gui_backend.query.obfuscation;

import com.google.common.hash.Hashing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("query")
@Tag("obfuscation")
public class QueryResultObfuscationTest {

    public static QueryResultObfuscator queryResultObfuscator;

    @BeforeAll
    public static void setUp() throws NoSuchAlgorithmException {
        queryResultObfuscator = new QueryResultObfuscator(Hashing.sha256());
    }

    @Test
    public void testTokenizeSiteName_NullResultThrows() {
        assertThrows(IllegalArgumentException.class, () -> queryResultObfuscator.tokenizeSiteName(null, null));
    }
}
