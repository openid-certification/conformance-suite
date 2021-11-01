package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.time.LocalDate;
import java.time.ZoneId;

public class FAPIBrazilSetPaymentDateToToday extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject obj = env.getObject("resource");
		if(obj == null){
			obj = env.getObject("config");
			obj = obj.getAsJsonObject("resource");
		}
		if(obj == null){
			throw error("resource object not found in config");
		}
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");

		// the dates are actually in UTC, but due to many banks having issues we are following the current
		// functional tests behaviour of using a local date as per the change in this commit:
		// https://gitlab.com/obb1/certification/-/commit/3ec567cae607ae9e448ecfed5a5566e4c705690e
		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

		obj.addProperty("date", currentDate.toString());

		logSuccess("Successfully added current date ('"+currentDate.toString()+"') to payment consent payload", obj);

		return env;
	}
}
