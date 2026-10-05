package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Adds a claim that no credential can contain to the single Credential Query of the DCQL query.
 *
 * Without claim_sets every requested claim is required, so the query becomes unsatisfiable:
 * per OID4VP section 6.4.1 "If the Wallet cannot deliver all claims requested by the Verifier
 * according to these rules, it MUST NOT return the respective Credential", and per section 6.4.2,
 * with that being the only (and so non-optional) credential, "it MUST NOT return any Credential(s)".
 * The query must therefore contain exactly one credential and no credential_sets.
 *
 * For mdoc the claim is placed in the namespace of the first existing claim, so that only the
 * data element, not the namespace, is missing from the credential.
 */
public class AddNonExistentClaimToDcqlQuery extends AbstractCondition {

	public static final String NON_EXISTENT_CLAIM = "conformance_suite_nonexistent_claim";

	// the query is normally the tester's, but a built-in query is used for the non-custom credential types
	private static final String QUERY = "The DCQL query (the 'dcql' field in the 'Client' section of the test configuration, "
		+ "or the conformance suite's built-in query for the selected credential type)";

	@Override
	@PreEnvironment(required = {"dcql_query"})
	@PostEnvironment(required = {"dcql_query"})
	public Environment evaluate(Environment env) {

		JsonObject dcqlQuery = env.getObject("dcql_query");

		if (dcqlQuery.has("credential_sets")) {
			throw error(QUERY + " contains credential_sets — "
					+ "this test requires a query without them, so that the single credential is required",
				args("dcql_query", dcqlQuery));
		}

		JsonArray credentials = dcqlQuery.getAsJsonArray("credentials");
		if (credentials == null || credentials.size() != 1) {
			throw error(QUERY + " must request exactly one credential "
					+ "for this test, so that a wallet unable to deliver it must not return a vp_token at all",
				args("dcql_query", dcqlQuery));
		}
		JsonObject credential = credentials.get(0).getAsJsonObject();

		if (credential.has("claim_sets")) {
			throw error(QUERY + " contains claim_sets — "
					+ "this test requires a query without them, so that every requested claim is required",
				args("dcql_query", dcqlQuery));
		}

		JsonArray claims = credential.getAsJsonArray("claims");
		if (claims == null || claims.isEmpty()) {
			throw error(QUERY + " does not request any claims — "
					+ "this test requires a query that requests at least one claim",
				args("dcql_query", dcqlQuery));
		}

		String format = OIDFJSON.getString(credential.get("format"));

		JsonArray path = new JsonArray();
		if ("mso_mdoc".equals(format)) {
			JsonArray firstPath = claims.get(0).getAsJsonObject().getAsJsonArray("path");
			path.add(OIDFJSON.getString(firstPath.get(0)));
		}
		path.add(NON_EXISTENT_CLAIM);

		JsonObject claim = new JsonObject();
		claim.add("path", path);
		claims.add(claim);

		log("Added a claim that the credential cannot contain to the DCQL query",
			args("added_claim_path", path,
				"dcql_query", dcqlQuery));

		return env;
	}

}
