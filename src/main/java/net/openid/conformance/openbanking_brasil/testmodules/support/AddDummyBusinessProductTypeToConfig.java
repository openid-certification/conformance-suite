package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddDummyBusinessProductTypeToConfig extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config" )
	public Environment evaluate(Environment env) {

		String dummyBusinessProductType = "business";
		env.putString("config", "consent.productType", dummyBusinessProductType);
		logSuccess("Dummy Business Product Type added successfully", args("Dummy Business Product Type", dummyBusinessProductType));
		return env;
	}
}
