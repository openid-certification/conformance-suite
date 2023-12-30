package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.specto.hoverfly.junit.rule.HoverflyRule;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CallAccountRequestsEndpointWithBearerToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	// Examples from OpenBanking spec

	private static JsonObject requestObject = JsonParser.parseString("""
			{
			  "Data": {
			    "Permissions": [
			      "ReadAccountsDetail",
			      "ReadBalances",
			      "ReadBeneficiariesDetail",
			      "ReadDirectDebits",
			      "ReadProducts",
			      "ReadStandingOrdersDetail",
			      "ReadTransactionsCredits",
			      "ReadTransactionsDebits",
			      "ReadTransactionsDetail"
			    ],
			    "ExpirationDateTime": "2017-05-02T00:00:00+00:00",
			    "TransactionFromDateTime": "2017-05-03T00:00:00+00:00",
			    "TransactionToDateTime": "2017-12-03T00:00:00+00:00"
			  },
			  "Risk": {}
			}""").getAsJsonObject();

	private static JsonObject responseObject = JsonParser.parseString("""
			{
			  "Data": {
			    "AccountRequestId": "88379",
			    "Status": "AwaitingAuthorisation",
			    "CreationDateTime": "2017-05-02T00:00:00+00:00",
			    "Permissions": [
			      "ReadAccountsDetail",
			      "ReadBalances",
			      "ReadBeneficiariesDetail",
			      "ReadDirectDebits",
			      "ReadProducts",
			      "ReadStandingOrdersDetail",
			      "ReadTransactionsCredits",
			      "ReadTransactionsDebits",
			      "ReadTransactionsDetail"
			    ],
			    "ExpirationDateTime": "2017-08-02T00:00:00+00:00",
			    "TransactionFromDateTime": "2017-05-03T00:00:00+00:00",
			    "TransactionToDateTime": "2017-12-03T00:00:00+00:00"
			  },
			  "Risk": {},
			  "Links": {
			    "Self": "/account-requests/88379"
			  },
			  "Meta": {
			    "TotalPages": 1
			  }
			}""").getAsJsonObject();

	private static JsonObject bearerToken = JsonParser.parseString("{"
		+ "\"value\":\"2YotnFZFEjr1zCsicMWpAA\","
		+ "\"type\":\"Bearer\""
		+ "}").getAsJsonObject();

	private static JsonObject exampleToken = JsonParser.parseString("{"
		+ "\"value\":\"2YotnFZFEjr1zCsicMWpAA\","
		+ "\"type\":\"example\""
		+ "}").getAsJsonObject();

	@ClassRule
	public static HoverflyRule hoverfly = HoverflyRule.inSimulationMode(dsl(
		service("example.com")
			.post("/account-requests")
			.header("Authorization", "Bearer 2YotnFZFEjr1zCsicMWpAA")
			.anyBody()
			.willReturn(success(responseObject.toString(), "application/json"))));

	private CallAccountRequestsEndpointWithBearerToken cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		hoverfly.resetJournal();

		cond = new CallAccountRequestsEndpointWithBearerToken();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.putObject("resource", new JsonObject());
	}

	/**
	 * Test method for {@link CallAccountRequestsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putObject("access_token", bearerToken);
		env.getObject("resource").addProperty("resourceUrl", "http://example.com/");
		env.putObject("account_requests_endpoint_request", requestObject);

		env.putObject("resource_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		hoverfly.verify(service("example.com")
			.post("/account-requests")
			.header("Authorization", "Bearer 2YotnFZFEjr1zCsicMWpAA")
			.anyBody());

		verify(env, atLeastOnce()).getString("access_token", "value");
		verify(env, atLeastOnce()).getString("access_token", "type");
		verify(env, atLeastOnce()).getString("resource", "resourceUrl");

		assertThat(env.getObject("account_requests_endpoint_response")).isEqualTo(responseObject);
	}

	/**
	 * Test method for {@link CallAccountRequestsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badToken() {

		env.putObject("access_token", exampleToken);
		env.getObject("resource").addProperty("resourceUrl", "http://example.com/");
		env.putObject("account_requests_endpoint_request", requestObject);
		env.putObject("resource_endpoint_request_headers", new JsonObject());

		cond.execute(env);

	}

	/**
	 * Test method for {@link CallAccountRequestsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badServer() {

		env.putObject("access_token", bearerToken);
		env.getObject("resource").addProperty("resourceUrl", "http://invalid.org/");
		env.putObject("account_requests_endpoint_request", requestObject);
		env.putObject("resource_endpoint_request_headers", new JsonObject());

		cond.execute(env);

	}

	/**
	 * Test method for {@link CallAccountRequestsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingToken() {

		env.getObject("resource").addProperty("resourceUrl", "http://example.com/");
		env.putObject("account_requests_endpoint_request", requestObject);
		env.putObject("resource_endpoint_request_headers", new JsonObject());

		cond.execute(env);

	}

	/**
	 * Test method for {@link CallAccountRequestsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingUrl() {

		env.putObject("access_token", bearerToken);
		env.putObject("account_requests_endpoint_request", requestObject);
		env.putObject("resource_endpoint_request_headers", new JsonObject());

		cond.execute(env);

	}

	/**
	 * Test method for {@link CallAccountRequestsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingRequest() {

		env.putObject("access_token", bearerToken);
		env.getObject("resource").addProperty("resourceUrl", "http://example.com/");
		env.putObject("resource_endpoint_request_headers", new JsonObject());

		cond.execute(env);

	}

}
