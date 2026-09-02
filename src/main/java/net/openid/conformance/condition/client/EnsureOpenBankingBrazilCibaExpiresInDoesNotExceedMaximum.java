package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.fapiciba.OpenBankingBrazilCibaMaximumExpiry;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.math.BigDecimal;

public class EnsureOpenBankingBrazilCibaExpiresInDoesNotExceedMaximum extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {
		OpenBankingBrazilCibaMaximumExpiry.MaximumExpiry maximumExpiry;
		try {
			maximumExpiry = OpenBankingBrazilCibaMaximumExpiry.resolve(env);
		} catch (IllegalArgumentException e) {
			throw error(e.getMessage());
		}

		JsonElement expiresInElement = env.getElementFromObject(
			"backchannel_authentication_endpoint_response", "expires_in");
		Integer expiresIn = getPositiveInteger(expiresInElement);
		if (expiresIn == null) {
			log("Skipped Open Finance Brasil maximum expiry validation because CIBA-Core validation handles missing or invalid expires_in",
				args("expires_in", expiresInElement));
			return env;
		}

		if (expiresIn > maximumExpiry.seconds()) {
			throw error("expires_in exceeds the Open Finance Brasil product or service maximum", args(
				"maximum_expiry", maximumExpiry.seconds(),
				"maximum_expiry_explicitly_configured", maximumExpiry.explicitlyConfigured(),
				"actual_expires_in", expiresIn));
		}

		logSuccess("expires_in does not exceed the Open Finance Brasil product or service maximum", args(
			"maximum_expiry", maximumExpiry.seconds(),
			"maximum_expiry_explicitly_configured", maximumExpiry.explicitlyConfigured(),
			"actual_expires_in", expiresIn));
		return env;
	}

	private Integer getPositiveInteger(JsonElement element) {
		if (element == null || !element.isJsonPrimitive()
			|| !element.getAsJsonPrimitive().isNumber()) {
			return null;
		}

		try {
			int value = new BigDecimal(OIDFJSON.getNumber(element).toString()).intValueExact();
			return value > 0 ? value : null;
		} catch (ArithmeticException | NumberFormatException e) {
			return null;
		}
	}
}
