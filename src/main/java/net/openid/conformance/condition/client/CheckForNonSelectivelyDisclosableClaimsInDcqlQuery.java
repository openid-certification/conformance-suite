package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.ExtractDCQLQueryFromAuthorizationRequest;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

public class CheckForNonSelectivelyDisclosableClaimsInDcqlQuery extends AbstractCondition {

	// SD-JWT VC registered JWT claims that MUST NOT be selectively disclosable, per
	// draft-ietf-oauth-sd-jwt-vc-13 section 3.2.2.2 (section 2.2.2.3 in draft-18, which also
	// adds aka_vcts). 'sub' and 'iat' are deliberately excluded: they MAY be selectively
	// disclosable.
	private static final Set<String> NON_SELECTIVELY_DISCLOSABLE_CLAIMS =
		Set.of("iss", "nbf", "exp", "cnf", "vct", "vct#integrity", "status", "aka_vcts");

	@Override
	@PreEnvironment(required = ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonObject dcql = env.getObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY);

		// Malformed entries are skipped silently here; structural validity is checked by
		// ValidateDCQLQuery, which runs before this condition.
		JsonArray offenders = new JsonArray();

		JsonElement credentialsEl = dcql.get("credentials");
		if (credentialsEl != null && credentialsEl.isJsonArray()) {
			for (JsonElement credEl : credentialsEl.getAsJsonArray()) {
				checkCredential(credEl, offenders);
			}
		}

		if (!offenders.isEmpty()) {
			throw error("The DCQL query requests SD-JWT VC registered claims that, when present, are " +
				"carried in the issuer-signed part of the credential and cannot be selectively disclosed. " +
				"Requesting them in a DCQL claims query has no effect on disclosure and likely " +
				"indicates a mistake in the query.",
				args("non_selectively_disclosable_claims_requested", offenders));
		}

		logSuccess("DCQL query does not request any SD-JWT VC claims that cannot be selectively disclosed");
		return env;
	}

	private void checkCredential(JsonElement credEl, JsonArray offenders) {
		if (!credEl.isJsonObject()) {
			return;
		}
		JsonObject cred = credEl.getAsJsonObject();

		// mdoc data elements are all selectively disclosable, and mdoc claim paths are
		// namespace-scoped, so only dc+sd-jwt credential queries are checked.
		JsonElement formatEl = cred.get("format");
		if (!OIDFJSON.isString(formatEl) || !"dc+sd-jwt".equals(OIDFJSON.getString(formatEl))) {
			return;
		}

		JsonElement claimsEl = cred.get("claims");
		if (claimsEl == null || !claimsEl.isJsonArray()) {
			return;
		}
		for (JsonElement claimEl : claimsEl.getAsJsonArray()) {
			if (!claimEl.isJsonObject()) {
				continue;
			}
			JsonElement pathEl = claimEl.getAsJsonObject().get("path");
			if (pathEl == null || !pathEl.isJsonArray()) {
				continue;
			}
			JsonArray path = pathEl.getAsJsonArray();
			// Only the first path element identifies a top-level registered claim; a nested
			// path like ["address", "status"] addresses a different, legitimate claim.
			if (path.isEmpty() || !OIDFJSON.isString(path.get(0))) {
				continue;
			}
			if (NON_SELECTIVELY_DISCLOSABLE_CLAIMS.contains(OIDFJSON.getString(path.get(0)))) {
				JsonObject offender = new JsonObject();
				offender.add("credential_id", cred.get("id"));
				offender.add("path", path);
				offenders.add(offender);
			}
		}
	}
}
