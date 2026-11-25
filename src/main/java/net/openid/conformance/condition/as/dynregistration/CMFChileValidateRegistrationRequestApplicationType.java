package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

/**
 * A missing or malformed application_type is reported by CMFChileEnsureRegistrationRequestContainsMandatoryMetadata.
 */
public class CMFChileValidateRegistrationRequestApplicationType extends AbstractCondition {

	private static final Set<String> ALLOWED = Set.of("web", "service");

	@Override
	@PreEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {
		JsonElement applicationType = env.getElementFromObject("dynamic_registration_request", "application_type");
		if (applicationType == null || !applicationType.isJsonPrimitive() || !applicationType.getAsJsonPrimitive().isString()) {
			log("application_type is not a string in the registration request", args("application_type", applicationType));
			return env;
		}
		String value = OIDFJSON.getString(applicationType);
		if (!ALLOWED.contains(value)) {
			throw error("application_type in the registration request must be 'web' or 'service'",
				args("application_type", value, "allowed", ALLOWED));
		}
		logSuccess("application_type in the registration request is allowed", args("application_type", value));
		return env;
	}
}
