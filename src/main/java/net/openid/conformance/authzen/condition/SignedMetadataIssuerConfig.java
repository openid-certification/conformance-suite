package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * How the optional 'Signed Metadata Issuer' ({@code pdp.metadata_issuer}) test configuration field is
 * read, in one place.
 *
 * <p>Three conditions have to agree on what "configured" means, and they disagree in visible ways if
 * they drift: {@link ValidatePDPSignedMetadataIss} decides whether to compare {@code iss} against it or
 * against the 'PDP Identifier', {@code ValidatePDPMetadataIssuer} validates the value, and
 * {@code WarnUnusedSignedMetadataIssuer} reports one the run cannot use. An empty string must behave
 * exactly like an absent field in all three, so that rule lives here rather than being spelled out
 * three times.
 */
final class SignedMetadataIssuerConfig {

	private SignedMetadataIssuerConfig() {
	}

	/** The raw configured value, whatever its JSON type; null when the field is absent. */
	static JsonElement element(Environment env) {
		return env.getElementFromObject("config", "pdp.metadata_issuer");
	}

	/**
	 * Did the tester supply a 'Signed Metadata Issuer'?
	 *
	 * <p>Absent, JSON null and the empty string all count as "not configured". A value of the wrong
	 * JSON type counts as configured, so that it gets reported rather than silently treated as absent
	 * — and so that callers can ask this question without {@code getString} throwing on it.
	 */
	static boolean isConfigured(Environment env) {
		JsonElement element = element(env);
		if (element == null || element.isJsonNull()) {
			return false;
		}
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
			return true;
		}
		return !OIDFJSON.getString(element).isEmpty();
	}

	/**
	 * The configured issuer as a string, or null when it is not configured or is not a string.
	 * {@code ValidatePDPMetadataIssuer} has already rejected the non-string case wherever this is
	 * called from, so a null return means the 'PDP Identifier' is the issuer to expect.
	 */
	static String value(Environment env) {
		JsonElement element = element(env);
		if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
			return null;
		}
		String issuer = OIDFJSON.getString(element);
		return issuer.isEmpty() ? null : issuer;
	}
}
