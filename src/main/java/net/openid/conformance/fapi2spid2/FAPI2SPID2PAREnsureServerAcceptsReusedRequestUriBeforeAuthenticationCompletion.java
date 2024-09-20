package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.client.ExpectLoginPage;

import net.openid.conformance.testmodule.PublishTestModule;

import java.util.List;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-par-ensure-reused-request-uri-prior-to-auth-completion-succeeds",
	displayName = "FAPI2-Security-Profile-ID2: PAR - ensure reused request uri prior to auth completion succeeds",
	summary = "This test checks that authorization servers that enforce one-time use of the `request_uri` do so at the point of authorization, not at the point of loading an authorization page. An initial authorization request is issued. The login screen is displayed, but the login must not be attempted. This is followed by a second authorization request which should proceed as normal and succeed",
	profile = "FAPI2-Security-Profile-ID2",
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
public class FAPI2SPID2PAREnsureServerAcceptsReusedRequestUriBeforeAuthenticationCompletion extends AbstractFAPI2SPID2ServerTestModule {
	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectLoginPage.class);
	}

	@Override
	protected void performRedirect(String method) {
		// As per https://bitbucket.org/openid/fapi/issues/635/one-time-use-of-request_uri-causing-error
		// perform a redirect to the authorization endpoint prior to a successful authorization flow.
		//
		// The user should take no action.
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

		// Proceed to the regular authorization flow. This should succeed.
		performRedirectAndWaitForPlaceholdersOrCallback("login_page_placeholder");
	}
}
