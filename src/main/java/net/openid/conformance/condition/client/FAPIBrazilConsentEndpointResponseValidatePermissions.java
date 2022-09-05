package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

public class FAPIBrazilConsentEndpointResponseValidatePermissions extends AbstractCondition {

	boolean jsonArraysIsSubset(JsonArray supersetJson, JsonArray subsetJson) {

		Set<String> superset = new HashSet<>();
		supersetJson.forEach(e -> superset.add(OIDFJSON.getString(e)));

		Set<String> subset = new HashSet<>();
		subsetJson.forEach(e -> subset.add(OIDFJSON.getString(e)));

		return superset.containsAll(subset);
	}

	@Override
	@PreEnvironment(required = { "consent_endpoint_response", "brazil_consent" })
	public Environment evaluate(Environment env) {
		String path = "data.permissions";

		JsonElement grantedPermissionsEl = env.getElementFromObject("consent_endpoint_response", path);
		if (grantedPermissionsEl == null) {
			throw error("Couldn't find "+path+" in the consent response");
		}
		if (!grantedPermissionsEl.isJsonArray()) {
			throw error(path+" in the consent response is not a JSON array", args("permissions", grantedPermissionsEl));
		}
		JsonArray grantedPermissions = (JsonArray) grantedPermissionsEl;
		if (grantedPermissions.size() <= 0) {
			throw error(path+" in the consent response is an empty array", args("permissions", grantedPermissionsEl));
		}

		JsonArray requestedPermissions = (JsonArray) env.getElementFromObject("brazil_consent", "requested_permissions");

		if (!jsonArraysIsSubset(requestedPermissions,grantedPermissions)) {
			throw error("Consent endpoint response contains different permissions than requested", args("granted", grantedPermissionsEl, "requested", requestedPermissions));
		}

		logSuccess("Consent endpoint response contains expected permissions", args("granted", grantedPermissionsEl, "requested", requestedPermissions));

		return env;
	}

}
