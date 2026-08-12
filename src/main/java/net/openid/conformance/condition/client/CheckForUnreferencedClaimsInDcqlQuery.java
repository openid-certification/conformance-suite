package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.ExtractDCQLQueryFromAuthorizationRequest;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

public class CheckForUnreferencedClaimsInDcqlQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonObject dcql = env.getObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY);

		// Malformed entries are skipped silently here; structural validity (ids being present on
		// every claims entry when claim_sets is used, and claim_sets only referencing known ids)
		// is checked by ValidateDCQLQuery, which runs before this condition.
		JsonArray offenders = new JsonArray();

		JsonElement credentialsEl = dcql.get("credentials");
		if (credentialsEl != null && credentialsEl.isJsonArray()) {
			for (JsonElement credEl : credentialsEl.getAsJsonArray()) {
				checkCredential(credEl, offenders);
			}
		}

		if (!offenders.isEmpty()) {
			throw error("The DCQL query contains entries in 'claims' that no 'claim_sets' option " +
				"references. When claim_sets is present the wallet returns the claims from one of " +
				"the listed options, so a claims entry that appears in no option can never be " +
				"requested and likely indicates a mistake in the query.",
				args("claims_not_referenced_by_any_claim_set", offenders));
		}

		logSuccess("The DCQL query does not contain claims entries unreferenced by claim_sets");
		return env;
	}

	private void checkCredential(JsonElement credEl, JsonArray offenders) {
		if (!credEl.isJsonObject()) {
			return;
		}
		JsonObject cred = credEl.getAsJsonObject();

		JsonElement claimSetsEl = cred.get("claim_sets");
		if (claimSetsEl == null || !claimSetsEl.isJsonArray()) {
			// Without claim_sets, every claims entry is requested, so none can be unreferenced.
			return;
		}
		JsonElement claimsEl = cred.get("claims");
		if (claimsEl == null || !claimsEl.isJsonArray()) {
			return;
		}

		Set<String> referencedIds = new HashSet<>();
		for (JsonElement optionEl : claimSetsEl.getAsJsonArray()) {
			if (!optionEl.isJsonArray()) {
				continue;
			}
			for (JsonElement idEl : optionEl.getAsJsonArray()) {
				if (OIDFJSON.isString(idEl)) {
					referencedIds.add(OIDFJSON.getString(idEl));
				}
			}
		}

		for (JsonElement claimEl : claimsEl.getAsJsonArray()) {
			if (!claimEl.isJsonObject()) {
				continue;
			}
			JsonObject claim = claimEl.getAsJsonObject();
			JsonElement idEl = claim.get("id");
			if (!OIDFJSON.isString(idEl)) {
				continue;
			}
			if (!referencedIds.contains(OIDFJSON.getString(idEl))) {
				JsonObject offender = new JsonObject();
				offender.add("credential_id", cred.get("id"));
				offender.add("claim_id", idEl);
				offender.add("path", claim.get("path"));
				offenders.add(offender);
			}
		}
	}
}
