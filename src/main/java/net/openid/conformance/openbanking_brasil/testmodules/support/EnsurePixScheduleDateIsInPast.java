package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JsonObjectBuilder;

import java.time.LocalDate;
import java.time.ZoneId;

public class EnsurePixScheduleDateIsInPast extends AbstractCondition {

	@Override
	@PreEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("consent_endpoint_request");

		LocalDate scheduledDate = LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1L);
		JsonObjectBuilder.addField(obj, "data.payment.schedule.single.date", scheduledDate.toString());

		log("Setting scheduled payment date to current date - 1 day: " + scheduledDate);
		obj.addProperty("date", scheduledDate.toString());
		logSuccess("Successfully created a scheduled payment date for yesterday", obj);
		return null;
	}
}
