package net.openid.conformance.oauth.statuslists;

import com.google.gson.JsonObject;

import java.time.Instant;

/**
 * Builds the claims of a Status List Token in JWT format (draft-ietf-oauth-status-list
 * section 5.1). The counterpart of {@link CwtStatusListTokenBuilder} for the JWT representation;
 * signing is left to the caller, since the emulated issuers sign with different keys.
 */
public final class JwtStatusListTokenClaimsBuilder {

	/** Media type of a Status List Token in JWT format. */
	public static final String CONTENT_TYPE = "application/statuslist+jwt";

	/** Value of the {@code typ} header of a Status List Token in JWT format. */
	public static final String TYP = "statuslist+jwt";

	private JwtStatusListTokenClaimsBuilder() {
		// utility class
	}

	/**
	 * @param uri the URI the status list is published at, used as the {@code sub} claim
	 * @param iat the issuance time
	 * @param exp the expiry time
	 * @param ttlSeconds the {@code ttl} claim
	 * @param bits bits per status list entry
	 * @param encodedStatusList the compressed, base64url encoded status list, as the {@code lst}
	 *   string
	 * @param aggregationUri the optional aggregation_uri element of the status_list claim
	 *   (draft-ietf-oauth-status-list section 4.2), or null to omit it
	 */
	public static JsonObject build(String uri, Instant iat, Instant exp, long ttlSeconds, int bits,
			String encodedStatusList, String aggregationUri) {

		JsonObject statusList = new JsonObject();
		statusList.addProperty("bits", bits);
		statusList.addProperty("lst", encodedStatusList);
		if (aggregationUri != null) {
			statusList.addProperty("aggregation_uri", aggregationUri);
		}

		JsonObject claims = new JsonObject();
		claims.addProperty("sub", uri);
		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("exp", exp.getEpochSecond());
		claims.addProperty("ttl", ttlSeconds);
		claims.add("status_list", statusList);
		return claims;
	}
}
