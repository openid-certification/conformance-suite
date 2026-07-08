package net.openid.conformance.condition.common;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectVerifierRejectedPresentationPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "verifier_verification_result_screenshot")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("Upload a screenshot showing that the verifier reported the presented credential as invalid / rejected the presentation.");
		env.putString("verifier_verification_result_screenshot", placeholder);

		return env;
	}

}
