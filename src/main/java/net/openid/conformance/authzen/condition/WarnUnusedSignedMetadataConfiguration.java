package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

/**
 * Point out the signed metadata fields of the test configuration that the run cannot use: the
 * 'Signed Metadata Issuer' ({@code pdp.metadata_issuer}) and the 'PDP JWK Set' ({@code pdp.jwks}).
 *
 * <p>Both fields only mean anything when the metadata carries a {@code signed_metadata} JWT: the
 * issuer names the party expected to attest to it, and the JWK Set holds the keys that verify it.
 * When it does, {@link ValidatePDPMetadataIssuer}, {@link EnsurePDPJwksConfigured} and the JWK Set
 * checks validate the values; when it does not, the fields are ignored, and a tester who filled them
 * in was expecting signed metadata that never arrived. Saying so is the point of this condition: the
 * silence would otherwise look like success.
 *
 * <p>This is not a PDP conformance failure — {@code signed_metadata} is OPTIONAL (§9.1.3) — so the
 * caller invokes it as a WARNING.
 */
public class WarnUnusedSignedMetadataConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config"})
	public Environment evaluate(Environment env) {
		List<String> unused = new ArrayList<>();
		if (SignedMetadataIssuerConfig.isConfigured(env)) {
			unused.add("'Signed Metadata Issuer'");
		}
		JsonElement jwks = env.getElementFromObject("config", "pdp.jwks");
		if (isFilledIn(jwks)) {
			unused.add("'PDP JWK Set'");
		}
		if (unused.isEmpty()) {
			logSuccess("The PDP's discovery metadata contains no `signed_metadata`, and neither a 'Signed Metadata "
				+ "Issuer' nor a 'PDP JWK Set' is configured for one");
			return env;
		}

		throw error(String.join(" and ", unused) + (unused.size() == 1 ? " is" : " are") + " configured in the "
			+ "'AuthZEN' section of the test configuration, but the PDP's discovery metadata contains no "
			+ "`signed_metadata` for " + (unused.size() == 1 ? "it" : "them") + " to apply to, so "
			+ (unused.size() == 1 ? "the field has" : "the fields have") + " been ignored. Either the PDP is "
			+ "not publishing the signed metadata you expected, or " + (unused.size() == 1 ? "the field" : "the fields")
			+ " should be left blank.",
			args("unused_fields", unused, "metadata_issuer", SignedMetadataIssuerConfig.element(env), "pdp_jwks", jwks));
	}

	/**
	 * Absent, JSON null, the empty string and an empty object or array count as blank. Any other value
	 * counts as filled in, whatever its shape, since nothing has validated it on this path.
	 */
	private static boolean isFilledIn(JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return false;
		}
		if (element.isJsonObject()) {
			return !element.getAsJsonObject().isEmpty();
		}
		if (element.isJsonArray()) {
			return !element.getAsJsonArray().isEmpty();
		}
		if (element.getAsJsonPrimitive().isString()) {
			return !OIDFJSON.getString(element).isEmpty();
		}
		return true;
	}
}
