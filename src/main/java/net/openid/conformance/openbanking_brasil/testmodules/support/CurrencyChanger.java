package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class CurrencyChanger extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		log("Current currency: " + env.getObject("config")
				.getAsJsonObject("resource")
				.getAsJsonObject("brazilPaymentConsent")
				.getAsJsonObject("data")
				.getAsJsonObject("payment")
				.getAsJsonPrimitive("currency")
				.toString()
		);
		env
			.getObject("config")
			.getAsJsonObject("resource")
			.getAsJsonObject("brazilPaymentConsent")
			.getAsJsonObject("data")
			.getAsJsonObject("payment")
			.addProperty("currency", "ZZZ");
		logSuccess("Changed currency to ZZZ successfully");
		return env;
	}

}
