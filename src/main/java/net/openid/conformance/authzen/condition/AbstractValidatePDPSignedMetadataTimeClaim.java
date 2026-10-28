package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Reads an RFC 7519 NumericDate claim from the PDP signed_metadata JWT.
 */
public abstract class AbstractValidatePDPSignedMetadataTimeClaim extends AbstractCondition {

	protected static final long CLOCK_SKEW_SECONDS = 5 * 60;

	/**
	 * Returns the claim in seconds since the epoch, or null when the JWT does not carry it.
	 * {@link ExtractPDPSignedMetadata} rejects an exp, iat or nbf that is null or not a number, so
	 * a claim present here is one.
	 */
	protected Long getTimeClaim(Environment env, String claim) {
		JsonElement claimEl = env.getElementFromObject("pdp_signed_metadata", "claims." + claim);
		if (claimEl == null) {
			return null;
		}
		return OIDFJSON.getNumber(claimEl).longValue();
	}

	protected static long nowSeconds() {
		return System.currentTimeMillis() / 1000L;
	}
}
