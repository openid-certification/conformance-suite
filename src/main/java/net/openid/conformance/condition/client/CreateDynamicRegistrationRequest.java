package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		// create an empty JSON to act as the registration request
		JsonObject dynamicRegistrationRequest = new JsonObject();

		// get the specified "client_name" from the client object if there is one.
		String clientName = env.getString("client_name");

		if(Strings.isNullOrEmpty(clientName)){
			clientName = "OIDF Conformance Test " + this.getTestId();
		} else {
			clientName = clientName + " " + this.getTestId();
		}

		dynamicRegistrationRequest.addProperty("client_name", clientName);
		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		logSuccess("Created dynamic registration request", dynamicRegistrationRequest);

		return env;
	}
}
