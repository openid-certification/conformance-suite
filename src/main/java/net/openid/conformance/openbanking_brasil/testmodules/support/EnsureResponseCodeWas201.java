package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureResponseCodeWas201 extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject resourceEndpoint = env.getObject("resource_endpoint_response_full");
		Integer status = OIDFJSON.getInt(resourceEndpoint.get("status"));
		if(status == 201){
			logSuccess("Status as expected: " + status);
		} else {
			throw error("Status not as expected: " + status);
		}
		return env;
	}
}
