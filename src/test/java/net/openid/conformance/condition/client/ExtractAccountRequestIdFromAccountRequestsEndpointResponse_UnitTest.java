package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ExtractAccountRequestIdFromAccountRequestsEndpointResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject endpointResponse;

	private ExtractAccountRequestIdFromAccountRequestsEndpointResponse cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ExtractAccountRequestIdFromAccountRequestsEndpointResponse();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Example from OpenBanking spec
		endpointResponse = JsonParser.parseString("""
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

	}

	/**
	 * Test method for {@link ExtractAccountRequestIdFromAccountRequestsEndpointResponse#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("account_requests_endpoint_response", endpointResponse);
		env.putInteger("ob_api_version", 2);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("account_requests_endpoint_response", "Data.AccountRequestId");

		assertThat(env.getString("account_request_id")).isEqualTo("88379");

	}

	/**
	 * Test method for {@link ExtractAccountRequestIdFromAccountRequestsEndpointResponse#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		env.putObject("account_requests_endpoint_response", new JsonObject());

		cond.execute(env);

	}

}
