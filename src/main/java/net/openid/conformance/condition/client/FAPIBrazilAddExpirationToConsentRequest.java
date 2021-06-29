package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.ZoneId;
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

		String rfc339ExpiryTime = DateTimeFormatter.ISO_INSTANT.format(expiryTime);

		data.addProperty("expirationDateTime", rfc339ExpiryTime);

		logSuccess("Added expiration time to consent request", args("consent_endpoint_request", consentRequest));

		return env;
	}

}
