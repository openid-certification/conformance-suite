package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class FAPICIBAEnsureRegistrationRequestContainsCibaGrantType extends AbstractCondition {

	private static final String CIBA_GRANT_TYPE = "urn:openid:params:grant-type:ciba";

	@Override
	@PreEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {
		JsonElement grantTypes = env.getElementFromObject("dynamic_registration_request", "grant_types");
		if (grantTypes == null || !grantTypes.isJsonArray()) {
			throw error("grant_types must be an array containing the CIBA grant type",
				args("grant_types", grantTypes, "required", CIBA_GRANT_TYPE));
		}

		for (JsonElement grantType : grantTypes.getAsJsonArray()) {
			if (grantType.isJsonPrimitive()
				&& grantType.getAsJsonPrimitive().isString()
				&& CIBA_GRANT_TYPE.equals(OIDFJSON.getString(grantType))) {
				logSuccess("Registration request contains the CIBA grant type",
					args("grant_types", grantTypes));
				return env;
			}
		}

		throw error("Registration request does not contain the CIBA grant type",
			args("grant_types", grantTypes, "required", CIBA_GRANT_TYPE));
	}
}
