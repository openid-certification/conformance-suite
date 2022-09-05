package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AddPolicyUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CreatePolicyUri;
import net.openid.conformance.condition.client.ExpectLoginPageWithPolicyLink;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Registration_policy_uri
@PublishTestModule(
	testName = "oidcc-registration-policy-uri",
	displayName = "OIDCC: dynamic registration with policy URI",
	summary = "This test calls the dynamic registration endpoint with a policy URI. This should result in the browser being redirected to a login page with a link to the policy document displayed. To make sure you get a fresh login page, you need to remove any cookies you may have received from the OP before proceeding. A screenshot of the login page should be uploaded.",
	profile = "OIDCC"
)
public class OIDCCRegistrationPolicyUri extends AbstractOIDCCDynamicRegistrationTest {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(CreatePolicyUri.class);
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectLoginPageWithPolicyLink.class, "OIDCR-2");
	}

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(AddPolicyUriToDynamicRegistrationRequest.class);
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
