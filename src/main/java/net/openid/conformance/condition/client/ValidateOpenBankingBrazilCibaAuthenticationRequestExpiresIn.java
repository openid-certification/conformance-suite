package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.fapiciba.OpenBankingBrazilCibaProfileConstants;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateOpenBankingBrazilCibaAuthenticationRequestExpiresIn extends AbstractCondition {

	@Override
	@PreEnvironment(required = {
		"authorization_endpoint_request",
		"backchannel_authentication_endpoint_response"
	})
	public Environment evaluate(Environment env) {
		int maximumExpiry = OpenBankingBrazilCibaProfileConstants
			.DATA_CONSENT_AUTHENTICATION_REQUEST_MAXIMUM_EXPIRY_SECONDS;
		Integer requestedExpiry = null;
		int expectedExpiresIn = maximumExpiry;

		JsonElement requestedExpiryElement = env.getElementFromObject(
			"authorization_endpoint_request", "requested_expiry");
		if (requestedExpiryElement != null) {
			try {
				Number requestedExpiryNumber = OIDFJSON.forceConversionToNumber(requestedExpiryElement);
				requestedExpiry = Integer.parseInt(requestedExpiryNumber.toString());
				expectedExpiresIn = Math.min(requestedExpiry, maximumExpiry);
			} catch (OIDFJSON.ValueIsJsonNullException e) {
				throw error("requested_expiry must not be JSON null",
					args("requested_expiry", requestedExpiryElement));
			} catch (OIDFJSON.UnexpectedJsonTypeException | NumberFormatException e) {
				throw error("requested_expiry must be an integer or a string representing an integer",
					args("requested_expiry", requestedExpiryElement));
			}
		}

		JsonElement expiresInElement = env.getElementFromObject(
			"backchannel_authentication_endpoint_response", "expires_in");
		if (expiresInElement == null || !expiresInElement.isJsonPrimitive()
			|| !expiresInElement.getAsJsonPrimitive().isNumber()) {
			throw error("expires_in is missing or is not a JSON number",
				args("expires_in", expiresInElement));
		}

		int actualExpiresIn = OIDFJSON.getInt(expiresInElement);
		if (actualExpiresIn != expectedExpiresIn) {
			throw error("expires_in does not match Open Finance Brasil requested_expiry rules", args(
				"requested_expiry", requestedExpiry,
				"maximum_expiry", maximumExpiry,
				"expected_expires_in", expectedExpiresIn,
				"actual_expires_in", actualExpiresIn));
		}

		logSuccess("expires_in matches Open Finance Brasil requested_expiry rules", args(
			"requested_expiry", requestedExpiry,
			"maximum_expiry", maximumExpiry,
			"expected_expires_in", expectedExpiresIn,
			"actual_expires_in", actualExpiresIn));
		return env;
	}
}
