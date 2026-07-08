package net.openid.conformance.condition.common;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectVerifierSuccessfulVerificationPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "verifier_verification_result_screenshot")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("Upload a screenshot showing that the verifier successfully verified the presented credential (for example, a page displaying the credential contents).");
		env.putString("verifier_verification_result_screenshot", placeholder);

		return env;
	}

}
