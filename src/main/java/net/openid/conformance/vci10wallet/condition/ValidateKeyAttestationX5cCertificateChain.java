package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.AbstractValidateX5cCertificateChain;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Validates the x5c certificate chain in a key attestation JWT and verifies the JWT
 * signature against the leaf certificate's public key.
 *
 * Per HAIP §4.5.1 / RFC 7515 §4.1.6, when x5c is present it carries the public key
 * used to verify the JWT signature. Combining chain validation and signature verification
 * in one condition prevents the "split key" gap where the JWT was signed by one key but
 * the x5c chain happened to validate independently.
 *
 * Skips silently when x5c is absent.
 */
public class ValidateKeyAttestationX5cCertificateChain extends AbstractValidateX5cCertificateChain {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject keyAttestationJwt = env.getElementFromObject("vci", "key_attestation_jwt").getAsJsonObject();
		JsonObject header = keyAttestationJwt.getAsJsonObject("header");

		JsonElement x5cEl = header.get("x5c");
		if (x5cEl == null || !x5cEl.isJsonArray() || x5cEl.getAsJsonArray().isEmpty()) {
			log("No x5c claim in key attestation JWT header — skipping chain validation");
			return env;
		}

		String rawJwt = OIDFJSON.getString(keyAttestationJwt.get("value"));

		try {
			List<String> x5c = OIDFJSON.convertJsonArrayToList(x5cEl.getAsJsonArray());
			List<X509Certificate> certs = parseX5cCertificatesFromStrings(x5c);

			String trustAnchorPem = env.getString("vci", "key_attestation_trust_anchor_pem");
			X509Certificate trustAnchorCert = trustAnchorPem != null ? X509CertUtils.parse(trustAnchorPem) : null;

			validateX5cCertificateChain(certs, trustAnchorCert);
			verifyJwtSignatureWithX5cLeafCert(rawJwt, certs);

			logSuccess("Validated key attestation x5c certificate chain and signature",
				args("x5c", x5c,
					"leaf_cert_subject", certs.get(0).getSubjectX500Principal().getName(),
					"chain_length", certs.size()));

			return env;
		} catch (ConditionError e) {
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF,
				"Key attestation x5c certificate chain validation failed");
			throw e;
		}
	}
}
