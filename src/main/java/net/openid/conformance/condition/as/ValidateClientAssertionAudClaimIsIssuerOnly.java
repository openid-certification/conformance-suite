package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Checks that the authorization server's issuer identifier is the sole audience of a client
 * authentication assertion, as RFC 10070 section 4 requires of the client: either the issuer
 * as a string, or an array holding the issuer and nothing else.
 *
 * Which other audience values are acceptable is decided by ValidateClientAssertionClaims and
 * its subclasses; the caller of this condition chooses the severity for a client that still
 * sends one of them.
 */
public class ValidateClientAssertionAudClaimIsIssuerOnly extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "server", "client_assertion" })
	public Environment evaluate(Environment env) {
		String issuer = env.getString("server", "issuer");
		if (Strings.isNullOrEmpty(issuer)) {
			throw error("Couldn't find the issuer in the server configuration to check the client assertion 'aud' claim against",
				args("server", env.getObject("server")));
		}

		JsonElement aud = env.getElementFromObject("client_assertion", "claims.aud");
		if (aud == null || aud.isJsonNull()) {
			throw error("The client assertion has no 'aud' claim. It must contain the authorization server's issuer identifier as its only value.",
				args("expected", issuer));
		}

		if (OIDFJSON.isString(aud)) {
			if (!issuer.equals(OIDFJSON.getString(aud))) {
				throw error(notTheIssuer(), args("expected", issuer, "actual", aud));
			}
		} else if (aud.isJsonArray()) {
			JsonArray values = aud.getAsJsonArray();
			JsonPrimitive issuerValue = new JsonPrimitive(issuer);
			if (!values.contains(issuerValue)) {
				throw error(notTheIssuer(), args("expected", issuer, "actual", aud));
			}
			if (values.size() != 1) {
				throw error("The client assertion 'aud' claim contains the authorization server's issuer identifier together with other values. "
					+ "The issuer identifier must be the only value. " + consequence(),
					args("expected", issuer, "actual", aud));
			}
		} else {
			throw error("The client assertion 'aud' claim is neither a string nor an array of strings. "
				+ "It must contain the authorization server's issuer identifier as its only value.",
				args("expected", issuer, "actual", aud));
		}

		logSuccess("The authorization server's issuer identifier is the only value of the client assertion 'aud' claim",
			args("aud", aud));
		return env;
	}

	private String notTheIssuer() {
		return "The client assertion 'aud' claim is not the authorization server's issuer identifier. "
			+ "Endpoint URLs were permitted as audience values by earlier specifications, but the issuer identifier must now be the only value. "
			+ consequence();
	}

	private String consequence() {
		return "RFC 10070 requires authorization servers to reject a client authentication assertion whose audience is anything else, "
			+ "so this client will fail to authenticate at servers that implement it.";
	}
}
