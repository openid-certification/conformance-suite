package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateAuthorizationCodeDpopBindingKey extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"incoming_dpop_proof"})
	public Environment evaluate(Environment env) {

		String expected = env.getString("authorization_code_dpop_jkt");
		String actual = env.getString("incoming_dpop_proof", "computed_dpop_jkt");

		if (Strings.isNullOrEmpty(expected)) {
			logSuccess("Authorization request does not use DPoP authorization code binding", args("dpop_jkt", actual));
		} else if (expected.equals(actual)) {
			logSuccess("Authorization code DPoP binding matches dpop_jkt", args("dpop_jkt", actual));
		} else {
			throw error("Mismatched authorization code DPoP binding", args("expected", expected, "actual", actual));
		}
		return env;
	}

}
