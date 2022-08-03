package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.util.UseResurce;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class AbstractCheckExpectedDateResponseTest extends AbstractJsonResponseConditionUnitTest {

	private CopyResourceEndpointResponse copyResourceEndpointResponse = new CopyResourceEndpointResponse();

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardTransactionsCurrentV2/cardTransactionsCurrentResponse.json")
	public void checkCreditCardTransactionsCurrentResponse() {
		CheckExpectedTransactionDateMaxLimitedResponse condition = new CheckExpectedTransactionDateMaxLimitedResponse();
		environment.putString("fromTransactionDateMaxLimited", "2021-05-01");
		environment.putString("toTransactionDateMaxLimited", "2021-06-01");
		copyAndAddFullRangeResponseToEnvironment("jsonResponses/creditCard/creditCardV2/cardTransactionsCurrentV2/cardTransactionsCurrentFullRangeResponse.json");
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/account/transactions/accountTransactionsResponseOK.json")
	public void checkAccountsTransactionV1Response() {
		CheckExpectedBookingDateResponse condition = new CheckExpectedBookingDateResponse();
		environment.putString("fromBookingDate", "2021-01-01");
		environment.putString("toBookingDate", "2021-02-01");
		copyAndAddFullRangeResponseToEnvironment("jsonResponses/account/transactions/accountTransactionsFullRangeResponseOK.json");
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/transactionsV2/accountTransactionsResponseOK.json")
	public void checkAccountsTransactionV2Response() {
		CheckExpectedBookingDateResponse condition = new CheckExpectedBookingDateResponse();
		environment.putString("fromBookingDate", "2021-01-01");
		environment.putString("toBookingDate", "2021-02-01");
		copyAndAddFullRangeResponseToEnvironment("jsonResponses/account/accountV2/transactionsV2/accountTransactionsFullRangeResponseOK.json");
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/transactionsV2/accountTransactionsResponseOK.json")
	public void checkAccountsTransactionCurrentResponse() {
		CheckExpectedBookingDateMaxLimitedResponse condition = new CheckExpectedBookingDateMaxLimitedResponse();
		environment.putString("fromBookingDateMaxLimited", "2021-01-01");
		environment.putString("toBookingDateMaxLimited", "2021-02-01");
		copyAndAddFullRangeResponseToEnvironment("jsonResponses/account/accountV2/transactionsV2/accountTransactionsFullRangeResponseOK.json");
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardTransactionsCurrentV2/cardTransactionsCurrentResponse.json")
	public void checkCreditCardTransactionsCurrentCheatResponse() {
		CheckExpectedTransactionDateMaxLimitedResponse condition = new CheckExpectedTransactionDateMaxLimitedResponse();
		environment.putString("fromTransactionDateMaxLimited", "2021-06-01");
		environment.putString("toTransactionDateMaxLimited", "2021-07-01");
		copyAndAddFullRangeResponseToEnvironment("jsonResponses/creditCard/creditCardV2/cardTransactionsCurrentV2/cardTransactionsCurrentFullRangeResponse.json");
		ConditionError conditionError = runAndFail(condition);
		assertThat(conditionError.getMessage(), containsString("The returned data array is not what was expected"));
	}


	private void copyAndAddFullRangeResponseToEnvironment(String path) {
		try {
			String fullResponseJson = IOUtils.resourceToString(path, StandardCharsets.UTF_8, getClass().getClassLoader());
			environment.putString("resource_endpoint_response_full", "body", fullResponseJson);
			run(copyResourceEndpointResponse);
			environment.mapKey("full_range_response", "resource_endpoint_response_full_copy");
		} catch (IOException e) {
			throw new AssertionError("Could not load resource");
		}
	}


}
