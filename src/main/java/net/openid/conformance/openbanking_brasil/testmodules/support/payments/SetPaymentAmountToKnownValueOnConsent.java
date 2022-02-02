package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetPaymentAmountToKnownValueOnConsent extends AbstractCondition {

	private static final Pattern PATTERN = Pattern.compile("^\\{\\{(?<amount>\\d+\\.\\d+)\\}\\}$");

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment");

		String amount = OIDFJSON.getString(obj.get("amount"));

		Matcher matcher = PATTERN.matcher(amount);
		if(matcher.matches()) {
			amount = matcher.group("amount");
			logSuccess("Allowed configured amount to pass through", Map.of("amount", amount));
			obj.addProperty("amount", amount);
			return env;
		}

		obj.addProperty("amount", "100.00");

		logSuccess("Added payment amount of 100.00 to payment consent");

		return env;
	}
}
