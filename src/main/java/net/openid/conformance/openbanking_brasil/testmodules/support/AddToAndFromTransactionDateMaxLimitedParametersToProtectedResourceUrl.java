package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class AddToAndFromTransactionDateMaxLimitedParametersToProtectedResourceUrl extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"protected_resource_url", "fromTransactionDateMaxLimited", "toTransactionDateMaxLimited"})
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("protected_resource_url");
		String fromTransactionDateMaxLimited = env.getString("fromTransactionDateMaxLimited");
		String toTransactionDateMaxLimited = env.getString("toTransactionDateMaxLimited");

		String url = String.format("%s?fromTransactionDate=%s&toTransactionDate=%s", baseUrl, fromTransactionDateMaxLimited, toTransactionDateMaxLimited);
		env.putString("protected_resource_url", url);
		logSuccess("Parameters were added to the resource URL", Map.of("URL", url));
		return env;
	}
}
