package net.openid.conformance.openbanking_brasil.plans;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateHttpResponseWas404 extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("errored_response");
		int statusCode = OIDFJSON.getInt(response.get("status_code"));
		if(statusCode != 404) {
			error("Was expecting a 404 response but it was actually " + statusCode);
		}
		return env;
	}
}
