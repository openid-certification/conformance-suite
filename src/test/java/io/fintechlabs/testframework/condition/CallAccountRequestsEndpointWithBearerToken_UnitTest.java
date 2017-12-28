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

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.specto.hoverfly.junit.rule.HoverflyRule;

@RunWith(MockitoJUnitRunner.class)
public class CallAccountRequestsEndpointWithBearerToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	// Examples from OpenBanking spec

	private static JsonObject requestObject = new JsonParser().parse("{\n" + 
			"  \"Data\": {\n" + 
			"    \"Permissions\": [\n" + 
			"      \"ReadAccountsDetail\",\n" + 
			"      \"ReadBalances\",\n" + 
			"      \"ReadBeneficiariesDetail\",\n" + 
			"      \"ReadDirectDebits\",\n" + 
			"      \"ReadProducts\",\n" + 
			"      \"ReadStandingOrdersDetail\",\n" + 
			"      \"ReadTransactionsCredits\",\n" + 
			"      \"ReadTransactionsDebits\",\n" + 
			"      \"ReadTransactionsDetail\"\n" + 
			"    ],\n" + 
			"    \"ExpirationDateTime\": \"2017-05-02T00:00:00+00:00\",\n" + 
			"    \"TransactionFromDateTime\": \"2017-05-03T00:00:00+00:00\",\n" + 
			"    \"TransactionToDateTime\": \"2017-12-03T00:00:00+00:00\"\n" + 
			"  },\n" + 
			"  \"Risk\": {}\n" + 
			"}").getAsJsonObject();

	private static JsonObject responseObject = new JsonParser().parse("{\n" + 
			"  \"Data\": {\n" + 
			"    \"AccountRequestId\": \"88379\",\n" + 
			"    \"Status\": \"AwaitingAuthorisation\",\n" + 
			"    \"CreationDateTime\": \"2017-05-02T00:00:00+00:00\",\n" + 
			"    \"Permissions\": [\n" + 
			"      \"ReadAccountsDetail\",\n" + 
			"      \"ReadBalances\",\n" + 
			"      \"ReadBeneficiariesDetail\",\n" + 
			"      \"ReadDirectDebits\",\n" + 
			"      \"ReadProducts\",\n" + 
			"      \"ReadStandingOrdersDetail\",\n" + 
			"      \"ReadTransactionsCredits\",\n" + 
			"      \"ReadTransactionsDebits\",\n" + 
			"      \"ReadTransactionsDetail\"\n" + 
			"    ],\n" + 
			"    \"ExpirationDateTime\": \"2017-08-02T00:00:00+00:00\",\n" + 
			"    \"TransactionFromDateTime\": \"2017-05-03T00:00:00+00:00\",\n" + 
			"    \"TransactionToDateTime\": \"2017-12-03T00:00:00+00:00\"\n" + 
			"  },\n" + 
			"  \"Risk\": {},\n" + 
			"  \"Links\": {\n" + 
			"    \"Self\": \"/account-requests/88379\"\n" + 
			"  },\n" + 
			"  \"Meta\": {\n" + 
			"    \"TotalPages\": 1\n" + 
			"  }\n" + 
			"}").getAsJsonObject();

	private static JsonObject bearerToken = new JsonParser().parse("{"
			+ "\"value\":\"2YotnFZFEjr1zCsicMWpAA\","
			+ "\"type\":\"Bearer\""
			+ "}").getAsJsonObject();

	private static JsonObject exampleToken = new JsonParser().parse("{"
			+ "\"value\":\"2YotnFZFEjr1zCsicMWpAA\","
			+ "\"type\":\"example\""
			+ "}").getAsJsonObject();

	@ClassRule
	public static HoverflyRule hoverfly = HoverflyRule.inSimulationMode(dsl(
		service("example.com")
			.post("/account-requests")
			.header("Authorization", "Bearer 2YotnFZFEjr1zCsicMWpAA")
			.anyBody()
			.willReturn(success(responseObject.toString(), "application/json"))
	));

	private CallAccountRequestsEndpointWithBearerToken cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		hoverfly.resetJournal();

		cond = new CallAccountRequestsEndpointWithBearerToken("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.put("resource", new JsonObject());
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.CallAccountRequestsEndpointWithBearerToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.put("access_token", bearerToken);
		env.get("resource").addProperty("resourceUrl", "http://example.com/");
		env.put("account_requests_endpoint_request", requestObject);

		cond.evaluate(env);

		hoverfly.verify(service("example.com")
				.post("/account-requests")
				.header("Authorization", "Bearer 2YotnFZFEjr1zCsicMWpAA")
				.anyBody());

		verify(env, atLeastOnce()).getString("access_token", "value");
		verify(env, atLeastOnce()).getString("access_token", "type");
		verify(env, atLeastOnce()).getString("resource", "resourceUrl");

		assertThat(env.get("account_requests_endpoint_response")).isEqualTo(responseObject);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.CallAccountRequestsEndpointWithBearerToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badToken() {

		env.put("access_token", exampleToken);
		env.get("resource").addProperty("resourceUrl", "http://example.com/");
		env.put("account_requests_endpoint_request", requestObject);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.CallAccountRequestsEndpointWithBearerToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badServer() {

		env.put("access_token", bearerToken);
		env.get("resource").addProperty("resourceUrl", "http://invalid.org/");
		env.put("account_requests_endpoint_request", requestObject);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.CallAccountRequestsEndpointWithBearerToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingToken() {

		env.get("resource").addProperty("resourceUrl", "http://example.com/");
		env.put("account_requests_endpoint_request", requestObject);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.CallAccountRequestsEndpointWithBearerToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingUrl() {

		env.put("access_token", bearerToken);
		env.put("account_requests_endpoint_request", requestObject);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.CallAccountRequestsEndpointWithBearerToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingRequest() {

		env.put("access_token", bearerToken);
		env.get("resource").addProperty("resourceUrl", "http://example.com/");

		cond.evaluate(env);

	}

}
