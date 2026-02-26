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
		if (obj == null) {
			JsonObject config = env.getObject("config");
			if (config != null) {
				obj = config.getAsJsonObject("resource");
			}
		}
		if (obj == null) {
			throw error("'resource' configuration not found; please check the 'Resource' section in the test configuration");
		}
		JsonObject brazilPaymentConsent = obj.getAsJsonObject("brazilPaymentConsent");
		if (brazilPaymentConsent == null) {
			throw error("'Payment consent request JSON' field is missing from the 'Resource' section in the test configuration", obj);
		}
		JsonObject data = brazilPaymentConsent.getAsJsonObject("data");
		if (data == null) {
			throw error("'data' object is missing from the 'Payment consent request JSON' in the test configuration", brazilPaymentConsent);
		}
		JsonObject payment = data.getAsJsonObject("payment");
		if (payment == null) {
			throw error("'payment' object is missing from 'data' in the 'Payment consent request JSON' in the test configuration", data);
		}

		// the dates are actually in UTC, but due to many banks having issues we are following the current
		// functional tests behaviour of using a local date as per the change in this commit:
		// https://gitlab.com/obb1/certification/-/commit/3ec567cae607ae9e448ecfed5a5566e4c705690e
		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

		payment.addProperty("date", currentDate.toString());

		logSuccess("Successfully added current date ('"+currentDate.toString()+"') to payment consent payload", payment);

		return env;
	}
}
