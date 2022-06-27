package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.testmodule.v1;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.openbanking_brasil.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "credit-card-api-page-size-test",
	displayName = "Validate that we can request credit card accounts with a page-size specified",
	summary = "Validates that we can request credit card accounts with a page-size specified\n" +
		"\u2022 Creates a Consent with the complete set of the credit cards permission group ([\"CREDIT_CARDS_ACCOUNTS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ\", \"CREDIT_CARDS_ACCOUNTS_LIMITS_READ\", \"CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ\", \"RESOURCES_READ\"])\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Credit Cards Accounts API \n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Credit Cards Transactions API with page size=1000\n" +
		"\u2022 Expects a 200 response - Validates that the answer is compliant with the paging rules set",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",

	}
)
public class CreditCardApiPageSizeTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildCreditCardsAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareAllCreditCardRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(AddCreditCardScopes.class);
	}

	@Override
	protected void validateResponse() {
		callAndStopOnFailure(ClearRequestObjectFromEnvironment.class);
		callAndStopOnFailure(SetProtectedResourceUrlPageSize1000.class);
		callAndStopOnFailure(SetResourceMethodToGet.class);
		callAndStopOnFailure(ClearContentTypeHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(CallProtectedResource.class);
		callAndStopOnFailure(EnsureResponseCodeWas200.class);
		callAndStopOnFailure(EnsureResponseHasLinks.class);
		callAndStopOnFailure(ValidateResponseMetaData.class);

	}

}
