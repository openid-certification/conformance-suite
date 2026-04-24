package net.openid.conformance.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Adds a random unknown key/value pair to a target JsonObject in the environment to verify that
 * the receiver ignores unknown fields.
 *
 * The target is a top-level env key. If the target object does not exist in the environment, the
 * condition fails.
 */
public class AddRandomFieldsToJsonObject extends AbstractCondition {

	private final String envKey;
	private final String targetDescription;

	public AddRandomFieldsToJsonObject(String targetDescription, String envKey) {
		this.targetDescription = targetDescription;
		this.envKey = envKey;
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonObject target = env.getObject(envKey);
		if (target == null) {
			throw error("Target object not found in environment", args("env_key", envKey));
		}

		String parameter = RandomStringUtils.secure().nextAlphanumeric(16);
		String value = RandomStringUtils.secure().nextAlphanumeric(16);
		target.addProperty(parameter, value);

		logSuccess("Added a random parameter to " + targetDescription
				+ ". As per spec, unrecognized fields MUST be ignored by the receiver.",
			args("target_object", target));

		return env;
	}
}
