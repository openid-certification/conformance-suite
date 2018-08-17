/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.fapi;

import java.util.ArrayList;
import java.util.List;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddCodeChallengeToAuthorizationEndpointRequest;
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

	public static List<TestExecutionUnit> createChallenge() {

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
}
