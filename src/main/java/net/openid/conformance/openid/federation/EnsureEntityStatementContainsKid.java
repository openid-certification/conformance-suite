package net.openid.conformance.openid.federation;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureEntityStatementContainsKid extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

		String kid = env.getString("id_token", "header.kid");

		if (Strings.isNullOrEmpty(kid)) {
			throw error("kid was not found in the ID token header");
		}

		logSuccess("kid was found in the ID token header", args("kid", kid));

		return env;
	}

}
