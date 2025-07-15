package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VCIInjectAuthorizationDetailsForPreAuthorizedCodeFlow extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject credentialConfiguration = new JsonObject();
		credentialConfiguration.addProperty("type", "openid_credential");
		credentialConfiguration.addProperty("credential_configuration_id", "eu.europa.ec.eudi.pid.1");

		JsonArray rarElements = new JsonArray();
		rarElements.add(credentialConfiguration);

		JsonObject richAuthorizationRequest = new JsonObject();
		richAuthorizationRequest.add("rar", rarElements);

		env.putObject("rich_authorization_request", richAuthorizationRequest);

		log("Injected authorization details", args("rar", rarElements));

		return env;
	}
}
