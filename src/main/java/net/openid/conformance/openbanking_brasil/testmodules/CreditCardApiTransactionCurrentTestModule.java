package net.openid.conformance.openbanking_brasil.testmodules;


import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "credit-card-api-transactions-current-test",
	displayName = "Test that the server has correctly implemented the current transactions resource",
	summary = "Test that the server has correctly implemented the current transactions resource\n" +
		"\u2022 Creates a consent with only Credit Cards permissions\n" +
		"\u2022 201 code and successful redirect\n" +
		"\u2022 Using the consent created, call the Credit Cards API\n" +
		"\u2022 Call the GET Credit Cards API\n" +
		"\u2022 Expect OK 200 - Validate all fields of the API - Fetch the first returned account ids to be used later on the test to get its transactions\n" +
		"\u2022 Call the GET Current Credit Cards Transactions API\n" +
		"\u2022 Expect OK 200 - Validate all fields of the API - Make sure if one transaction is found it has today’s date on it\n" +
		"\u2022 Call the GET Current Credit Cards Transactions API, send query parameters fromTransactionDateMaxLimited and toTransactionDateMaxLimited using the max period ( D-6 should be the from and D should be today)\n" +
		"\u2022 Expect OK 200 - Validate all fields of the API - Make sure if transactions are found that none of them are more than 1 week older\n" +
		"\u2022 Call the GET Current Credit Cards Transactions API, send query parameters fromTransactionDateMaxLimited and toTransactionDateMaxLimited using a period that is over the expected valid period. Both from and to booking date should be D-30 to D-20 for example\n" +
		"\u2022 Expect 422 Unprocessable Entity\n",
	profile = OBBProfile.OBB_PROFIlE_PHASE2,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.resourceUrl",
		"consent.productType"
	}
)
public class CreditCardApiTransactionCurrentTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void validateResponse() {

	}

}
