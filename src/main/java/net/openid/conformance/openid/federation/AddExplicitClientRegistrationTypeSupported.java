package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddExplicitClientRegistrationTypeSupported extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonObject openIdProvider = env.getElementFromObject("server", "metadata.openid_provider").getAsJsonObject();

		JsonArray clientRegistrationTypesSupported = openIdProvider.getAsJsonArray("client_registration_types_supported");
		clientRegistrationTypesSupported.add("explicit");

		openIdProvider.addProperty("federation_registration_endpoint", env.getString("base_url") + "/register");

		logSuccess("Added explicit client_registration_types_supported to openid_provider configuration", args("client_registration_types_supported", clientRegistrationTypesSupported));

		return env;
	}

}
