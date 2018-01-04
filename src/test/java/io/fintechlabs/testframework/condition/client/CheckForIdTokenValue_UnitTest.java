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
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.CheckForIdTokenValue;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckForIdTokenValue_UnitTest {

	@Spy
	private Environment env = new Environment();
	
	@Mock
	private TestInstanceEventLog eventLog;
	
	private JsonObject goodResponse;
	
	private JsonObject badResponse;
	
	private CheckForIdTokenValue cond;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		cond = new CheckForIdTokenValue("UNIT-TEST", eventLog, ConditionResult.INFO);
		
		goodResponse = new JsonParser().parse("{"
				+ "\"access_token\":"
				+ "\"eyJraWQiOiJyc2ExIiwiYWxnIjoiUlMyNTYifQ."
				+ "eyJzdWIiOiJhZG1pbiIsImF6cCI6ImNsaWVudCIsImlzcyI6Imh0dHBzOlwvXC9taXRyZWlkLm9yZ1wvIiwiZXhwIjoxNTA2NDU5NzU3LCJpYXQiOjE1MDY0NTYxNTcsImp0aSI6Ijc3MmJiODBhLWQ2ZTEtNGUwMi1hN2M3LTAxNWMwZGQyMjdkNSJ9."
				+ "OsuhXSzoODk6VdUjPSyq9vp_ZLqgNczSF1Gvz8-wh1mGdN1aadWy2faKyqIXoQ7OrOycNkOigmyhYoGYoqM30SmkSm9bmpoTOxCAQDclPRdTjmV_-cF9bRLTvaz-78xOcotk4c9kJQHPvUdXiistvnUywME80rKr5K4m4oAYrv-Ii6ntXK6Wbatx3wjEE8Erkq1dkBO9FKCjqIpVeTM-106uFD6yw4sRZZOgY9ibzovp9522l3VkjaU88UEjTu7WbSaLLyaEgiBVSDoweTogEzI-BIZhjtkaxiDzgKvXUJFF8jxyU-2YB4wgs5oi6SUkWJk825I3Lk2v4EZjE3pZag\","
				+ "\"token_type\":\"Bearer\","
				+ "\"expires_in\":3599,"
				+ "\"scope\":\"address phone openid email profile\","
				+ "\"id_token\":\"eyJraWQiOiJyc2ExIiwiYWxnIjoiUlMyNTYifQ."
				+ "eyJzdWIiOiI5MDM0Mi5BU0RGSldGQSIsImF1ZCI6ImNsaWVudCIsImF1dGhfdGltZSI6MTUwNjQ1NDIyNywia2lkIjoicnNhMSIsImlzcyI6Imh0dHBzOlwvXC9taXRyZWlkLm9yZ1wvIiwiZXhwIjoxNTA2NDU2NzU3LCJpYXQiOjE1MDY0NTYxNTcsImp0aSI6IjM2YzU4M2M5LTE1M2UtNDBhOC05M2MzLWEzNWZkYTgwM2QzOCJ9."
				+ "SuGDMrdIh_tCsoWt51xs7rs036KAL3OcwdTcJxmuEUT24EufZILQ9_2rNX8BLA9S-YwYkS243oFz9UfBmnnqj6H27-BO7yBSwGnofDwV6GN4yLXmJfrzC6EEvSPkYMnHo7ha2eIUDFEHcTuKg1eSyKvkaPhklg3R5QHl4xo43FnKfQ8TrhAEH07FNKGFVS67xr00a17OD8VNn3LlZISr-iVbaueNBeYD9obUEmL5IJR8Y37qNK4egirn41BXQKK7xguF2nebQpN-1lcewW5OnEWy7yGd7M88l-WVzfNyFCM75bKZFAbv_W2w1glh38M2DJNRbe2SJhMxkpMxwUeLBA\""
				+ "}").getAsJsonObject();
		
		badResponse = new JsonParser().parse("{"
				+ "\"access_token\":"
				+ "\"eyJraWQiOiJyc2ExIiwiYWxnIjoiUlMyNTYifQ."
				+ "eyJzdWIiOiJhZG1pbiIsImF6cCI6ImNsaWVudCIsImlzcyI6Imh0dHBzOlwvXC9taXRyZWlkLm9yZ1wvIiwiZXhwIjoxNTA2NDU5NzU3LCJpYXQiOjE1MDY0NTYxNTcsImp0aSI6Ijc3MmJiODBhLWQ2ZTEtNGUwMi1hN2M3LTAxNWMwZGQyMjdkNSJ9."
				+ "OsuhXSzoODk6VdUjPSyq9vp_ZLqgNczSF1Gvz8-wh1mGdN1aadWy2faKyqIXoQ7OrOycNkOigmyhYoGYoqM30SmkSm9bmpoTOxCAQDclPRdTjmV_-cF9bRLTvaz-78xOcotk4c9kJQHPvUdXiistvnUywME80rKr5K4m4oAYrv-Ii6ntXK6Wbatx3wjEE8Erkq1dkBO9FKCjqIpVeTM-106uFD6yw4sRZZOgY9ibzovp9522l3VkjaU88UEjTu7WbSaLLyaEgiBVSDoweTogEzI-BIZhjtkaxiDzgKvXUJFF8jxyU-2YB4wgs5oi6SUkWJk825I3Lk2v4EZjE3pZag\","
				+ "\"token_type\":\"Bearer\","
				+ "\"expires_in\":3599,"
				+ "\"scope\":\"address phone openid email profile\""
				+ "}").getAsJsonObject();
		
		
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckForIdTokenValue#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {
		
		env.put("token_endpoint_response", goodResponse);

		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("token_endpoint_response", "id_token");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckForIdTokenValue#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {
		
		env.put("token_endpoint_response", badResponse);

		cond.evaluate(env);
		
	}
}
