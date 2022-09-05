package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AddTosUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CreateTosUri;
import net.openid.conformance.condition.client.ExpectLoginPageWithTosLink;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Registration_tos_uri
@PublishTestModule(
	testName = "oidcc-registration-tos-uri",
	displayName = "OIDCC: dynamic registration with TOS URI",
	summary = "This test calls the dynamic registration endpoint with a TOS URI. This should result in the browser being redirected to a login page with a link to the TOS document displayed. To make sure you get a fresh login page, you need to remove any cookies you may have received from the OP before proceeding. A screenshot of the login page should be uploaded.",
	profile = "OIDCC"
)
public class OIDCCRegistrationTosUri extends AbstractOIDCCDynamicRegistrationTest {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(CreateTosUri.class);
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectLoginPageWithTosLink.class, "OIDCR-2");
	}

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(AddTosUriToDynamicRegistrationRequest.class);
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
