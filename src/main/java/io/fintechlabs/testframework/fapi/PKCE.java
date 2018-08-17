package io.fintechlabs.testframework.fapi;

import java.util.ArrayList;
import java.util.List;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddCodeChallengeToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreatePlainCodeChallenge;
import io.fintechlabs.testframework.condition.client.CreateRandomCodeVerifier;
import io.fintechlabs.testframework.condition.client.CreateS256CodeChallenge;
import io.fintechlabs.testframework.testmodule.ConditionCallBuilder;
import io.fintechlabs.testframework.testmodule.TestExecutionBuilder;
import io.fintechlabs.testframework.testmodule.TestExecutionUnit;

/**
 * @author jricher
 *
 */
public class PKCE {

	/**
	 * Create a new condition call builder, which can be passed to call()
	 */
	// TODO: abstract this into another class
	protected static ConditionCallBuilder condition(Class<? extends Condition> conditionClass) {
		return new ConditionCallBuilder(conditionClass);
	}

	/**
	 * Create a new test execution builder, which can be passed to call()
	 */
	// TODO: abstract this into another class
	protected static TestExecutionBuilder exec() {
		return new TestExecutionBuilder();
	}

	public static List<TestExecutionUnit> createS256ChallengeAndAddtoAuthorizationEndpointRequest() {

		List<TestExecutionUnit> calls = new ArrayList<>();

		calls.add(condition(CreateRandomCodeVerifier.class));
		calls.add(exec().exposeEnvironmentString("code_verifier"));
		calls.add(condition(CreateS256CodeChallenge.class));
		calls.add(exec()
			.exposeEnvironmentString("code_challenge")
			.exposeEnvironmentString("code_challenge_method"));
		calls.add(condition(AddCodeChallengeToAuthorizationEndpointRequest.class)
			.requirement("FAPI-1-5.2.2-7"));

		return calls;
	}

	public static List<TestExecutionUnit> createPlainChallengeAndAddtoAuthorizationEndpointRequest() {

		List<TestExecutionUnit> calls = new ArrayList<>();

		calls.add(condition(CreateRandomCodeVerifier.class));
		calls.add(exec().exposeEnvironmentString("code_verifier"));
		calls.add(condition(CreatePlainCodeChallenge.class));
		calls.add(exec()
			.exposeEnvironmentString("code_challenge")
			.exposeEnvironmentString("code_challenge_method"));
		calls.add(condition(AddCodeChallengeToAuthorizationEndpointRequest.class)
			.requirement("FAPI-1-5.2.2-7"));

		return calls;
	}
}
