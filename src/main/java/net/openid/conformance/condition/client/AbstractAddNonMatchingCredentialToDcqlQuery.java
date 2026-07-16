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
 * where the original is required and the fake one is required or optional depending
 * on the subclass.
 *
 * The DCQL query must NOT already contain credential_sets.
 */
public abstract class AbstractAddNonMatchingCredentialToDcqlQuery extends AbstractCondition {

	/** Whether the credential_set for the non-matching credential is marked required. */
	protected abstract boolean nonMatchingCredentialRequired();

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
		JsonObject originalCredential = credentials.get(0).getAsJsonObject();
		String originalId = OIDFJSON.getString(originalCredential.get("id"));
		String originalFormat = OIDFJSON.getString(originalCredential.get("format"));

		// Add a fake credential entry that can never match, with the same format as the original
		// so a single-format test configuration still validates cleanly.
		String fakeId = "nonexistent_credential";
		JsonObject fakeCredential = new JsonObject();
		fakeCredential.addProperty("id", fakeId);
		fakeCredential.addProperty("format", originalFormat);
		JsonObject fakeMeta = new JsonObject();
		JsonArray fakeClaims = new JsonArray();
		JsonObject fakeClaim = new JsonObject();
		JsonArray fakePath = new JsonArray();
		if ("mso_mdoc".equals(originalFormat)) {
			fakeMeta.addProperty("doctype_value", "org.conformance-suite.nonexistent.credential.1");
			fakePath.add("org.conformance-suite.nonexistent.namespace.1");
			fakePath.add("nonexistent_claim");
		} else {
			JsonArray fakeVctValues = new JsonArray();
			fakeVctValues.add("urn:conformance-suite:nonexistent:credential:1");
			fakeMeta.add("vct_values", fakeVctValues);
			fakePath.add("nonexistent_claim");
		}
		fakeCredential.add("meta", fakeMeta);
		fakeClaim.add("path", fakePath);
		fakeClaims.add(fakeClaim);
		fakeCredential.add("claims", fakeClaims);
		credentials.add(fakeCredential);

		JsonArray credentialSets = new JsonArray();
		credentialSets.add(credentialSet(originalId, true));
		credentialSets.add(credentialSet(fakeId, nonMatchingCredentialRequired()));
		dcqlQuery.add("credential_sets", credentialSets);

		String fakeRequirement = nonMatchingCredentialRequired() ? "required" : "optional";
		log("Added non-matching " + fakeRequirement + " credential and credential_sets to DCQL query",
			args("original_credential_id", originalId,
				"fake_credential_id", fakeId,
				"dcql_query", dcqlQuery));

		return env;
	}

	private static JsonObject credentialSet(String credentialId, boolean required) {
		JsonObject set = new JsonObject();
		JsonArray options = new JsonArray();
		JsonArray option = new JsonArray();
		option.add(credentialId);
		options.add(option);
		set.add("options", options);
		set.addProperty("required", required);
		return set;
	}
}
