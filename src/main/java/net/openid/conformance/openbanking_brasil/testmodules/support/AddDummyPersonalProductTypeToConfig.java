package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddDummyPersonalProductTypeToConfig extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config" )
	public Environment evaluate(Environment env) {

		String dummyPersonalProductType = "personal";
		env.putString("config", "consent.productType", dummyPersonalProductType);
		logSuccess("Dummy Personal Product Type added successfully", args("Dummy Personal Product Type", dummyPersonalProductType));
		return env;
	}
}
