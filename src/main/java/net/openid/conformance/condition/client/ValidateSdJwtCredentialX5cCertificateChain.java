package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.AbstractValidateX5cCertificateChain;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.Profile;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Validates the x5c certificate chain and credential signature for an SD-JWT VC credential.
 *
 * Per HAIP section 4.5.1 / 6.1.1:
 * - The x5c header MUST be present
 * - The leaf certificate MUST NOT be self-signed
 * - The trust anchor MUST NOT be included in the x5c chain
 * - The credential signature MUST verify using the leaf certificate
 */
public class ValidateSdJwtCredentialX5cCertificateChain extends AbstractValidateX5cCertificateChain {

	@Override
	@PreEnvironment(required = {"sdjwt"})
	public Environment evaluate(Environment env) {

		JsonObject credentialHeader = env.getElementFromObject("sdjwt", "credential.header").getAsJsonObject();

		if (!credentialHeader.has("x5c")) {
			throw error("Credential MUST contain an x5c in the header", args("credential_header", credentialHeader));
		}

		List<String> x5c = OIDFJSON.convertJsonArrayToList(credentialHeader.getAsJsonArray("x5c"));
		if (x5c.isEmpty()) {
			throw error("x5c header claim MUST NOT be empty", args("credential_header", credentialHeader));
		}

		List<X509Certificate> certs = parseX5cCertificatesFromStrings(x5c);

		String trustAnchorPem = env.getString("credential_trust_anchor_pem");
		X509Certificate trustAnchor = trustAnchorPem != null ? X509CertUtils.parse(trustAnchorPem) : null;
		validateX5cCertificateChain(certs, trustAnchor, Profile.isHaip(env));

		String credentialJwt = env.getString("sdjwt", "credential.value");
		verifyJwtSignatureWithX5cLeafCert(credentialJwt, certs);

		logSuccess("Validated x5c certificate chain and credential signature",
			args("x5c", x5c,
				"leaf_cert_subject", certs.get(0).getSubjectX500Principal().getName(),
				"chain_length", certs.size()));

		return env;
	}
}
