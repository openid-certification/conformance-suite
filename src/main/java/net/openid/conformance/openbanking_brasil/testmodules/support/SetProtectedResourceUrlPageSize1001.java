package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;

public class SetProtectedResourceUrlPageSize1001 extends AbstractCondition {

	//private static Gson GSON = JsonUtils.createBigDecimalAwareGson();

	@Override
	@PostEnvironment(strings = "protected_resource_url")
	public Environment evaluate(Environment env) {

		String resourceUrl = env.getString("protected_resource_url");
		env.putString("protected_resource_url", resourceUrl.concat("?page-size=1001"));

		return env;
	}
}

