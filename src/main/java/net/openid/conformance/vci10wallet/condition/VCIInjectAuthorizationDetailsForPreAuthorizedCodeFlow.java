package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCIInjectAuthorizationDetailsForPreAuthorizedCodeFlow extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"credential_configuration_id_hint"})
	public Environment evaluate(Environment env) {

		String credentialConfigurationIdHint = env.getString("credential_configuration_id_hint");
		log("Using credential_configuration_id " + credentialConfigurationIdHint, args("credential_configuration_id", credentialConfigurationIdHint));

		JsonObject credentialConfiguration = new JsonObject();
		credentialConfiguration.addProperty("type", "openid_credential");
		credentialConfiguration.addProperty("credential_configuration_id", credentialConfigurationIdHint);

		JsonArray rarElements = new JsonArray();
		rarElements.add(credentialConfiguration);

		JsonObject richAuthorizationRequest = new JsonObject();
		richAuthorizationRequest.add("rar", rarElements);

		env.putObject("rich_authorization_request", richAuthorizationRequest);

		log("Injected authorization details", args("rar", rarElements));

		return env;
	}
}
