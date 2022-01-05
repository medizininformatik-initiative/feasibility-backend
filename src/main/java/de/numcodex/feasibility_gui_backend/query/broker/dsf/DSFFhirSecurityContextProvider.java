package de.numcodex.feasibility_gui_backend.query.broker.dsf;

import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * An entity that can provide a security context for communicating with a FHIR server.
 */
public class DSFFhirSecurityContextProvider implements FhirSecurityContextProvider {

    private final String keyStoreFile;
    private final char[] keyStorePassword;
    private final String certificateFile;

    public DSFFhirSecurityContextProvider(String keyStoreFile, char[] keyStorePassword, String certificateFile) {
        this.keyStoreFile = keyStoreFile;
        this.keyStorePassword = keyStorePassword;
        this.certificateFile = certificateFile;
    }

    @Override
    public FhirSecurityContext provideSecurityContext() throws FhirSecurityContextProvisionException {
        try {
            Path localWebsocketKsFile = Paths.get(keyStoreFile);
            if (!Files.isReadable(localWebsocketKsFile)) {
                throw new IOException("Keystore file '" + localWebsocketKsFile + "' not readable");
            }
            KeyStore localKeyStore = CertificateReader.fromPkcs12(localWebsocketKsFile, keyStorePassword);
            KeyStore localTrustStore = CertificateHelper.extractTrust(localKeyStore);

            if (!Files.isReadable(Paths.get(certificateFile))) {
                throw new IOException("Certificate file '" + certificateFile + "' not readable");
            }
            FileInputStream inStream = new FileInputStream(certificateFile);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(inStream);

            localTrustStore.setCertificateEntry("zars", cert);

            return new FhirSecurityContext(localKeyStore, localTrustStore, keyStorePassword);
        } catch (Exception e) {
            throw new FhirSecurityContextProvisionException(e);
        }
    }
}
