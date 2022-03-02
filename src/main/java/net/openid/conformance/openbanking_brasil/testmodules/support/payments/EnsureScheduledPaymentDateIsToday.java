package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JsonObjectBuilder;

import java.time.LocalDate;
import java.time.ZoneId;

public class EnsureScheduledPaymentDateIsToday extends AbstractCondition {

	@Override
	@PreEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("consent_endpoint_request");

		if(obj == null){
			logFailure("Cannot find consent_endpoint_request object.");
			return env;
		}

		LocalDate scheduledDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
		log("Setting scheduled payment date to current date: " + scheduledDate);

		JsonObjectBuilder.addField(obj, "data.payment.schedule.single.date", scheduledDate.toString());
		logSuccess("Successfully added current date to payment schedule", obj);

		return env;
	}

}
