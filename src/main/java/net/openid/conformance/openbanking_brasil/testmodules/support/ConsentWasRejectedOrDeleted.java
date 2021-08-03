package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ConsentWasRejectedOrDeleted extends AbstractJsonAssertingCondition {
	@Override
	public Environment evaluate(Environment env) {
		if (env.containsObject("errored_response")) {
			JsonObject response = env.getObject("errored_response");
			String consentId = env.getString("consent_id");
			int statusCode = OIDFJSON.getInt(response.get("status_code"));
			if(statusCode != 404) {
				error("Was expecting a 404 response but it was actually " + statusCode);
			}
			log("The consent was not found, as expected.", args("consentId", consentId));
			return env;
		} else {
			JsonObject consentResponse = bodyFrom(env);
			JsonElement statusElement = findByPath(consentResponse, "$.data.status");
			String status = OIDFJSON.getString(statusElement);
			if (!status.equals("REJECTED")) {
				throw error("Expected consent to be in the REJECTED state after redirect but it was not", args("status", status));
			}
			logSuccess("Consent was in the REJECTED state after redirect");
			return env;
		}
	}
}
