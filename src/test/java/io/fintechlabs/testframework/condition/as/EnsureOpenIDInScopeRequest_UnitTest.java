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

package io.fintechlabs.testframework.condition.as;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class EnsureOpenIDInScopeRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureOpenIDInScopeRequest cond;

	private String goodScope = "openid foo bar";

	private String onlyScope = "openid";

	private String badScope = "foo bar";

	private String emptyScope = "";

	@Before
	public void setUp() throws Exception {

		cond = new EnsureOpenIDInScopeRequest("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void test_good() {

		env.putString("scope", goodScope);
		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("scope");
	}

	@Test
	public void test_only() {

		env.putString("scope", onlyScope);
		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("scope");

	}

	@Test(expected = ConditionError.class)
	public void test_bad() {

		env.putString("scope", badScope);
		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("scope");

	}

	@Test(expected = ConditionError.class)
	public void test_empty() {

		env.putString("scope", emptyScope);
		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("scope");

	}
}
