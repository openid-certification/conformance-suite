package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Removes the last claim from the first credential's claims array in the DCQL query.
 * This tests that the wallet correctly handles a subset of claims being requested
 * and ideally only discloses the requested subset (data minimization).
 */
public class RemoveLastClaimFromDcqlQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"dcql_query"})
	public Environment evaluate(Environment env) {

		JsonObject dcqlQuery = env.getObject("dcql_query");
		JsonArray credentials = dcqlQuery.getAsJsonArray("credentials");
		JsonObject firstCredential = credentials.get(0).getAsJsonObject();
		JsonArray claims = firstCredential.getAsJsonArray("claims");

		if (claims == null || claims.size() <= 1) {
			throw error("DCQL query must have at least 2 claims to remove one",
				args("dcql_query", dcqlQuery));
		}

		var removedClaim = claims.remove(claims.size() - 1);

		log("Removed last claim from DCQL query to test selective disclosure",
			args("removed_claim", removedClaim, "remaining_claims", claims));

		return env;
	}
}
