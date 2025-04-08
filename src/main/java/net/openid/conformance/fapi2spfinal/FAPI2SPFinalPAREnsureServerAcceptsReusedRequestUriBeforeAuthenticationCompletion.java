package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.client.ExpectLoginPage;

import net.openid.conformance.testmodule.PublishTestModule;

import java.util.List;

@PublishTestModule(
	testName = "fapi2-security-profile-final-par-ensure-reused-request-uri-prior-to-auth-completion-succeeds",
	displayName = "FAPI2-Security-Profile-Final: PAR - ensure reused request uri prior to auth completion succeeds",
	summary = "This test checks that authorization servers that enforce one-time use of `request_uri` values do so at the point of authorization, not at the point of visiting the authorization endpoint. This is achieved by visiting the authorization endpoint twice with the same 'request_uri' value. On the first visit no login should be attempted. On the second visit the login is attempted and is expected to succeed. On error a screenshot showing the resulting error needs to be uploaded  This test is as per FAPI 2.0 Security Profile 5.3.2.2 Note 3. This is a recommendation and as such any failure of this test will result in a warning.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPI2SPFinalPAREnsureServerAcceptsReusedRequestUriBeforeAuthenticationCompletion extends AbstractFAPI2SPFinalServerTestModule {
	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectLoginPage.class);
	}

	@Override
	protected void performRedirect(String method) {
		// Initial visit to the authorization endpoint. The user should take no action.
		redirect(env.getString("redirect_to_authorization_endpoint"));

		// Wait for the login screen to be loaded.
		boolean loginScreenVisited = false;

		for (int attempts = 0; attempts < 10; attempts++) {
			setStatus(Status.WAITING);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			setStatus(Status.RUNNING);

			List<String> visitedUrls = getBrowser().getVisited();

			if (visitedUrls.contains(env.getString("redirect_to_authorization_endpoint"))) {
				loginScreenVisited = true;
				break;
			}
		}

		if (! loginScreenVisited) {
			throw new RuntimeException("Could not perform initial redirect to authorization server");
		}
		eventLog.endBlock();

		eventLog.startBlock("Make second request to authorization endpoint");

		// Proceed with the regular authorization flow, revisiting the authorization endpoint and logging in.
		performRedirectAndWaitForPlaceholdersOrCallback("login_page_placeholder");
	}
}
