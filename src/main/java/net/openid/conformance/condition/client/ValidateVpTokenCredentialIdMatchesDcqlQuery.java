package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Validates that the credential_id key in the vp_token response matches
 * one of the credential IDs from the DCQL query.
 */
public class ValidateVpTokenCredentialIdMatchesDcqlQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {

		String credentialId = env.getString("credential_id");
		JsonObject dcqlQuery = env.getObject("dcql_query");

		if (DcqlQueryUtils.findCredentialById(dcqlQuery, credentialId) != null) {
			logSuccess("VP token credential_id matches a credential entry in the DCQL query",
				args("credential_id", credentialId));
			return env;
		}

		throw error("VP token credential_id does not match any credential entry in the DCQL query",
			args("credential_id", credentialId, "dcql_query", dcqlQuery));
	}
}
