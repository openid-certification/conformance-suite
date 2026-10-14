package net.openid.conformance.condition.client.lote;

import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * The SD-JWT VC variant of {@link AbstractValidateOwnCredentialSigningCertificateAgainstLote}: the
 * configured 'Signing JWK', or the built-in signer when none is configured.
 */
public class ValidateOwnSdJwtSigningCertificateAgainstLote extends AbstractValidateOwnCredentialSigningCertificateAgainstLote {

	private static final String SIGNING_JWK_FIELD = "'Signing JWK' field in the 'Credential Issuer' section";

	@Override
	protected Map<String, List<X509Certificate>> signingChain(Environment env) {
		List<X509Certificate> configured = chainOfConfiguredJwks(env.getElementFromObject("config", "credential.signing_jwk"), SIGNING_JWK_FIELD);
		return configured == null
			? Map.of("built-in SD-JWT VC signer", TestKeysAndCerts.getSdJwtPidSigner().getJwk().getParsedX509CertChain())
			: Map.of(SIGNING_JWK_FIELD, configured);
	}
}
