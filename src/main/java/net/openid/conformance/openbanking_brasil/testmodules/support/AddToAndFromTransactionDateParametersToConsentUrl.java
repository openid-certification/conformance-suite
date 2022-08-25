package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class AddToAndFromTransactionDateParametersToConsentUrl extends AbstractCondition
{
	@Override
	@PreEnvironment(strings = {"fromTransactionDate", "toTransactionDate"}, required = "config")
	@PostEnvironment(strings = "consent_url")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("config", "resource.consentUrl");
		String fromTransactionDate = env.getString("fromTransactionDate");
		String toTransactionDate = env.getString("toTransactionDate");

		String url = String.format("%s?fromTransactionDate=%s&toTransactionDate=%s", baseUrl, fromTransactionDate, toTransactionDate);
		env.putString("consent_url", url);
		logSuccess("Parameters were added to the consent URL", Map.of("URL", url));
		return env;
	}
}
