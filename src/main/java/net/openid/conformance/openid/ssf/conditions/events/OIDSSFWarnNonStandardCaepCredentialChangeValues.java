package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

/**
 * Warns if credential_type or change_type in a CAEP Credential Change event
 * are not one of the standard values defined in CAEP 1.0 Section 3.3.
 * Non-standard values are permitted as mutually agreed extension values
 * but may indicate an interoperability issue.
 */
public class OIDSSFWarnNonStandardCaepCredentialChangeValues extends AbstractCondition {

	private static final Set<String> STANDARD_CREDENTIAL_TYPES = Set.of(
		"password", "pin", "x509", "fido2-platform", "fido2-roaming",
		"fido-u2f", "verifiable-credential", "phone-voice", "phone-sms", "app"
	);

	private static final Set<String> STANDARD_CHANGE_TYPES = Set.of("create", "revoke", "update", "delete");

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();

		String credentialType = OIDFJSON.tryGetString(eventData.get("credential_type"));
		String changeType = OIDFJSON.tryGetString(eventData.get("change_type"));

		if (credentialType != null && !STANDARD_CREDENTIAL_TYPES.contains(credentialType)) {
			throw error("credential_type is not one of the standard values defined in CAEP 1.0 Section 3.3; "
					+ "this may be a mutually agreed extension value",
				args("credential_type", credentialType, "standard_values", STANDARD_CREDENTIAL_TYPES));
		}

		if (changeType != null && !STANDARD_CHANGE_TYPES.contains(changeType)) {
			throw error("change_type is not one of the standard values defined in CAEP 1.0 Section 3.3; "
					+ "this may be a mutually agreed extension value",
				args("change_type", changeType, "standard_values", STANDARD_CHANGE_TYPES));
		}

		logSuccess("credential_type and change_type are standard values",
			args("credential_type", credentialType, "change_type", changeType));

		return env;
	}
}
