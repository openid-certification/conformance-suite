package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JsonObjectBuilder;

import java.time.LocalDate;
import java.time.ZoneId;

public class EnsurePaymentDateIsTomorrow extends AbstractCondition {

	@Override
	@PreEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("consent_endpoint_request");
		if(obj == null){
			logFailure("Cannot find consent_endpoint_request object.");
			return env;
		}

		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(1L);
		log("Setting payment date to current date +1: " + currentDate);

		JsonObjectBuilder.addField(obj, "data.payment.date", currentDate.toString());

		logSuccess("Successfully added current date +1 to payment schedule", obj);

		return env;
	}

}
