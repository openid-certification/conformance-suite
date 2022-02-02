package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.LocalDate;
import java.time.ZoneId;

public class FAPIBrazilScheduleConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "consent_endpoint_request" )
	@PostEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject requestObject = env.getObject("consent_endpoint_request");
		requestObject = (JsonObject) requestObject.get("data");
		requestObject = (JsonObject) requestObject.get("payment");

		JsonObject schedule = new JsonObject();
		JsonObject single = new JsonObject();
		schedule.add("single", single);
		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

		single.addProperty("date", currentDate.toString());
		requestObject.add("schedule", schedule);

		return env;
	}
}
