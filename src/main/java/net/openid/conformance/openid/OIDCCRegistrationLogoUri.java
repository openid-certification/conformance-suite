package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AddLogoUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CreateLogoUri;
import net.openid.conformance.condition.client.ExpectLoginPageWithLogo;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Registration_logo_uri
@PublishTestModule(
	testName = "oidcc-registration-logo-uri",
	displayName = "OIDCC: dynamic registration with Logo URI",
	summary = "This test calls the dynamic registration endpoint with a logo URI. This should result in the browser being redirected to a login page with the RP logo displayed. To make sure you get a fresh login page, you need to remove any cookies you may have received from the OP before proceeding. A screenshot of the login page should be uploaded.",
	profile = "OIDCC"
)
public class OIDCCRegistrationLogoUri extends AbstractOIDCCDynamicRegistrationTest {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(CreateLogoUri.class);
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectLoginPageWithLogo.class, "OIDCR-2");
	}

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(AddLogoUriToDynamicRegistrationRequest.class);
	}

	@Override
	protected void performAuthorizationFlow() {
		// Redirect to the authorization endpoint to check the appearance of the login page.

		eventLog.startBlock("Make request to authorization endpoint");
		createAuthorizationRequest();
		createAuthorizationRedirect();
		performRedirectAndWaitForPlaceholdersOrCallback("login_page_placeholder");
		eventLog.endBlock();
	}
}
