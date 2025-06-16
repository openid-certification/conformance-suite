package net.openid.conformance.vciid2wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class VCIAddCredentialDataToAuthorizationDetailsForTokenEndpointResponse extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonArray authDetails = getJsonArrayFromEnvironment(env, "rich_authorization_request", "rar", "authorization_details", false);

		for(JsonObject openIdCredential : extractOpenIdCredentials(env)) {
			authDetails.add(openIdCredential);
		}

		return env;
	}

	protected List<JsonObject> extractOpenIdCredentials(Environment env) {

		JsonObject openIdCredential = new JsonObject();
		openIdCredential.addProperty("type", "openid_credential");
		openIdCredential.addProperty("credential_configuration_id", "eu.europa.ec.eudi.pid.1");
		JsonArray credentialIdentifiers = new JsonArray();
		credentialIdentifiers.add("eu.europa.ec.eudi.pid.1:foo");
		openIdCredential.add("credential_identifiers", credentialIdentifiers);

		return List.of(openIdCredential);
	}
}
