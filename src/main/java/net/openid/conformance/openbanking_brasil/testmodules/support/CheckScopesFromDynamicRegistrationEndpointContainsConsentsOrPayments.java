package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class CheckScopesFromDynamicRegistrationEndpointContainsConsentsOrPayments extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");
		if (client.has("scope")) {
			String scopes = OIDFJSON.getString(client.get("scope"));

			if(scopes.contains("consents")){
				env.putString("scopeToBeUsed", "consents");
			} else if (scopes.contains("payments")) {
				env.putString("scopeToBeUsed", "payments");
			}else {
				logFailure("Required scopes are not present in the Dynamic Client Registration response",
					Map.of("required", "consents or payments", "present", scopes));
			}

		}else {
			logFailure("scope field is missing in Dynamic Client Registration response");
		}
		return null;
	}
}
