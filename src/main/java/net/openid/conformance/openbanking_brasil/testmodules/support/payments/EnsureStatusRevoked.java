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

public class EnsureStatusRevoked extends AbstractCondition {
	@Override
	@PreEnvironment(required = "consent_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement status = env.getElementFromObject("consent_endpoint_response", "data.status");
		if(status == null){
			logFailure("Cannot find Status object.");
			return env;
		}

		log("Checking if Status is REVOKED");
		if(!OIDFJSON.getString(status).equals("REVOKED")){
			logFailure("Expected REVOKED");
		}

		logSuccess("Successfully verified Status as REVOKED");

		return env;
	}
}
