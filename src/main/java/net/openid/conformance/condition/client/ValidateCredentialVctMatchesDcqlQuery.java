package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates that the credential's vct value matches what the DCQL query requested.
 */
public class ValidateCredentialVctMatchesDcqlQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt", "dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {

		String vct = env.getString("sdjwt", "credential.claims.vct");
		if (vct == null) {
			throw error("Credential does not contain a vct claim");
		}

		String credentialId = env.getString("credential_id");
		JsonObject dcqlQuery = env.getObject("dcql_query");
		JsonObject matchingCredential = DcqlQueryUtils.findCredentialById(dcqlQuery, credentialId);

		if (matchingCredential == null) {
			throw error("No DCQL credential entry found matching credential_id",
				args("credential_id", credentialId, "dcql_query", dcqlQuery));
		}

		JsonElement metaEl = matchingCredential.get("meta");
		if (metaEl == null || !metaEl.isJsonObject()) {
			log("DCQL credential entry has no meta field, skipping vct validation");
			return env;
		}

		JsonElement vctValuesEl = metaEl.getAsJsonObject().get("vct_values");
		if (vctValuesEl == null || !vctValuesEl.isJsonArray()) {
			log("DCQL credential entry has no vct_values in meta, skipping vct validation");
			return env;
		}

		JsonArray vctValues = vctValuesEl.getAsJsonArray();
		boolean matched = false;
		for (JsonElement v : vctValues) {
			if (vct.equals(OIDFJSON.getString(v))) {
				matched = true;
				break;
			}
		}

		if (!matched) {
			throw error("Credential vct does not match any of the requested vct_values in the DCQL query",
				args("vct", vct, "vct_values", vctValues, "credential_id", credentialId));
		}

		logSuccess("Credential vct matches DCQL query",
			args("vct", vct, "vct_values", vctValues));
		return env;
	}
}
