package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kotlinx.io.bytestring.ByteString;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.multipaz.crypto.X509Cert;
import org.multipaz.mdoc.rical.RicalCertificateInfo;
import org.multipaz.mdoc.rical.SignedRical;
import org.multipaz.trustmanagement.TrustResult;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pre-flight interoperability check for the wallet tests: validates that the certificate chains
 * of the suite's own configured request signing keys (the x5c in 'JWKS', and in the second
 * client's JWKS when present) chain to a reader CA certificate listed in the configured RICAL
 * (ISO/IEC 18013-5 second edition draft Annex F.3.2.6). A wallet that trusts this RICAL is
 * expected to reject the suite's signed requests if the chain is not covered, so a failure here
 * reports a problem with the test configuration or the RICAL registration, not with the wallet
 * under test — this condition is expected to be called as a WARNING.
 */
public class ValidateConfiguredClientCertificatesAgainstRical extends AbstractRicalCondition {

	@Override
	@PreEnvironment(required = { "client_jwks", "rical" })
	public Environment evaluate(Environment env) {

		Map<String, List<X509Cert>> chains = new LinkedHashMap<>();
		chains.put("JWKS", x5cChainFromJwks(env.getObject("client_jwks"), "JWKS"));
		JsonObject client2Jwks = env.getObject("client2_jwks");
		if (client2Jwks != null) {
			chains.put("Second client JWKS", x5cChainFromJwks(client2Jwks, "Second client JWKS"));
		}

		SignedRical signedRical;
		try {
			verifyRicalCoseSignature(getRicalCoseSign1(getRicalBytes(env)));
			signedRical = parseSignedRicalLenient(getRicalBytes(env)).signedRical;
		} catch (Exception e) {
			throw error("The configured RICAL could not be parsed or its COSE signature does not verify, so the suite's request signing certificate chains cannot be evaluated against it", e);
		}
		String ricalProvider = signedRical.getRical().getProvider();

		JsonObject results = new JsonObject();
		List<String> untrusted = new ArrayList<>();
		for (Map.Entry<String, List<X509Cert>> entry : chains.entrySet()) {
			TrustResult trustResult = verifyChainAgainstRical(signedRical, entry.getValue());
			JsonObject result = new JsonObject();
			result.addProperty("leaf_subject", entry.getValue().get(0).getSubject().getName());
			result.addProperty("trusted", trustResult.isTrusted());
			if (trustResult.isTrusted()) {
				RicalCertificateInfo matched = findFirstMatchingRicalEntry(signedRical, entry.getValue());
				if (matched != null) {
					result.addProperty("matched_rical_entry_subject", matched.getCertificate().getSubject().getName());
					result.addProperty("trust_constraints_on_matched_entry",
						matched.getTrustConstraints() == null ? 0 : matched.getTrustConstraints().size());
				}
			} else {
				if (trustResult.getError() != null) {
					result.addProperty("error", trustResult.getError().getMessage());
				}
				untrusted.add(entry.getKey());
			}
			results.add(entry.getKey(), result);
		}

		if (!untrusted.isEmpty()) {
			throw error("The suite's configured request signing certificate does not chain to a reader CA certificate in the configured RICAL; a wallet that trusts this RICAL is expected to reject the suite's signed requests. Check the '"
					+ String.join("' and '", untrusted) + "' field in the 'Client' section of the test configuration, or the reader CA's registration with the RICAL provider.",
				args("results", results, "rical_provider", ricalProvider));
		}

		logSuccess("The suite's configured request signing certificate chains validate against reader CA certificates listed in the RICAL",
			args("results", results, "rical_provider", ricalProvider));

		return env;
	}

	/** Extracts the first signing key's x5c chain from a JWKS as multipaz certificates. */
	private List<X509Cert> x5cChainFromJwks(JsonObject jwks, String fieldLabel) {
		JsonElement keysEl = jwks.get("keys");
		if (keysEl == null || !keysEl.isJsonArray() || keysEl.getAsJsonArray().isEmpty()) {
			throw error("The '" + fieldLabel + "' field in the 'Client' section of the test configuration contains no keys");
		}
		// the request signing key is the first signing-use key with an x5c, matching how the
		// request object signer picks its key
		JsonObject signingKey = null;
		for (JsonElement keyEl : keysEl.getAsJsonArray()) {
			JsonObject key = keyEl.getAsJsonObject();
			if (key.has("x5c") && (!key.has("use") || "sig".equals(OIDFJSON.getString(key.get("use"))))) {
				signingKey = key;
				break;
			}
		}
		if (signingKey == null) {
			throw error("No key with an 'x5c' certificate chain found in the '" + fieldLabel
				+ "' field in the 'Client' section of the test configuration, so there is no certificate to evaluate against the RICAL");
		}
		List<X509Cert> chain = new ArrayList<>();
		for (JsonElement certEl : signingKey.getAsJsonObject().get("x5c").getAsJsonArray()) {
			try {
				byte[] der = Base64.getDecoder().decode(OIDFJSON.getString(certEl));
				chain.add(new X509Cert(new ByteString(der, 0, der.length)));
			} catch (Exception e) {
				throw error("Failed to parse a certificate in the x5c of the '" + fieldLabel
					+ "' field in the 'Client' section of the test configuration", e);
			}
		}
		return chain;
	}
}
