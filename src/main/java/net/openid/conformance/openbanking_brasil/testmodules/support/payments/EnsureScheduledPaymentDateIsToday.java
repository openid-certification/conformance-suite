package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.time.LocalDate;
import java.time.ZoneId;

public class EnsureScheduledPaymentDateIsToday extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("resource");
		if(obj == null){
			obj = env.getObject("config");
			obj = obj.getAsJsonObject("resource");
		}
		if(obj == null){
			logFailure("Cannot find resource object.");
			return env;
		}
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");
		obj.remove("date");
		logSuccess("Successfully removed standard payment date");
		obj = obj.getAsJsonObject("schedule");
		obj = obj.getAsJsonObject("single");

		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

		log("Setting scheduled payment date to today: " + currentDate);
		obj.addProperty("date", currentDate.toString());
		logSuccess("Successfully added current date to payment schedule", obj);


		return env;
	}

}
