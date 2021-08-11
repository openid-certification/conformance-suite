package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

public class FAPIBrazilConsentEndpointResponseValidatePermissions extends AbstractCondition {

	boolean jsonArraysContainSameEntries(JsonArray array1, JsonArray array2) {

		Set<String> array1Set = new HashSet<>();
		array1.forEach(e -> array1Set.add(OIDFJSON.getString(e)));

		Set<String> array2Set = new HashSet<>();
		array2.forEach(e -> array2Set.add(OIDFJSON.getString(e)));

		return array1Set.equals(array2Set);
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

		JsonArray requestedPermissions = (JsonArray) env.getElementFromObject("brazil_consent", "requested_permissions");

		if (requestedPermissions.size() < grantedPermissions.size()) {
			throw error("A greater number of permissions returned then requested", args("granted", grantedPermissionsEl, "requested", requestedPermissions));
		}

		for (JsonElement element : grantedPermissions) {
			if (!requestedPermissions.contains(element)) {
				throw error("Unrequested permission returned", args("granted", grantedPermissionsEl, "requested", requestedPermissions));
			}
		}

		logSuccess("Consent endpoint response contains expected permissions", args("granted", grantedPermissionsEl, "requested", requestedPermissions));

		return env;
	}

}
