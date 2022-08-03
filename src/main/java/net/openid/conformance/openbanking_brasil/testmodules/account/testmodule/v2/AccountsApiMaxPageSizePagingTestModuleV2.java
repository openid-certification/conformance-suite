package net.openid.conformance.openbanking_brasil.testmodules.account.testmodule.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.account.v2.AccountTransactionsValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.account.BuildAccountsConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.account.PrepareAllAccountRelatedConsentsForHappyPathTest;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateMetaOnlyRequestDateTime;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@PublishTestModule(
	testName = "account-api-max-page-size-paging-test-v2",
	displayName = "Test result set paging: Banks should configure a test accounts list which contains their maximum page-size + 1 items (maximum page-size must be between 25 and 1000). For example, if the bank support a maximum page-size of 50, then they must setup a test resource with at least 51 items. The initial request should receive a response with 50 items. Requesting the 'next' link, found in the metadata, should receive a response with at least 1 item",
	summary = "Test result set paging: Banks should configure a test accounts list that contains their maximum page-size + 1 item (maximum page-size must be between 25 and 1000). For example, if the bank supports a maximum page-size of 50, then they must set up a test resource with at least 51 items. The initial request should receive a response with 50 items. Requesting the 'next' link, found in the metadata, should receive a response with at least 1 item.\n" +
		"\u2022 Creates a Consent with the complete set of the accounts permission group (\"ACCOUNTS_READ\", \"ACCOUNTS_TRANSACTIONS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Accounts API V2\n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Accounts API V2 with page size=1000\n" +
		"\u2022 Expects a 200 response and expect that the links and meta attributes display the next page \n" +
		"\u2022 Validates the number of records being return",
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
public class AccountsApiMaxPageSizePagingTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
		env.putString("fromBookingDate", currentDate.minusDays(360).format(FORMATTER));
		env.putString("toBookingDate", currentDate.format(FORMATTER));
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddAccountScope.class);
		callAndStopOnFailure(PrepareAllAccountRelatedConsentsForHappyPathTest.class);
	}

	@Override
	protected void validateResponse() {

		preCallProtectedResource("Prepare to Fetch Account Transactions V2");
		callAndStopOnFailure(AccountSelector.class);
		callAndStopOnFailure(SetProtectedResourceUrlTransactionsPageSize1000.class);
		callAndStopOnFailure(AppendToAndFromBookingDateParametersToProtectedResourceUrl.class);
		callAndStopOnFailure(SetResourceMethodToGet.class);
		callAndStopOnFailure(ClearContentTypeHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(CallProtectedResource.class);
		callAndStopOnFailure(EnsureResponseCodeWas200.class);
		callAndContinueOnFailure(AccountTransactionsValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateNumberOfRecordsPage1.class, Condition.ConditionResult.FAILURE);

		preCallProtectedResource("Prepare to Fetch page 2 of Account Transactions V2");
		callAndStopOnFailure(ClearRequestObjectFromEnvironment.class);
		callAndStopOnFailure(SetProtectedResourceUrlToNextEndpoint.class);
		callAndStopOnFailure(SetResourceMethodToGet.class);
		callAndStopOnFailure(ClearContentTypeHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(CallProtectedResource.class);
		callAndStopOnFailure(EnsureResponseCodeWas200.class);
		callAndContinueOnFailure(AccountTransactionsValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateMetaOnlyRequestDateTime.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateNumberOfRecordsPage2.class, Condition.ConditionResult.FAILURE);

	}

}
