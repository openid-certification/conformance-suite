package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.time.LocalDate;
import java.time.ZoneId;

public class EnsurePaymentDateIsToday extends AbstractCondition {

	@Override
	protected Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");

		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Fortaleza"));
		log("Setting date in consent config to current date:");
		obj.addProperty("date", currentDate.toString());
		logSuccess("Successfully added current date to consent config", obj);

		obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPixPayment");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");

		log("Setting date in payment config to current date:");
		obj.addProperty("date", currentDate.toString());
		logSuccess("Successfully added current date to payment config", obj);

		logSuccess("");

		return env;
	}
}
