package net.openid.conformance.condition.client.lote;

import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.multipaz.crypto.X509CertJvmKt;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/** The mdoc variant of {@link AbstractValidateOwnCredentialSigningCertificateAgainstLote}: the suite's document signer. */
public class ValidateOwnMdocSigningCertificateAgainstLote extends AbstractValidateOwnCredentialSigningCertificateAgainstLote {

	@Override
	protected Map<String, List<X509Certificate>> signingChain(Environment env) {
		return Map.of("built-in mdoc document signer",
			List.of(X509CertJvmKt.getJavaX509Certificate(TestKeysAndCerts.getDocumentSignerCert())));
	}
}
