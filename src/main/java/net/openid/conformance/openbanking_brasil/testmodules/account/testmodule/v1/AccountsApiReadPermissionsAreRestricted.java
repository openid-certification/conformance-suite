package net.openid.conformance.openbanking_brasil.testmodules.account.testmodule.v1;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.account.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "account-api-permissions-restriction-test",
	displayName = "Ensures permissions allow you to call only the correct resources",
	summary = "Ensures permissions allow you to call only the correct resources - When completed, please upload a screenshot of the permissions being requested by the bank\n" +
		"\u2022 Creates a Consent with the incomplete set of the accounts permission group (\"ACCOUNTS_READ\", \"ACCOUNTS_TRANSACTIONS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Accounts API \n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Accounts API specifying an account ID\n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Accounts Transactions API specifying an account ID\n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Accounts Balances API specifying an account ID\n" +
		"\u2022 Expects a 403 response \n" +
		"\u2022 Calls GET Accounts Limits API specifying an account ID\n" +
		"\u2022 Expects a 403 response ",
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
public class AccountsApiReadPermissionsAreRestricted extends AbstractOBBrasilFunctionalTestModule {
	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}
	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(RequestAccountReadOnly.class);
		callAndStopOnFailure(AddAccountScope.class);
		callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
	}

	@Override
	protected void validateResponse() {
		callAndStopOnFailure(AccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
		preCallProtectedResource("Fetch Account");

		runInBlock("Ensure we can call the account transactions API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);
			preCallProtectedResource();
		});

		runInBlock("Ensure we cannot call the account balance API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountBalances.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});



		runInBlock("Ensure we cannot call the account limits API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountLimits.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		fireTestReviewNeeded();

	}

}
