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

package io.fintechlabs.testframework.condition.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class EnsureMinimumTokenLength_UnitTest {


	@Spy
	private Environment env = new Environment();
	
	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureMinimumTokenLength cond;
	
	@Before
	public void setUp() throws Exception {
		
		cond = new EnsureMinimumTokenLength("UNIT-TEST", eventLog, ConditionResult.INFO);

	}
	
	@Test
	public void testEvaluate_entropyGood() {
		JsonObject o = new JsonObject();
		o.addProperty("access_token", "aQm0ukLetSUOXr1XA8RLHdeO9eFdoBGF8Sn1UhP9");
		env.put("token_endpoint_response", o);
		
		cond.evaluate(env);
	}
	
	@Test(expected = ConditionError.class)
	public void testEvaluate_entropyBad() {
		JsonObject o = new JsonObject();
		o.addProperty("access_token", "aQm0ukLetSUOXr1");
		env.put("token_endpoint_response", o);
		
		cond.evaluate(env);		
	}
	
}
