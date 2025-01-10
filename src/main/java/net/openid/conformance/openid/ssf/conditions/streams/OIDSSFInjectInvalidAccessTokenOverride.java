package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFInjectInvalidAccessTokenOverride extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		env.putString("invalid_access_token", "value", "invalid");
		env.putString("invalid_access_token", "type", "Bearer");
		env.mapKey("access_token", "invalid_access_token");

		logSuccess("Overrode access_token with invalid access_token");

		return env;
	}

	public static void undo(Environment env) {
		env.unmapKey("access_token");
		env.removeElement("invalid_access_token", "value");
		env.removeElement("invalid_access_token", "type");
	}
}
