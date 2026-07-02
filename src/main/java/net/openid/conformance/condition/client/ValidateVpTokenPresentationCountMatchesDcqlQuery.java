package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates that each entry in the vp_token contains exactly one Presentation when the
 * corresponding DCQL Credential Query has 'multiple' omitted or set to false, as per
 * OID4VP 1.0 section 8.1.
 *
 * Entries whose key doesn't match a Credential Query (reported by
 * ValidateVpTokenCredentialIdMatchesDcqlQuery) or whose value is not an array (reported by
 * ExtractVP1FinalVpTokenDCQL) are skipped so failures aren't double-reported.
 */
public class ValidateVpTokenPresentationCountMatchesDcqlQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_response", "dcql_query"})
	public Environment evaluate(Environment env) {

		JsonElement vpToken = env.getElementFromObject("authorization_endpoint_response", "vp_token");
		if (vpToken == null || !vpToken.isJsonObject()) {
			throw error("Missing or invalid vp_token parameter", args("vp_token", vpToken));
		}
		JsonObject dcqlQuery = env.getObject("dcql_query");

		for (var entry : vpToken.getAsJsonObject().entrySet()) {
			String credentialId = entry.getKey();
			JsonElement presentations = entry.getValue();

			JsonObject credentialQuery = DcqlQueryUtils.findCredentialById(dcqlQuery, credentialId);
			if (credentialQuery == null || !presentations.isJsonArray()) {
				continue;
			}

			JsonElement multipleEl = credentialQuery.get("multiple");
			boolean multiple = multipleEl != null && OIDFJSON.getBoolean(multipleEl);
			int count = presentations.getAsJsonArray().size();

			if (!multiple && count != 1) {
				throw error("The vp_token array for a Credential Query with 'multiple' omitted or set to false must contain exactly one Presentation",
					args("credential_id", credentialId, "presentation_count", count, "vp_token", vpToken));
			}
		}

		logSuccess("Each vp_token entry contains a number of Presentations consistent with the Credential Query's 'multiple' setting",
			args("vp_token", vpToken));

		return env;
	}
}
