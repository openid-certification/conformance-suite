package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Objects;

public class BuildCustomCustomersConfigResourceUrlFromConsentUrl extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		String productType = env.getString("config", "consent.productType");
		if(!Objects.equals(productType, "personal") && !Objects.equals(productType, "business")) {
			throw error("productType is not valid, it must be either personal or business", args("productType", productType));
		}

		String consentUrl = env.getString("config","resource.consentUrl");
		String apiReplacement = "customers";
		String endpointReplacement = productType + "/identifications";

		String validatorRegex = "^(https://)(.*?)(consents/v[0-9]/consents)";
		if(!consentUrl.matches(validatorRegex)) {
			throw error("consentUrl is not valid, please ensure that url matches " + validatorRegex, args("consentUrl", consentUrl));
		}

		String resourceUrl = consentUrl.replaceFirst("consents", apiReplacement).replaceFirst("consents", endpointReplacement);
		env.putString("config", "resource.resourceUrl", resourceUrl);

		logSuccess(String.format("resourceUrl for %s set up", apiReplacement), args("resourceUrl", resourceUrl));
		return env;
	}
}
