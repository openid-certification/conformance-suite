package net.openid.conformance.openbanking_brasil.testmodules.customerAPI;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

import java.util.Objects;

public class PrepareToGetCustomCustomerIdentifications extends ResourceBuilder {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		allowDifferentBaseUrl = true;

		String productType = env.getString("config", "consent.productType");
		if(!Objects.equals(productType, "personal") && !Objects.equals(productType, "business")) {
			throw error("productType is not valid, it must be either personal or business", args("productType", productType));
		}
		String endPoint = "/" + productType + "/identifications";

		setApi("customers");
		setEndpoint(endPoint);

		return super.evaluate(env);
	}
}
