package net.openid.conformance.fapiciba;

import com.google.gson.JsonElement;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.math.BigDecimal;

public final class OpenBankingBrazilCibaMaximumExpiry {

	public static final String CONFIGURATION_FIELD = "brazil_ciba_maximum_expiry";
	public static final String CONFIGURATION_FIELD_LABEL = "Brazil CIBA maximum expiry";

	private OpenBankingBrazilCibaMaximumExpiry() {
	}

	public static MaximumExpiry resolve(Environment env) {
		JsonElement configuredMaximum = env.getElementFromObject("client", CONFIGURATION_FIELD);
		if (configuredMaximum == null) {
			return new MaximumExpiry(
				OpenBankingBrazilCibaProfileConstants.DEFAULT_AUTHENTICATION_REQUEST_MAXIMUM_EXPIRY_SECONDS,
				false);
		}

		try {
			Number configuredMaximumNumber = OIDFJSON.forceConversionToNumber(configuredMaximum);
			int seconds = new BigDecimal(configuredMaximumNumber.toString()).intValueExact();
			if (seconds <= 0 || seconds == Integer.MAX_VALUE) {
				throw invalidConfiguration();
			}
			return new MaximumExpiry(seconds, true);
		} catch (OIDFJSON.ValueIsJsonNullException | OIDFJSON.UnexpectedJsonTypeException
				 | ArithmeticException | NumberFormatException e) {
			throw invalidConfiguration();
		}
	}

	private static IllegalArgumentException invalidConfiguration() {
		return new IllegalArgumentException("'%s' field in the 'Client' section must be a positive integer no greater than %d in the test configuration"
			.formatted(CONFIGURATION_FIELD_LABEL, Integer.MAX_VALUE - 1));
	}

	public record MaximumExpiry(int seconds, boolean explicitlyConfigured) {
	}
}
