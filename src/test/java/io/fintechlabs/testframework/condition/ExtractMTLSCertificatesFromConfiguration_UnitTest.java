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

package io.fintechlabs.testframework.condition;

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

/**
 * @author jricher
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ExtractMTLSCertificatesFromConfiguration_UnitTest {

	
	@Spy
	private Environment env = new Environment();
	
	@Mock
	private TestInstanceEventLog eventLog;
	
	private ExtractMTLSCertificatesFromConfiguration cond;
	
	private String cert = "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";
	
	private String key = "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";
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		cond = new ExtractMTLSCertificatesFromConfiguration("UNIT-TEST", eventLog, ConditionResult.INFO);
		
		
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ExtractAuthorizationCodeFromAuthorizationResponse#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {
		
		JsonObject config = new JsonParser().parse("{\"mtls\":{"
				+ "\"cert\":\""+ cert + "\","
				+ "\"key\":\"" + key + "\""
				+ "}}").getAsJsonObject();

		env.put("config", config);
		
		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("config", "mtls.cert");
		verify(env, atLeastOnce()).getString("config", "mtls.key");
		
		assertThat(env.getString("mutual_tls_authentication", "cert")).isEqualTo(cert);
		assertThat(env.getString("mutual_tls_authentication", "key")).isEqualTo(key);
	}
	
	@Test(expected = ConditionError.class)
	public void testEvaluate_noKey() {
		
		JsonObject config = new JsonParser().parse("{\"mtls\":{"
				+ "\"cert\":\""+ cert + "\""
				+ "}}").getAsJsonObject();

		env.put("config", config);
		
		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("config", "mtls.cert");
		verify(env, atLeastOnce()).getString("config", "mtls.key");
		
		assertThat(env.getString("mutual_tls_authentication", "cert")).isEqualTo(cert);
		assertThat(env.getString("mutual_tls_authentication", "key")).isEqualTo(key);
	}
	
	@Test(expected = ConditionError.class)
	public void testEvaluate_badKey() {
		
		JsonObject config = new JsonParser().parse("{\"mtls\":{"
				+ "\"cert\":\""+ cert + "\","
				+ "\"key\":\"bad key value\""
				+ "}}").getAsJsonObject();

		env.put("config", config);
		
		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("config", "mtls.cert");
		verify(env, atLeastOnce()).getString("config", "mtls.key");
		
		assertThat(env.getString("mutual_tls_authentication", "cert")).isEqualTo(cert);
		assertThat(env.getString("mutual_tls_authentication", "key")).isEqualTo(key);
	}
	
	@Test(expected = ConditionError.class)
	public void testEvaluate_noCert() {
		
		JsonObject config = new JsonParser().parse("{\"mtls\":{"
				+ "\"key\":\"" + key + "\""
				+ "}}").getAsJsonObject();

		env.put("config", config);
		
		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("config", "mtls.cert");
		verify(env, atLeastOnce()).getString("config", "mtls.key");
		
		assertThat(env.getString("mutual_tls_authentication", "cert")).isEqualTo(cert);
		assertThat(env.getString("mutual_tls_authentication", "key")).isEqualTo(key);
	}
	
	@Test(expected = ConditionError.class)
	public void testEvaluate_badCert() {
		
		JsonObject config = new JsonParser().parse("{\"mtls\":{"
				+ "\"cert\":\"bad cert value\","
				+ "\"key\":\"" + key + "\""
				+ "}}").getAsJsonObject();

		env.put("config", config);
		
		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("config", "mtls.cert");
		verify(env, atLeastOnce()).getString("config", "mtls.key");
		
		assertThat(env.getString("mutual_tls_authentication", "cert")).isEqualTo(cert);
		assertThat(env.getString("mutual_tls_authentication", "key")).isEqualTo(key);
	}
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ExtractAuthorizationCodeFromAuthorizationResponse#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {
		
		cond.evaluate(env);
	}

}
