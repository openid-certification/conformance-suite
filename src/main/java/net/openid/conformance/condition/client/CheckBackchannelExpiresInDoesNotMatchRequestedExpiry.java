package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CheckBackchannelExpiresInDoesNotMatchRequestedExpiry extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "backchannel_authentication_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonElement requestedExpiryElement = env.getElementFromObject("authorization_endpoint_request", "requested_expiry");
		if (requestedExpiryElement == null) {
			throw error("requested_expiry was not found in the authorization request");
		}

		JsonElement expiresInElement = env.getElementFromObject("backchannel_authentication_endpoint_response", "expires_in");
		if (expiresInElement == null || !expiresInElement.isJsonPrimitive() || !expiresInElement.getAsJsonPrimitive().isNumber()) {
			throw error("expires_in is missing or not a number in the backchannel response");
		}

		try {
			Number requestedExpiryNumber = OIDFJSON.forceConversionToNumber(requestedExpiryElement);
			int requestedExpiry = Integer.parseInt(requestedExpiryNumber.toString());
			int expiresIn = OIDFJSON.getInt(expiresInElement);

			if (requestedExpiry == expiresIn) {
				throw error("requested_expiry appears to affect backchannel expires_in. It should be ignored for Brazil CIBA", args(
					"requested_expiry", requestedExpiry,
					"expires_in", expiresIn
				));
			}

			logSuccess("Backchannel expires_in does not match requested_expiry", args(
				"requested_expiry", requestedExpiry,
				"expires_in", expiresIn
			));
			return env;

		} catch (OIDFJSON.ValueIsJsonNullException e) {
			throw error("requested_expiry must not be JSON null", args("requested_expiry", requestedExpiryElement));
		} catch (OIDFJSON.UnexpectedJsonTypeException | NumberFormatException e) {
			throw error("requested_expiry must be an integer or a string representing an integer", args("requested_expiry", requestedExpiryElement));
		}
	}
}
