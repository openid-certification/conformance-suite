package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class FAPIBrazilAddExpirationToConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "consent_endpoint_request" )
	@PostEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject consentRequest = env.getObject("consent_endpoint_request");
		JsonObject data = consentRequest.getAsJsonObject("data");

		Instant expiryTime = Instant.now().plus(2, ChronoUnit.HOURS);
		Instant expiryTimeNoFractionalSeconds = expiryTime.truncatedTo(ChronoUnit.SECONDS);

		String rfc3339ExpiryTime = DateTimeFormatter.ISO_INSTANT.format(expiryTimeNoFractionalSeconds);

		data.addProperty("expirationDateTime", rfc3339ExpiryTime);

		logSuccess("Added expiration time to consent request", args("consent_endpoint_request", consentRequest));

		return env;
	}

}
