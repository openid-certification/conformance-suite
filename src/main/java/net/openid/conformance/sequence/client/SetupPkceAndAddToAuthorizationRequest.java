package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.client.AddCodeChallengeToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreateRandomCodeVerifier;
import net.openid.conformance.condition.client.CreateS256CodeChallenge;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class SetupPkceAndAddToAuthorizationRequest extends AbstractConditionSequence {
	@Override
	public void evaluate() {
		call(condition(CreateRandomCodeVerifier.class).requirement("RFC7636-4.1"));
		call(exec().exposeEnvironmentString("code_verifier"));
		call(condition(CreateS256CodeChallenge.class).requirement("RFC7636-4.2"));
		call(exec()
			.exposeEnvironmentString("code_challenge")
			.exposeEnvironmentString("code_challenge_method"));
		call(condition(AddCodeChallengeToAuthorizationEndpointRequest.class)
			.requirement("RFC7636-4.3"));

	}
}
