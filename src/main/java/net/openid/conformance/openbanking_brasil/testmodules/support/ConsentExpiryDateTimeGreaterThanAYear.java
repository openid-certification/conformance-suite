package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ConsentExpiryDateTimeGreaterThanAYear extends AbstractCondition {

	@Override
	@PreEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {

		Instant expiryTime = Instant.now().plus(367, ChronoUnit.DAYS);
		Instant expiryTimeNoFractionalSeconds = expiryTime.truncatedTo(ChronoUnit.SECONDS);
		String rfc3339ExpiryTime = DateTimeFormatter.ISO_INSTANT.format(expiryTimeNoFractionalSeconds);

		JsonObject consentRequest = env.getObject("consent_endpoint_request");
		JsonObject data = consentRequest.getAsJsonObject("data");

		data.addProperty("expirationDateTime", rfc3339ExpiryTime);
		logSuccess("Set expiry date to be more than a year away", args("expiry", rfc3339ExpiryTime));
		return env;
	}
}
