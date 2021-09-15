package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.account.AccountIdentificationResponseValidator;
import net.openid.conformance.openbanking_brasil.account.AccountListValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "account-api-permissions-restriction-test",
	displayName = "Ensures permissions allow you to call only the correct resources",
	summary = "Ensures permissions allow you to call only the correct resources - When completed, please upload a screenshot of the permissions being requested by the bank",
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
public class AccountsApiReadPermissionsAreRestricted extends AbstractOBBrasilFunctionalTestModule {

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
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});



		runInBlock("Ensure we cannot call the account limits API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountLimits.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		fireTestReviewNeeded();

	}

}
