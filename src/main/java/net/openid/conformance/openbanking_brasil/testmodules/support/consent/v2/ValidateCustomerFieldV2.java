package net.openid.conformance.openbanking_brasil.testmodules.support.consent.v2;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateCustomerFieldV2 extends AbstractValidateFieldV2 {
    @Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		JsonElement config = env.getObject("config");
		validateMainFields(config);
		return env;
	}
}
