package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.client.AddCodeChallengeToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateRandomCodeVerifier;
import io.fintechlabs.testframework.condition.client.CreateS256CodeChallenge;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

/**
 * @author jricher
 *
 */
public class AddPKCEToAuthorizationRequest extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		call(condition(CreateRandomCodeVerifier.class));
		call(exec().exposeEnvironmentString("code_verifier"));
		call(condition(CreateS256CodeChallenge.class));
		call(exec()
			.exposeEnvironmentString("code_challenge")
			.exposeEnvironmentString("code_challenge_method"));
		call(condition(AddCodeChallengeToAuthorizationEndpointRequest.class)
			.requirement("FAPI-R-5.2.2-7"));

	}

}
