package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

/**
 * Flags a {@code credential_type} in a CAEP Credential Change event that is not one of the values listed in
 * <a href="https://openid.net/specs/openid-caep-1_0-final.html#section-3.3.1">CAEP 1.0 Section 3.3.1</a>.
 * The spec permits "any other credential type supported mutually by the Transmitter and the Receiver", so a
 * non-standard value is not a violation but may indicate an interoperability issue; callers should invoke this
 * at WARNING severity. {@code change_type} is a closed set and is enforced by
 * {@link OIDSSFValidateCaepCredentialChangeEvent}.
 * Reads the event payload from {@code ssf.caep_event.data}.
 */
public class OIDSSFWarnNonStandardCaepCredentialChangeValues extends AbstractCondition {

	private static final Set<String> STANDARD_CREDENTIAL_TYPES = Set.of(
		"password", "pin", "x509", "fido2-platform", "fido2-roaming",
		"fido-u2f", "verifiable-credential", "phone-voice", "phone-sms", "app"
	);

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();

		String credentialType = OIDFJSON.tryGetString(eventData.get("credential_type"));

		if (credentialType != null && !STANDARD_CREDENTIAL_TYPES.contains(credentialType)) {
			throw error("credential_type is not one of the standard values defined for the credential-change event; "
					+ "this may be a mutually agreed extension value",
				args("credential_type", credentialType, "standard_values", STANDARD_CREDENTIAL_TYPES));
		}

		logSuccess("credential_type is a standard value", args("credential_type", credentialType));

		return env;
	}
}
