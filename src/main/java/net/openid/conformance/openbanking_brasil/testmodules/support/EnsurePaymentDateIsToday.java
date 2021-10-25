package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.time.LocalDate;
import java.time.ZoneId;

public class EnsurePaymentDateIsToday extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		log(env.toString());

		JsonObject obj;
		try {
			obj = env.getObject("resource");
			if(obj == null){
				throw new NullPointerException("No resource Object");
			}
		} catch(NullPointerException e) {
			obj = env.getObject("config");
			obj = obj.getAsJsonObject("resource");
		}
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");

		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Fortaleza"));

		log("Setting date in consent config to current date: " + currentDate);
		obj.addProperty("date", currentDate.toString());
		logSuccess("Successfully added current date to consent config", obj);

		return env;
	}
}
