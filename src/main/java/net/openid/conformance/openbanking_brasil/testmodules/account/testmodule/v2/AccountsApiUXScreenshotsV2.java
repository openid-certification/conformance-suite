package net.openid.conformance.openbanking_brasil.testmodules.account.testmodule.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddAccountScope;
import net.openid.conformance.openbanking_brasil.testmodules.account.BuildAccountsConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.account.PrepareAllAccountRelatedConsentsForHappyPathTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "account-api-ux-test-v2",
	displayName = "Verifies the screens implemented by the financial institution",
	summary = "Verifies the screens implemented by the financial institution - Please upload screenshots of the login screen, MFA screen and consent screen when finished\n" +
		"\u2022 Creates a Consent with the complete set of the accounts permission group (\"ACCOUNTS_READ\", \"ACCOUNTS_BALANCES_READ\", \"RESOURCES_READ\", \"ACCOUNTS_TRANSACTIONS_READ\", \"ACCOUNTS_OVERDRAFT_LIMITS_READ\")\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Requests the user to upload a series of pictures regarding the customer authorization flow",
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
public class AccountsApiUXScreenshotsV2 extends AbstractOBBrasilFunctionalTestModule {
	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareAllAccountRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(AddAccountScope.class);
	}

	@Override
	protected void validateResponse() {
		fireTestReviewNeeded();
	}
}
