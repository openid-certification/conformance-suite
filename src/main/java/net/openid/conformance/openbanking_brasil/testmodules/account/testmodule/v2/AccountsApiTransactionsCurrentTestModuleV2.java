package net.openid.conformance.openbanking_brasil.testmodules.account.testmodule.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.account.v2.AccountListValidatorV2;
import net.openid.conformance.openbanking_brasil.account.v2.AccountTransactionsValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.account.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateMetaOnlyRequestDateTime;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@PublishTestModule(
	testName = "account-api-transactions-current-test-v2",
	displayName = "Test that the server has correctly implemented the current transactions resource V2",
	summary = "Test that the server has correctly implemented the current transactions resource V@\n" +
		"\u2022 Creates a consent with only Accounts permissions\n" +
		"\u2022 Expect - 201 code and successful redirect\n" +
		"\u2022 Using the consent created, call the Accounts API V2\n" +
		"\u2022 Call the GET Accounts API V2\n" +
		"\u2022 Expect OK 200 - Validate all fields of the API - Fetch the first returned account ids to be used on the transactions API Call\n" +
		"\u2022 Call the GET Current Accounts Transactions API V2\n" +
		"\u2022 Expect OK 200 - Validate all fields of the API - Make sure that any transaction returned have today’s date on it - Test can also expect an empty list\n" +
		"\u2022 Call the GET Current Accounts Transactions API V2, send query parameters fromBookingDate and toBookingDate using the max 7 day period\n" +
		"\u2022 Expect OK 200 - Validate all fields of the API - Make sure if transactions are found that none of them are more than 1 week older\n" +
		"\u2022 Call the GET Current Accounts Transactions API V2, send query parameters fromBookingDate and toBookingDate using a period that is not over the expected valid period\n" +
		"\u2022 Expect 422 Unprocessable Entity\n",
	profile = OBBProfile.OBB_PROFIlE_PHASE2_VERSION2,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"consent.productType"
	}
)
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class AccountsApiTransactionsCurrentTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final String FROM_BOOKING_DATE_MAX_LIMITED = "fromBookingDateMaxLimited";
	private static final String TO_BOOKING_DATE_MAX_LIMITED = "toBookingDateMaxLimited";

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(AccountListValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(AccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingCurrentAccountTransactions.class);

//		 Call without parameters
		runInBlock("Fetch Account Current transactions V2", () -> call(getPreCallProtectedResourceSequence()));
		runInBlock("Validate Account Current Transactions V2",
			() -> call(getValidationSequence()
				.then(condition(EnsureTransactionsDateIsSetToToday.class)))
		);

		// Call with full range parameters | Anti Cheat
		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
		env.putString(FROM_BOOKING_DATE_MAX_LIMITED, currentDate.minusDays(6).format(FORMATTER));
		env.putString(TO_BOOKING_DATE_MAX_LIMITED, currentDate.format(FORMATTER));

		callAndStopOnFailure(PrepareUrlForFetchingCurrentAccountTransactions.class);
		callAndStopOnFailure(AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl.class);
		runInBlock("Fetch Account Current transactions with full range date parameters",
			() -> call(getPreCallProtectedResourceSequence()
				.then(condition(CopyResourceEndpointResponse.class)))
		);
		env.mapKey("full_range_response", "resource_endpoint_response_full_copy");

		// Call with valid  parameters
		env.putString(FROM_BOOKING_DATE_MAX_LIMITED, currentDate.minusDays(5).format(FORMATTER));
		env.putString(TO_BOOKING_DATE_MAX_LIMITED, currentDate.format(FORMATTER));

		callAndStopOnFailure(PrepareUrlForFetchingCurrentAccountTransactions.class);
		callAndStopOnFailure(AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl.class);
		runInBlock("Fetch Account Current transactions with valid date parameters", () -> call(getPreCallProtectedResourceSequence()));
		runInBlock("Validate Account Current Transactions",
			() -> call(getValidationSequence()
				.then(condition(CheckExpectedBookingDateMaxLimitedResponse.class))
				.then(condition(EnsureTransactionsDateIsNoOlderThan7Days.class)))
		);

		// Call with invalid  parameters
		env.putString(FROM_BOOKING_DATE_MAX_LIMITED, currentDate.minusDays(30).format(FORMATTER));
		env.putString(TO_BOOKING_DATE_MAX_LIMITED, currentDate.minusDays(20).format(FORMATTER));

		callAndStopOnFailure(PrepareUrlForFetchingCurrentAccountTransactions.class);
		callAndStopOnFailure(AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl.class);
		runInBlock("Fetch Account Current transactions with invalid date parameters",
			() -> call(getPreCallProtectedResourceSequence()
				.replace(EnsureResponseCodeWas200.class, condition(EnsureResponseCodeWas422.class)))
		);

		callAndStopOnFailure(ResourceErrorMetaValidator.class);
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddAccountScope.class);
		callAndStopOnFailure(PrepareAllAccountRelatedConsentsForHappyPathTest.class);
	}

	protected ConditionSequence getValidationSequence() {
		return sequenceOf(
			condition(AccountTransactionsValidatorV2.class),
			condition(EnsureResponseHasLinks.class),
			condition(ValidateMetaOnlyRequestDateTime.class),
			condition(VerifyAdditionalFieldsWhenMetaOnlyRequestDateTime.class)
				.dontStopOnFailure()
				.onFail(Condition.ConditionResult.WARNING)
				.skipIfStringMissing("metaOnlyRequestDateTime")
		);
	}

	protected ConditionSequence getPreCallProtectedResourceSequence() {
		return sequenceOf(
			condition(CreateEmptyResourceEndpointRequestHeaders.class),
			condition(AddFAPIAuthDateToResourceEndpointRequest.class),
			condition(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class),
			condition(CreateRandomFAPIInteractionId.class),
			condition(AddFAPIInteractionIdToResourceEndpointRequest.class),
			condition(CallProtectedResource.class),
			condition(EnsureResponseCodeWas200.class),
			condition(CheckForDateHeaderInResourceResponse.class),
			condition(CheckForFAPIInteractionIdInResourceResponse.class),
			condition(EnsureResourceResponseReturnedJsonContentType.class)
		);
	}
}
