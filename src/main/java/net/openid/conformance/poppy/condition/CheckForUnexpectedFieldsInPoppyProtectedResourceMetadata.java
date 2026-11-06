package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CheckForUnexpectedFieldsInPoppyProtectedResourceMetadata extends AbstractCheckForUnexpectedPoppyFields {

	// RFC 9728 section 2
	private static final Set<String> KNOWN = Set.of("resource", "authorization_servers", "jwks_uri", "scopes_supported",
		"bearer_methods_supported", "resource_signing_alg_values_supported", "resource_name", "resource_documentation",
		"resource_policy_uri", "resource_tos_uri", "tls_client_certificate_bound_access_tokens",
		"authorization_details_types_supported", "dpop_signing_alg_values_supported", "dpop_bound_access_tokens_required",
		"signed_metadata");

	@Override
	@PreEnvironment(required = "poppy_protected_resource_metadata")
	public Environment evaluate(Environment env) {
		JsonObject metadata = env.getObject("poppy_protected_resource_metadata");
		List<String> unexpected = new ArrayList<>();
		checkObject(unexpected, "", metadata, KNOWN);
		if (!unexpected.isEmpty()) {
			throw error("Protected resource metadata contains fields RFC 9728 section 2 does not define", args("unexpected_fields", unexpected));
		}
		logSuccess("Protected resource metadata contains only fields defined by RFC 9728");
		return env;
	}
}
