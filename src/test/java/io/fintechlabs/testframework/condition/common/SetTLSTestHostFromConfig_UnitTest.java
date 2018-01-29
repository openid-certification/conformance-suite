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

package io.fintechlabs.testframework.condition.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class SetTLSTestHostFromConfig_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private SetTLSTestHostFromConfig cond;

	private JsonObject tlsConfig = new JsonParser().parse("{"
			+ "\"testHost\":\"example.com\","
			+ "\"testPort\":443"
			+ "}").getAsJsonObject();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new SetTLSTestHostFromConfig("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.SetTLSTestHostFromConfig#evaluate(io.fintechlabs.testframework.testmodule.Environment).
	 */
	@Test
	public void testEvaluate() {

		JsonObject config = new JsonObject();
		config.add("tls", tlsConfig);
		env.put("config", config);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("config", "tls.testHost");
		verify(env, atLeastOnce()).getInteger("config", "tls.testPort");

		assertThat(env.getString("tls", "testHost")).isEqualTo(tlsConfig.get("testHost").getAsString());
		assertThat(env.getString("tls", "testPort")).isEqualTo(tlsConfig.get("testPort").getAsString());
	}

}
