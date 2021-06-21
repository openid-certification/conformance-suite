package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class FAPIBrazilChangeConsentStatusToAuthorized extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "consent_response"})
	@PostEnvironment(required = "consent_response")
	public Environment evaluate(Environment env) {

		JsonObject consentResponse = env.getObject("consent_response");
		JsonObject data = consentResponse.get("data").getAsJsonObject();
		data.addProperty("status", "AUTHORISED");

		Instant baseDateRough = Instant.now();
		Instant baseDate = baseDateRough.minusNanos(baseDateRough.getNano());
		String statusUpdateDateTime = DateTimeFormatter.ISO_INSTANT.format(baseDate);

		data.addProperty("statusUpdateDateTime", statusUpdateDateTime);

		logSuccess("Changed consent status to AUTHORISED", args("consent", consentResponse));

		return env;



	}

}
