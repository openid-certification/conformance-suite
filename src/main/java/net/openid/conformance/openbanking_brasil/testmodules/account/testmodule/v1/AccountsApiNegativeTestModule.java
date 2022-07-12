package net.openid.conformance.openbanking_brasil.testmodules.account.testmodule.v1;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.account.v1.AccountLimitsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.account.BuildAccountsConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.account.PrepareAllAccountRelatedConsentsForHappyPathTest;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "account-api-negative-test",
	displayName = "Validate correct handling of errors for the account API resources",
	summary = "Validates correct handling of errors for the account API resources\n" +
		"\u2022 Creates a Consent with the complete set of the accounts permission group (\"ACCOUNTS_READ\",\"ACCOUNTS_BALANCES_READ\",\"RESOURCES_READ\", \"ACCOUNTS_TRANSACTIONS_READ\", \"ACCOUNTS_OVERDRAFT_LIMITS_READ\")\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Accounts API \n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Accounts API \n" +
		"\u2022 Expects a 200 response",
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
public class AccountsApiNegativeTestModule extends AbstractOBBrasilFunctionalTestModule {
	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddAccountScope.class);
		callAndStopOnFailure(PrepareAllAccountRelatedConsentsForHappyPathTest.class);
	}
	@Override
	protected void validateResponse() {
		preCallProtectedResource("Fetch Account limits");
		callAndStopOnFailure(ForceNaCurrency.class);
		callAndContinueOnFailure(AccountLimitsValidator.class, Condition.ConditionResult.SUCCESS);
	}
}
