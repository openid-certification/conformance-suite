package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * HAIP 4.3: the wallet uses the scope parameter to name the Credential Type to be issued. At least one
 * requested scope value must be the scope of a credential configuration the emulated issuer advertises;
 * other values are allowed, as OpenID4VCI 5.1.2 lets the wallet add authorization server or profile scopes.
 */
public class VCIEnsureRequestedScopeIncludesCredentialConfigurationScope extends AbstractCondition {

	@Override
	@PreEnvironment(required = "credential_configuration_id_scope_map", strings = "scope")
	public Environment evaluate(Environment env) {
		Set<String> requested = new LinkedHashSet<>(Arrays.asList(env.getString("scope").trim().split(" +")));
		JsonObject scopeMap = env.getObject("credential_configuration_id_scope_map");
		List<String> matching = requested.stream().filter(scopeMap::has).toList();
		if (matching.isEmpty()) {
			throw error("None of the requested scope values is the scope of a credential configuration in the credential issuer metadata",
				args("requested_scope_values", requested, "credential_configuration_scopes", scopeMap.keySet()));
		}
		logSuccess("The requested scope names a credential configuration in the credential issuer metadata",
			args("matching_scope_values", matching, "requested_scope_values", requested));
		return env;
	}
}
