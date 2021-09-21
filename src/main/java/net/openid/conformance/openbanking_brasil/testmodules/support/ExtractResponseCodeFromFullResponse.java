package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ExtractResponseCodeFromFullResponse extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonElement responseCode = env.getObject("resource_endpoint_response_full").get("status");

		env.putInteger("resource_endpoint_response_status", OIDFJSON.getInt(responseCode));

		return env;
	}

}



