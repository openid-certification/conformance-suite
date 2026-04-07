package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Removes the claims array from the first credential entry in the DCQL query.
 * This tests that the wallet can handle a DCQL query that requests a credential
 * by type (vct) without specifying individual claims.
 */
public class RemoveClaimsFromDcqlQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"dcql_query"})
	public Environment evaluate(Environment env) {

		JsonObject dcqlQuery = env.getObject("dcql_query");
		JsonObject firstCredential = dcqlQuery.getAsJsonArray("credentials").get(0).getAsJsonObject();

		var removedClaims = firstCredential.remove("claims");

		log("Removed claims array from DCQL query credential entry",
			args("removed_claims", removedClaims));

		return env;
	}
}
