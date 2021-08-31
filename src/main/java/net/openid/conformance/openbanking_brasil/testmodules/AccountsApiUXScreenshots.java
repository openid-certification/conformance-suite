package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddAccountScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareAllAccountRelatedConsentsForHappyPathTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "account-api-ux-test",
	displayName = "Verifies the screens implemented by the financial institution",
	summary = "Verifies the screens implemented by the financial institution - Please upload screenshots of the login screen, MFA screen and consent screen when finished",
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
public class AccountsApiUXScreenshots extends AbstractOBBrasilFunctionalTestModule {

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
