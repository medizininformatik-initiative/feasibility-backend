package de.numcodex.feasibility_gui_backend.query.broker.dsf;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;

public class DSFFhirSecurityContextProviderTest {

    private static final char[] PASSWORD = "password".toCharArray();
    static private CertificateFactory cf;

    @BeforeAll
    static void setup() throws Exception {
        cf = CertificateFactory.getInstance("X.509");
    }

    @Test
    void trustStoreContainsCertificateFromPEM() throws Exception {
        DSFFhirSecurityContextProvider securityContextProvider = new DSFFhirSecurityContextProvider(
                getFilePath("test.p12"), PASSWORD, getFilePath("foo.pem"));

        FhirSecurityContext securityContext = securityContextProvider.provideSecurityContext();

        assertThat("trust store does not contain 'foo' certificate", getCertificateAlias(securityContext, "foo.pem"),
                not(emptyOrNullString()));
    }

    @Test
    void trustStoreContainsMultipleCertificatesFromPEM() throws Exception {
        DSFFhirSecurityContextProvider securityContextProvider = new DSFFhirSecurityContextProvider(
                getFilePath("test.p12"), PASSWORD, getFilePath("multiple.pem"));

        FhirSecurityContext securityContext = securityContextProvider.provideSecurityContext();

        assertThat("trust store does not contain 'foo' certificate", getCertificateAlias(securityContext, "foo.pem"),
                not(emptyOrNullString()));
        assertThat("trust store does not contain 'bar' certificate", getCertificateAlias(securityContext, "bar.pem"),
                not(emptyOrNullString()));
    }

    private String getCertificateAlias(FhirSecurityContext securityContext, String fileName) throws Exception {
        InputStream resource = DSFFhirSecurityContextProviderTest.class.getResourceAsStream(fileName);
        Certificate cert = cf.generateCertificate(resource);
        return securityContext.trustStore.getCertificateAlias(cert);
    }

    private String getFilePath(String filename) {
        return DSFFhirSecurityContextProviderTest.class.getResource(filename).getPath();
    }
}
