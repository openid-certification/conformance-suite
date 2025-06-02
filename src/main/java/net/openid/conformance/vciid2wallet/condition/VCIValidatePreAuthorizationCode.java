package net.openid.conformance.vciid2wallet.condition;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VCIValidatePreAuthorizationCode extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String expected = env.getString("vci","pre-authorized_code");
		String actual = env.getString("token_endpoint_request", "body_form_params.pre-authorized_code");

		if (Strings.isNullOrEmpty(expected)) {
			throw error("Couldn't find pre-authorized code to compare");
		}

		if (expected.equals(actual)) {
			logSuccess("Found pre-authorized code", args("pre-authorized_code", actual));
			return env;
		} else {
			throw error("Didn't find matching pre-authorized code", args("expected", expected, "actual", actual));
		}
	}
}
