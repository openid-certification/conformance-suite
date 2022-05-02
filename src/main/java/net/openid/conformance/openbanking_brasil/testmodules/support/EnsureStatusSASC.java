package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureStatusSASC extends AbstractCondition {
	@Override
	@PreEnvironment(required = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement status = env.getElementFromObject("resource_endpoint_response", "data.status");
		if(status == null){
			logFailure("Cannot find Status object.");
			return env;
		}

		log("Checking if Status is SASC");
		if(!OIDFJSON.getString(status).equals("SASC")){
			logFailure("Expected SASC");
		}

		logSuccess("Successfully verified Status as SASC");


		return env;
	}
}
