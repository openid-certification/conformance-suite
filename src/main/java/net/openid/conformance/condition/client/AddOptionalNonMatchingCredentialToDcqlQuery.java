package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Adds a second credential entry to the DCQL query that will never match any
 * real credential (uses an impossible vct value), and wraps both in credential_sets
 * where the original is required and the fake one is optional.
 *
 * This tests that wallets correctly handle credential_sets with optional entries
 * and don't fail the whole request when an optional credential cannot be matched.
 *
 * The DCQL query must NOT already contain credential_sets.
 */
public class AddOptionalNonMatchingCredentialToDcqlQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"dcql_query"})
	@PostEnvironment(required = {"dcql_query"})
	public Environment evaluate(Environment env) {

		JsonObject dcqlQuery = env.getObject("dcql_query");

		if (dcqlQuery.has("credential_sets")) {
			throw error("DCQL query already contains credential_sets — this test requires a query without them",
				args("dcql_query", dcqlQuery));
		}

		JsonArray credentials = dcqlQuery.getAsJsonArray("credentials");
		String originalId = OIDFJSON.getString(credentials.get(0).getAsJsonObject().get("id"));

		// Add a fake credential entry that can never match
		String fakeId = "nonexistent_credential";
		JsonObject fakeCredential = new JsonObject();
		fakeCredential.addProperty("id", fakeId);
		fakeCredential.addProperty("format", "dc+sd-jwt");
		JsonObject fakeMeta = new JsonObject();
		JsonArray fakeVctValues = new JsonArray();
		fakeVctValues.add("urn:conformance-suite:nonexistent:credential:1");
		fakeMeta.add("vct_values", fakeVctValues);
		fakeCredential.add("meta", fakeMeta);
		JsonArray fakeClaims = new JsonArray();
		JsonObject fakeClaim = new JsonObject();
		JsonArray fakePath = new JsonArray();
		fakePath.add("nonexistent_claim");
		fakeClaim.add("path", fakePath);
		fakeClaims.add(fakeClaim);
		fakeCredential.add("claims", fakeClaims);
		credentials.add(fakeCredential);

		// Add credential_sets: original is required, fake is optional
		JsonArray credentialSets = new JsonArray();

		JsonObject requiredSet = new JsonObject();
		JsonArray requiredOptions = new JsonArray();
		JsonArray requiredOption = new JsonArray();
		requiredOption.add(originalId);
		requiredOptions.add(requiredOption);
		requiredSet.add("options", requiredOptions);
		requiredSet.addProperty("required", true);
		credentialSets.add(requiredSet);

		JsonObject optionalSet = new JsonObject();
		JsonArray optionalOptions = new JsonArray();
		JsonArray optionalOption = new JsonArray();
		optionalOption.add(fakeId);
		optionalOptions.add(optionalOption);
		optionalSet.add("options", optionalOptions);
		optionalSet.addProperty("required", false);
		credentialSets.add(optionalSet);

		dcqlQuery.add("credential_sets", credentialSets);

		log("Added non-matching optional credential and credential_sets to DCQL query",
			args("original_credential_id", originalId,
				"fake_credential_id", fakeId,
				"dcql_query", dcqlQuery));

		return env;
	}
}
