package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates that the credential_id key in the vp_token response matches
 * one of the credential IDs from the DCQL query.
 *
 * Per OID4VP section 6.2, the VP Token is a JSON object where each key
 * corresponds to the id of a Credential Query in the DCQL query.
 */
public class ValidateVpTokenCredentialIdMatchesDcqlQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {

		String credentialId = env.getString("credential_id");
		JsonObject dcqlQuery = env.getObject("dcql_query");
		JsonArray credentials = dcqlQuery.getAsJsonArray("credentials");

		if (credentials == null) {
			throw error("DCQL query does not contain a credentials array");
		}

		for (JsonElement credEl : credentials) {
			JsonObject cred = credEl.getAsJsonObject();
			if (cred.has("id") && credentialId.equals(OIDFJSON.getString(cred.get("id")))) {
				logSuccess("VP token credential_id matches a credential entry in the DCQL query",
					args("credential_id", credentialId));
				return env;
			}
		}

		throw error("VP token credential_id does not match any credential entry in the DCQL query",
			args("credential_id", credentialId, "dcql_query", dcqlQuery));
	}
}
