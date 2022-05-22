package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ForcePersonalProductType extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate (Environment env) {
		String productType = "personal";
		env.putString("config", "consent.productType", productType);

		logSuccess("productType changed to personal", args("productType", productType));
		return env;
	}
}
