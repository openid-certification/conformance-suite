package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddQueryToRedirectUriInAuthorizationRequest;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_redirect_uri_Query_Added
@PublishTestModule(
	testName = "oidcc-redirect-uri-query-added",
	displayName = "OIDCC: request with redirect_uri with query component when registered redirect_uri has no query component",
	summary = "This test uses a redirect uri with a query component when the registered redirect uri has no query component. The authorization server should display an error saying the redirect uri is invalid, a screenshot of which should be uploaded.",
	profile = "OIDCC"
)
public class OIDCCRedirectUriQueryAdded extends AbstractOIDCCServerTestExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "OIDCC-3.1.2.1");

		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.then(condition(AddQueryToRedirectUriInAuthorizationRequest.class));
	}

	@Override
	protected void processCallback() {
		throw new TestFailureException(getId(), "The authorization server called the registered redirect uri. This should not have happened as the client provided a bad redirect_uri in the request.");
	}
}
