package net.openid.conformance.openinsurance.testmodule.support;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OpinSetPermissionsBuilderForAllPermissions extends AbstractCondition {

	@PostEnvironment(strings = {"permissions_builder"})
	@Override
	public Environment evaluate(Environment env) {

		String productType = env.getString("config", "consent.productType");
		if (Strings.isNullOrEmpty(productType)) {
			throw error("Product type (Business or Personal) must be specified in the test configuration");
		}

		String permissionsBuilder;
		if(productType.equals("business")) {
			permissionsBuilder = PermissionsGroup.ALL_BUSINESS.name();

		} else {
			permissionsBuilder = PermissionsGroup.All_PERSONAL.name();
		}

		env.putString("permissions_builder", permissionsBuilder);
		logSuccess(String.format("permissions_builder was set to %s", permissionsBuilder));

		return env;
	}
}
