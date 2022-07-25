package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class AppendToAndFromTransactionDateParametersToProtectedResourceUrl extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"protected_resource_url", "fromTransactionDate", "toTransactionDate"})
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("protected_resource_url");
		String fromTransactionDate = env.getString("fromTransactionDate");
		String toTransactionDate = env.getString("toTransactionDate");

		// Here, we assume there already is a query parameter in the url, thus we use & instead of ?
		String url = String.format("%s&fromTransactionDate=%s&toTransactionDate=%s", baseUrl, fromTransactionDate, toTransactionDate);
		env.putString("protected_resource_url", url);
		logSuccess("Parameters were added to the resource URL", Map.of("URL", url));
		return env;
	}
}
