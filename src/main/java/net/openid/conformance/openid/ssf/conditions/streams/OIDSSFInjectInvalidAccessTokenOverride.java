package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class OIDSSFInjectInvalidAccessTokenOverride extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String accessTokenValue = "invalid:" + UUID.randomUUID();
		env.putString("invalid_access_token", "value", accessTokenValue);
		env.putString("invalid_access_token", "type", "Bearer");
		env.mapKey("access_token", "invalid_access_token");

		logSuccess("Replacing access_token with invalid access_token", args("invalid_access_token", accessTokenValue));

		return env;
	}

	public static void undo(Environment env) {
		env.unmapKey("access_token");
		env.removeElement("invalid_access_token", "value");
		env.removeElement("invalid_access_token", "type");
	}
}
