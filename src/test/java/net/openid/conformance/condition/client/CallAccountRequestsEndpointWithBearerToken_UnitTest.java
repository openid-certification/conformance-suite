package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit5.HoverflyExtension;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(HoverflyExtension.class)
@ExtendWith(MockitoExtension.class)
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

	private CallAccountRequestsEndpointWithBearerToken cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp(Hoverfly hoverfly) throws Exception {

		hoverfly.simulate(dsl(
			service("example.com")
				.post("/account-requests")
				.header("Authorization", "Bearer 2YotnFZFEjr1zCsicMWpAA")
				.anyBody()
				.willReturn(success(responseObject.toString(), "application/json"))));
		hoverfly.resetJournal();

		cond = new CallAccountRequestsEndpointWithBearerToken();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.putObject("resource", new JsonObject());
	}

	/**
	 * Test method for {@link CallAccountRequestsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError(Hoverfly hoverfly) {

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
	@Test
	public void testEvaluate_badToken() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("access_token", exampleToken);
			env.getObject("resource").addProperty("resourceUrl", "http://example.com/");
			env.putObject("account_requests_endpoint_request", requestObject);
			env.putObject("resource_endpoint_request_headers", new JsonObject());

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link CallAccountRequestsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_badServer() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("access_token", bearerToken);
			env.getObject("resource").addProperty("resourceUrl", "http://invalid.org/");
			env.putObject("account_requests_endpoint_request", requestObject);
			env.putObject("resource_endpoint_request_headers", new JsonObject());

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link CallAccountRequestsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingToken() {
		assertThrows(ConditionError.class, () -> {

			env.getObject("resource").addProperty("resourceUrl", "http://example.com/");
			env.putObject("account_requests_endpoint_request", requestObject);
			env.putObject("resource_endpoint_request_headers", new JsonObject());

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link CallAccountRequestsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingUrl() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("access_token", bearerToken);
			env.putObject("account_requests_endpoint_request", requestObject);
			env.putObject("resource_endpoint_request_headers", new JsonObject());

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link CallAccountRequestsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingRequest() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("access_token", bearerToken);
			env.getObject("resource").addProperty("resourceUrl", "http://example.com/");
			env.putObject("resource_endpoint_request_headers", new JsonObject());

			cond.execute(env);

		});

	}

}
