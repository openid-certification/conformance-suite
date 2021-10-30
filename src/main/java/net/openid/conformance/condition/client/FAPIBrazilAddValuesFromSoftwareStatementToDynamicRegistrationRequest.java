package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class FAPIBrazilAddValuesFromSoftwareStatementToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request", "software_statement_assertion" })
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");
		JsonObject ssaClaims = getJsonObjectFromEnvironment(env, "software_statement_assertion", "claims", "SSA claims");

		for(Map.Entry<String,JsonElement> entry : ssaClaims.entrySet()) {
			String key = entry.getKey();
			JsonElement valueEl = entry.getValue();

			dynamicRegistrationRequest.add(key, valueEl);

			// Brazil's software statement is not standards compliant because it has added "software_" as a prefix
			// to many of the keys; for example the SSA contains 'software_policy_uri' instead of 'policy_uri' as
			// defined by RFC7591.
			// But equally software_id does not have an extra 'software' added.
			// attempt to make a full DCR request by include the key both with and without any software_ prefix
			if (key.startsWith("software_")) {
				String keyNoSoftware = key.replaceFirst("software_", "");
				dynamicRegistrationRequest.add(keyNoSoftware, valueEl);
			}
		}

		log("Added values from software_statement to dynamic registration request",
			args("dynamic_registration_request", dynamicRegistrationRequest));

		return env;
	}
}
