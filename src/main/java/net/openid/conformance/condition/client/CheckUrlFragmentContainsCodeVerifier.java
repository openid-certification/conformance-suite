package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckUrlFragmentContainsCodeVerifier extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "code_verifier")
	public Environment evaluate(Environment env) {
		String implicitHash = env.getString("implicit_hash");

		if (Strings.isNullOrEmpty(implicitHash)) {
			throw error("URL fragment passed to redirect_uri is missing/empty but one was returned from response_uri");
		}

		String expected = "#" + env.getString("code_verifier");
		if (!expected.equals(implicitHash)) {
			throw error("URL fragment passed to redirect_uri contains more than the one expected entry",
				args("expected", expected, "actual", implicitHash));
		}

		logSuccess("URL fragment passed to redirect_uri matches expected one", args("expected", expected));

		return env;
	}
}
