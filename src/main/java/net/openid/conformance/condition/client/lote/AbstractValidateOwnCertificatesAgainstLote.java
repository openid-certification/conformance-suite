package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.X509CertificateUtil;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pre-flight interoperability checks: the certificate chains the suite itself will present must
 * chain to a trusted entity in the configured list of trusted entities for the role, or the
 * implementation under test that trusts the list is expected to reject them. A failure reports
 * on the suite's listing with the scheme operator, or on the test configuration, not on the
 * implementation under test, so these conditions are expected to be called as a WARNING.
 */
public abstract class AbstractValidateOwnCertificatesAgainstLote extends AbstractCondition {

	/**
	 * @param chains the suite's own chains, leaf first, keyed by what they are
	 * @param what the suite's role, for the messages ("request signing", "credential signing", ...)
	 * @param slots the lists whose issuance services must cover the chains
	 */
	protected Environment check(Environment env, Map<String, List<X509Certificate>> chains, String what, LoteSlot... slots) {
		LoteTrustAnchors.TrustAnchorSet anchors = LoteTrustAnchors.fromLists(env, LoteSlot.ISSUANCE_SUFFIX, List.of(slots));
		List<String> slotLabels = new ArrayList<>();
		for (LoteSlot slot : slots) {
			if (env.containsObject(slot.envKey)) {
				slotLabels.add("'" + slot.fieldLabel + "'");
			}
		}

		JsonObject results = new JsonObject();
		List<String> uncovered = new ArrayList<>();
		for (Map.Entry<String, List<X509Certificate>> entry : chains.entrySet()) {
			JsonObject result = new JsonObject();
			result.addProperty("leaf_subject", entry.getValue().get(0).getSubjectX500Principal().getName());
			try {
				X509Certificate matched = X509CertificateUtil.validateX5cCertificateChain(entry.getValue(), anchors.certs());
				result.addProperty("trusted", matched != null);
				result.addProperty("matched_trust_anchor", anchors.sourceOf(matched));
				if (matched == null) {
					uncovered.add(entry.getKey());
				}
			} catch (X509CertificateUtil.X5cCertificateChainException e) {
				result.addProperty("trusted", false);
				result.addProperty("error", e.getMessage());
				uncovered.add(entry.getKey());
			}
			results.add(entry.getKey(), result);
		}

		if (!uncovered.isEmpty()) {
			throw error("The suite's own " + what + " certificate does not chain to a trusted entity in the configured "
					+ String.join(" / ", slotLabels) + "; an implementation that trusts that list is expected to reject what the suite"
					+ " signs with it. Check the suite's listing with the scheme operator (its CA certificates are published at"
					+ " /trust-anchors), or the test configuration: " + String.join(", ", uncovered),
				args("results", results, "trust_anchors_in_list", anchors.sourcesJson()));
		}

		logSuccess("The suite's own " + what + " certificate chains to a trusted entity in the configured " + String.join(" / ", slotLabels),
			args("results", results));
		return env;
	}

	/**
	 * Whether none of {@code slots}' lists is configured, in which case there is nothing to check the
	 * suite's certificates against; logs that.
	 */
	protected boolean noListConfigured(Environment env, String what, LoteSlot... slots) {
		for (LoteSlot slot : slots) {
			if (env.containsObject(slot.envKey)) {
				return false;
			}
		}
		List<String> labels = new ArrayList<>();
		for (LoteSlot slot : slots) {
			labels.add("'" + slot.fieldLabel + "'");
		}
		log("No " + String.join(" or ", labels) + " is configured; nothing to check the suite's " + what + " certificate against");
		return true;
	}

	/** The x5c chain of the signing key of a configured JWKS, or null when the JWKS is absent. */
	protected List<X509Certificate> chainOfConfiguredJwks(JsonElement jwksEl, String fieldLabel) {
		if (jwksEl == null || jwksEl.isJsonNull()) {
			return null;
		}
		if (!jwksEl.isJsonObject()) {
			throw error("The " + fieldLabel + " of the test configuration is not a JSON object");
		}
		JsonObject jwks = jwksEl.getAsJsonObject().has("keys")
			? jwksEl.getAsJsonObject() : JWKUtil.createJwksObjectFromJwkObjects(jwksEl.getAsJsonObject());
		try {
			return JWKUtil.getSigningKeyCertificateChain(jwks);
		} catch (JWKUtil.SigningKeyChainException e) {
			throw error("The " + fieldLabel + " of the test configuration " + e.getMessage()
				+ ", so there is no certificate chain to evaluate against the list of trusted entities", e);
		}
	}
}
