package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddSectorIdentifierUriToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request", strings = "base_url")
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");

		String baseUri = env.getString("base_url");

		String sectorIdentifierUri = baseUri + "/redirect_uris.json";

		dynamicRegistrationRequest.addProperty("sector_identifier_uri", sectorIdentifierUri);

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Added sector_identifier_uri to dynamic registration request", args("dynamic_registration_request", dynamicRegistrationRequest));

		return env;
	}

}
