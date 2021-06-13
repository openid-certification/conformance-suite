package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddClientNameToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request")
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");

		// get the specified "client_name" from the client object if there is one.
		String clientName = env.getString("client_name");

		if(Strings.isNullOrEmpty(clientName)){
			clientName = "OIDF Conformance Test " + this.getTestId();
		} else {
			clientName = clientName + " " + this.getTestId();
		}

		dynamicRegistrationRequest.addProperty("client_name", clientName);

		log("Added client_name to registration request", dynamicRegistrationRequest);

		return env;
	}
}
