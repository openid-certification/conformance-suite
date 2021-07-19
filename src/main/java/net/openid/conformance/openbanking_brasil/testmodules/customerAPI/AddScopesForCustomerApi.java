package net.openid.conformance.openbanking_brasil.testmodules.customerAPI;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.AbstractScopeAddingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddScopesForCustomerApi extends AbstractScopeAddingCondition {

	@Override
	protected String newScope() {
		return "customers";
	}

}
