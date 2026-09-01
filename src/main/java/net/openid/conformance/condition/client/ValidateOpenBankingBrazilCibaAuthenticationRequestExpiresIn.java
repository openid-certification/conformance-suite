package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.fapiciba.OpenBankingBrazilCibaMaximumExpiry;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.math.BigDecimal;

public class ValidateOpenBankingBrazilCibaAuthenticationRequestExpiresIn extends AbstractCondition {

	@Override
	@PreEnvironment(required = {
		"authorization_endpoint_request",
		"backchannel_authentication_endpoint_response"
	})
	public Environment evaluate(Environment env) {
		OpenBankingBrazilCibaMaximumExpiry.MaximumExpiry maximumExpiry;
		try {
			maximumExpiry = OpenBankingBrazilCibaMaximumExpiry.resolve(env);
		} catch (IllegalArgumentException e) {
			throw error(e.getMessage());
		}

		Integer requestedExpiry = null;

		JsonElement requestedExpiryElement = env.getElementFromObject(
			"authorization_endpoint_request", "requested_expiry");
		if (requestedExpiryElement != null) {
			try {
				Number requestedExpiryNumber = OIDFJSON.forceConversionToNumber(requestedExpiryElement);
				requestedExpiry = Integer.parseInt(requestedExpiryNumber.toString());
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

		int actualExpiresIn;
		try {
			actualExpiresIn = new BigDecimal(OIDFJSON.getNumber(expiresInElement).toString()).intValueExact();
		} catch (ArithmeticException e) {
			throw error("expires_in must be a positive integer", args("expires_in", expiresInElement));
		}
		if (actualExpiresIn <= 0) {
			throw error("expires_in must be a positive integer", args("expires_in", expiresInElement));
		}

		boolean exactValueRequired = maximumExpiry.explicitlyConfigured()
			|| requestedExpiry != null && requestedExpiry <= maximumExpiry.seconds();
		int expectedExpiresIn = requestedExpiry != null && requestedExpiry <= maximumExpiry.seconds()
			? requestedExpiry : maximumExpiry.seconds();

		if (exactValueRequired && actualExpiresIn != expectedExpiresIn) {
			throw error("expires_in does not match Open Finance Brasil requested_expiry rules", args(
				"requested_expiry", requestedExpiry,
				"maximum_expiry", maximumExpiry.seconds(),
				"maximum_expiry_explicitly_configured", maximumExpiry.explicitlyConfigured(),
				"expected_expires_in", expectedExpiresIn,
				"actual_expires_in", actualExpiresIn));
		}
		if (!exactValueRequired && actualExpiresIn > maximumExpiry.seconds()) {
			throw error("expires_in is outside the permitted Open Finance Brasil range", args(
				"requested_expiry", requestedExpiry,
				"maximum_expiry", maximumExpiry.seconds(),
				"actual_expires_in", actualExpiresIn));
		}

		logSuccess("expires_in matches Open Finance Brasil requested_expiry rules", args(
			"requested_expiry", requestedExpiry,
			"maximum_expiry", maximumExpiry.seconds(),
			"maximum_expiry_explicitly_configured", maximumExpiry.explicitlyConfigured(),
			"actual_expires_in", actualExpiresIn));
		return env;
	}
}
