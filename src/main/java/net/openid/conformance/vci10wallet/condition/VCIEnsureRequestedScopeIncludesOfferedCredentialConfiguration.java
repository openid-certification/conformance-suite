package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Issuer-initiated flow: the requested scope should include the scope of a credential configuration named
 * in the credential offer. HAIP 4.2 requires the wallet to use that scope, so HAIP callers run this as a
 * FAILURE; OpenID4VCI 5.1.2 only describes the lookup, so other profiles run it as a WARNING. One matching
 * value is enough: the wallet may add other scopes and may request a subset of the offered configurations.
 */
public class VCIEnsureRequestedScopeIncludesOfferedCredentialConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"credential_configuration_id_scope_map", "vci"}, strings = "scope")
	public Environment evaluate(Environment env) {
		JsonElement offeredEl = env.getElementFromObject("vci", "credential_offer.credential_configuration_ids");
		if (offeredEl == null || !offeredEl.isJsonArray()) {
			throw error("The credential offer has no credential_configuration_ids array", args("credential_offer", env.getElementFromObject("vci", "credential_offer")));
		}
		JsonArray offered = offeredEl.getAsJsonArray();
		List<String> requested = Arrays.asList(env.getString("scope").trim().split(" +"));
		JsonObject scopeMap = env.getObject("credential_configuration_id_scope_map");
		Set<String> offeredScopes = new LinkedHashSet<>();
		for (Map.Entry<String, JsonElement> entry : scopeMap.entrySet()) {
			for (JsonElement idEl : entry.getValue().getAsJsonArray()) {
				if (offered.contains(idEl)) {
					offeredScopes.add(entry.getKey());
				}
			}
		}
		for (String scope : offeredScopes) {
			if (requested.contains(scope)) {
				logSuccess("The requested scope includes the scope of an offered credential configuration",
					args("offered_credential_configuration_ids", offered, "scope_value", scope));
				return env;
			}
		}
		throw error("The requested scope does not include the scope of any credential configuration in the credential offer",
			args("offered_credential_configuration_ids", offered, "offered_scopes", offeredScopes, "requested_scope_values", requested));
	}
}
