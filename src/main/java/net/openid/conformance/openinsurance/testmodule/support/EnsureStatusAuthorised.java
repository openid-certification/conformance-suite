package net.openid.conformance.openinsurance.testmodule.support;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureStatusAuthorised extends AbstractCondition {
	@Override
	@PreEnvironment(required = "consent_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement status = env.getElementFromObject("consent_endpoint_response", "data.status");
		if(status == null){
			logFailure("Cannot find Status object.");
			return env;
		}

		log("Checking if Status is AUTHORISED");
		if(!OIDFJSON.getString(status).equals("AUTHORISED")){
			logFailure("Expected AUTHORISED");
		}

		logSuccess("Successfully verified Status as AUTHORISED");

		return env;
	}
}
