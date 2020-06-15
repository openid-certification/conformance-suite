package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;
import java.util.Date;

public class ValidateLogoutTokenClaims extends AbstractCondition {

	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = { "logout_token", "server", "client" } )
	public Environment evaluate(Environment env) {

		String clientId = env.getString("client", "client_id"); // to check the audience
		String issuer = env.getString("server", "issuer"); // to validate the issuer
		Instant now = Instant.now(); // to check timestamps

		if (Strings.isNullOrEmpty(clientId)
			|| Strings.isNullOrEmpty(issuer)) {
			throw error("Couldn't find values to test token against");
		}

		// checks are in order listed in https://openid.net/specs/openid-connect-backchannel-1_0.html#LogoutToken
		// iss
		//REQUIRED. Issuer Identifier, as specified in Section 2 of [OpenID.Core].
		String logoutTokenIss = env.getString("logout_token", "claims.iss");
		if (logoutTokenIss == null) {
			throw error("'iss' claim missing");
		}
		if (!issuer.equals(logoutTokenIss)) {
			throw error("Issuer mismatch", args("expected", issuer, "actual", logoutTokenIss));
		}

		//sub
		//OPTIONAL. Subject Identifier, as specified in Section 2 of [OpenID.Core].
		// checked in CheckIdTokenSubMatchesLogoutToken & CheckLogoutTokenHasSubOrSid

		//aud
		//REQUIRED. Audience(s), as specified in Section 2 of [OpenID.Core].
		JsonElement aud = env.getElementFromObject("logout_token", "claims.aud");
		if (aud == null) {
			throw error("'aud' claim missing");
		}
		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(clientId))) {
				throw error("'aud' array does not contain our client id", args("expected", clientId, "actual", aud));
			}
		} else {
			if (!clientId.equals(OIDFJSON.getString(aud))) {
				throw error("'aud' is not our client id", args("expected", clientId, "actual", aud));
			}
		}

		//iat
		//REQUIRED. Issued at time, as specified in Section 2 of [OpenID.Core].
		Long iat = env.getLong("logout_token", "claims.iat");
		if (iat == null) {
			throw error("'iat' claim missing");
		} else {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
				throw error("Token 'iat' in the future", args("issued-at", new Date(iat * 1000L), "now", now));
			}
		}

		//jti
		//REQUIRED. Unique identifier for the token, as specified in Section 9 of [OpenID.Core].
		String jti = env.getString("logout_token", "claims.jti");
		if (jti == null) {
			throw error("jti missing");
		}

		//events
		//REQUIRED. Claim whose value is a JSON object containing the member name http://schemas.openid.net/event/backchannel-logout. This declares that the JWT is a Logout Token. The corresponding member value MUST be a JSON object and SHOULD be the empty JSON object {}.
		JsonElement events = env.getElementFromObject("logout_token", "claims.events");
		if (events == null) {
			throw error("'events' claim missing");
		}
		if (!events.isJsonObject()) {
			throw error("'events' claim is not a json object");
		}
		JsonObject eventsObj = events.getAsJsonObject();
		if (eventsObj.size() != 1) {
			throw error("'events' object does not contain exactly 1 entry", eventsObj);
		}
		JsonElement eventsValueElement = eventsObj.get("http://schemas.openid.net/event/backchannel-logout");
		if (eventsValueElement == null) {
			throw error("http://schemas.openid.net/event/backchannel-logout entry is missing from 'events' claim", eventsObj);
		}
		if (!eventsValueElement.isJsonObject()) {
			throw error("http://schemas.openid.net/event/backchannel-logout is not a json object");
		}
		JsonObject eventsValue = (JsonObject) eventsValueElement;
		if (eventsValue.size() != 0) {
			throw error("http://schemas.openid.net/event/backchannel-logout is not an empty object", eventsObj);
		}

		//sid
		//OPTIONAL. Session ID - String identifier for a Session. This represents a Session of a User Agent or device for a logged-in End-User at an RP. Different sid values are used to identify distinct sessions at an OP. The sid value need only be unique in the context of a particular issuer. Its contents are opaque to the RP. Its syntax is the same as an OAuth 2.0 Client Identifier.
		// Checked in CheckIdTokenSidMatchesLogoutToken & CheckLogoutTokenHasSubOrSid

		// nonce checked in CheckLogoutTokenNoNonce

		logSuccess("logout token iss, aud, iat, jti and events claims passed validation checks");
		return env;

	}

}
