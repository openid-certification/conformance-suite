package net.openid.conformance.openbanking_brasil.testmodules.productsNServices;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class PrepareToGetProductsNServices extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		if (getRequirements().isEmpty()) {
			throw error("Url part to resource must be add in environment, when configure test module.");
		}
		String urlPart = getRequirements().iterator().next();
		String baseURL = env.getString("config", "resource.resourceUrl");
		String protectedUrl = String.format("%s/%s", baseURL, urlPart);
		env.putString("protected_resource_url", protectedUrl);
		env.putString("resource_endpoint_response", "");
		return env;
	}

}
