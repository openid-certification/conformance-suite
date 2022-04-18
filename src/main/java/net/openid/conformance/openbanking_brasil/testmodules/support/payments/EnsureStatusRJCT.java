package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonObjectBuilder;

import java.time.LocalDate;
import java.time.ZoneId;

public class EnsureStatusRJCT extends AbstractCondition {
	@Override
	@PreEnvironment(required = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement status = env.getElementFromObject("resource_endpoint_response", "data.status");
		if(status == null){
			logFailure("Cannot find Status object.");
			return env;
		}

		log("Checking if Status is RJCT");
		if(OIDFJSON.getString(status).equals("RJCT")){
			logSuccess("Successfully verified Status as RJCT");
		} else {
			logFailure("Expected RJCT");
		}

		return env;
	}
}
