package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.*;
import net.openid.conformance.openbanking_brasil.account.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "account-api-bookingdate-test",
	displayName = "Test the max date of a payment",
	summary = "Testing that the server is respecting the BookingDate filter rules\n" +
		"\u2022 Creates a consent with only ACCOUNTS permissions\n" +
		"\u2022 201 code and successful redirect\n" +
		"\u2022 Using the consent created, call the Accounts API\n" +
		"\u2022 Call GET Accounts Transactions API, send headers fromBookingDate and toBookingDate using the max period (12 months from exisiting date)\n" +
		"\u2022 Expect success, fetch a transaction, get the transactionDate\n" +
		"\u2022 Call GET Accounts Transactions API, send headers fromBookingDate and toBookingDate to be the transactionDate",
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
		"resource.resourceUrl"
	}
)

public class AccountApiBookingDateTest extends AbstractOBBrasilFunctionalTestModule{

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddAccountScope.class);
		callAndStopOnFailure(PrepareAllAccountRelatedConsentsForHappyPathTest.class);
	}

	Boolean keepHeaders = false;

	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(AccountListValidator.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(AccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
		preCallProtectedResource("Fetch Account");
		callAndContinueOnFailure(AccountIdentificationResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(PrepareUrlForFetchingAccountBalances.class);
		preCallProtectedResource("Fetch Account balance");
		callAndContinueOnFailure(AccountBalancesResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);
		callAndStopOnFailure(LogKnownIssue.class,"BCLOG-F02-172");
		eventLog.startBlock("Add booking date headers 1 year apart");
		keepHeaders = true;
		callAndContinueOnFailure(AddBookingDateHeaders.class, Condition.ConditionResult.FAILURE);
		preCallProtectedResource("Fetch Account transactions");
		eventLog.startBlock("Set booking date headers as transaction date");
		callAndContinueOnFailure(DateExtractor.class, Condition.ConditionResult.FAILURE);
		preCallProtectedResource("Fetch Account transactions");
		keepHeaders = false;
		eventLog.startBlock("End of date tests");
		callAndContinueOnFailure(AccountTransactionsValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));
	}

	@Override
	protected void preCallProtectedResource() {
		if (!keepHeaders) {
			callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);
		}
		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-3");
		callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-4");
		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-5");
		callAndStopOnFailure(CallProtectedResource.class, "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");
		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");
		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");
		callAndContinueOnFailure(EnsureResourceResponseReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-9", "FAPI1-BASE-6.2.1-10");
	}
}
