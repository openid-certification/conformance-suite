package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * A missing or malformed grant_types is reported by CMFChileEnsureRegistrationRequestContainsMandatoryMetadata.
 */
public class CMFChileEnsureRegistrationRequestGrantTypesIsAuthorizationCodeOnly extends AbstractCondition {

	private static final String AUTHORIZATION_CODE = "authorization_code";

	@Override
	@PreEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {
		JsonElement grantTypes = env.getElementFromObject("dynamic_registration_request", "grant_types");
		if (grantTypes == null || !grantTypes.isJsonArray()) {
			log("grant_types is not a JSON array in the registration request", args("grant_types", grantTypes));
			return env;
		}
		JsonArray array = grantTypes.getAsJsonArray();
		for (JsonElement element : array) {
			if (!element.isJsonPrimitive() || !AUTHORIZATION_CODE.equals(OIDFJSON.getString(element))) {
				throw error("grant_types in the registration request must only contain 'authorization_code'",
					args("grant_types", array, "unexpected", element));
			}
		}
		logSuccess("grant_types in the registration request only contains 'authorization_code'", args("grant_types", array));
		return env;
	}
}
