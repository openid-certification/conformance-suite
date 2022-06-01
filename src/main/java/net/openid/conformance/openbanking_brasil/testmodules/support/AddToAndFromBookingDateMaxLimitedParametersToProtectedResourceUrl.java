package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"protected_resource_url", "fromBookingDate", "toBookingDate"})
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("protected_resource_url");
		String fromBookingDate = env.getString("fromBookingDate");
		String toBookingDate = env.getString("toBookingDate");

		String url = String.format("%s?fromBookingDate=%s&toBookingDate=%s", baseUrl, fromBookingDate, toBookingDate);
		env.putString("protected_resource_url", url);
		logSuccess("Parameters were added to the resource URL", Map.of("URL", url));
		return env;
	}
}
