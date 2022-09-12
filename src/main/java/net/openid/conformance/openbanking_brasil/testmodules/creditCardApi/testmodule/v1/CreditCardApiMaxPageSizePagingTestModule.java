package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.testmodule.v1;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.openbanking_brasil.*;
import net.openid.conformance.openbanking_brasil.creditCard.v1.CreditCardAccountsTransactionResponseValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.PrepareUrlForFetchingCardTransactions;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@PublishTestModule(
	testName = "credit-card-api-max-page-size-paging-test",
	displayName = "Test result set paging: Banks should configure a test credit card transactions list which contains their maximum page-size + 1 items (maximum page-size must be between 25 and 1000). For example, if the bank support a maximum page-size of 50, then they must setup a test resource with at least 51 items. The initial request should receive a response with 50 items. Requesting the 'next' link, found in the metadata, should receive a response with at least 1 item",
	summary = "Test result set paging: Banks should configure a test credit card transactions list that contains their maximum page-size + 1 item (maximum page-size must be between 25 and 1000). For example, if the bank supports a maximum page-size of 50, then they must set up a test resource with at least 51 items. The initial request should receive a response with 50 items. Requesting the 'next' link, found in the metadata, should receive a response with at least 1 item\n" +
		"\u2022 Creates a Consent with the complete set of the credit cards permission group ([\"CREDIT_CARDS_ACCOUNTS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ\", \"CREDIT_CARDS_ACCOUNTS_LIMITS_READ\", \"CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ\", \"RESOURCES_READ\"])\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Accounts API \n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Accounts API with page size=1000\n" +
		"\u2022 Expects a 200 response and expect that the links and meta attributes display the next page \n" +
		"\u2022 Validates the number of records in first page and compares it to the page-size returned \n" +
		"\u2022 Validates the number of records in second page and compares it to the page-size returned \n" +
		"\u2022 Checks the total records parameter and compares it with the number of records in the first and second pages",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf"
	}
)
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class CreditCardApiMaxPageSizePagingTestModule extends AbstractOBBrasilFunctionalTestModule {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildCreditCardsAccountsConfigResourceUrlFromConsentUrl.class);
		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
		env.putString("fromTransactionDate", currentDate.minusDays(360).format(FORMATTER));
		env.putString("toTransactionDate", currentDate.format(FORMATTER));
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareAllCreditCardRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(AddCreditCardScopes.class);
	}

	@Override
	protected void validateResponse() {

		preCallProtectedResource("Prepare to Fetch Credit Card Transactions");
		callAndStopOnFailure(CardAccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingCardTransactions.class);
		env.putInteger("required_page_size", 1000);
		callAndStopOnFailure(AddSpecifiedPageSizeParameterToProtectedResourceUrl.class);
		callAndStopOnFailure(AddToAndFromTransactionDateParametersToProtectedResourceUrl.class);
		callAndStopOnFailure(SetResourceMethodToGet.class);
		callAndStopOnFailure(ClearContentTypeHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(CallProtectedResource.class);
		callAndStopOnFailure(EnsureResponseCodeWas200.class);
		callAndContinueOnFailure(CreditCardAccountsTransactionResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateNumberOfRecordsPage1.class, Condition.ConditionResult.FAILURE);

		preCallProtectedResource("Prepare to Fetch page 2 of Credit Card Tansactions");
		callAndStopOnFailure(ClearRequestObjectFromEnvironment.class);
		callAndContinueOnFailure(SetProtectedResourceUrlToNextEndpoint.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(SetResourceMethodToGet.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ClearContentTypeHeaderForResourceEndpointRequest.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(CreditCardAccountsTransactionResponseValidator.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateNumberOfRecordsPage2.class, Condition.ConditionResult.FAILURE);

	}

}
