package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class OIDSSFCheckScopesWithOauthProtectedResourceMetadata extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject oauthProtectedResourceMetadata = env.getObject("oauth_protected_resource_metadata");
		if (oauthProtectedResourceMetadata == null) {
			log("No OAuth Protected Resource Metadata found, skipping check. ");
			return env;
		}

		JsonElement scopesSupportedElement = oauthProtectedResourceMetadata.get("scopes_supported");
		if (scopesSupportedElement == null) {
			logFailure("OAuth Protected Resource Metadata missing required claim scopes_supported", args("oauth_protected_resource_metadata", oauthProtectedResourceMetadata));
			return env;
		}

		// add the scope if it exists
		String requestedClientScopes = env.getString("client", "scope");
		if (requestedClientScopes == null) {
			log("No requested client scopes found, skipping check.");
			return env;
		}

		List<String> supportedScopes = OIDFJSON.convertJsonArrayToList(scopesSupportedElement.getAsJsonArray());
		List<String> requestedScopes = List.of(requestedClientScopes.split(" "));

		if (supportedScopes.containsAll(requestedScopes)) {
			logSuccess("All requested client scopes are supported by the protected resource", args("requested_scopes", requestedScopes, "scopes_supported", supportedScopes));
		} else {
			logFailure("Requested client scopes are not supported by the protected resource", args("requested_scopes", requestedScopes, "scopes_supported", supportedScopes));
		}

		return env;
	}
}
