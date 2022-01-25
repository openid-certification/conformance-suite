package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddQueryToRedirectUri;
import net.openid.conformance.condition.client.ReplaceRedirectUriQueryInAuthorizationRequest;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantNotApplicable;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_redirect_uri_Query_Mismatch
@PublishTestModule(
	testName = "oidcc-redirect-uri-query-mismatch",
	displayName = "OIDCC: rejects redirect_uri when query parameter does not match what is registered",
	summary = "This test uses a redirect uri with a query component that does not match the query in the registered redirect uri. The authorization server should display an error saying the redirect uri is invalid, a screenshot of which should be uploaded.",
	profile = "OIDCC"
)
@VariantNotApplicable(parameter = ClientRegistration.class, values = {"static_client"})
public class OIDCCRedirectUriQueryMismatch extends AbstractOIDCCServerTestExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "OIDCC-3.1.2.1");

		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected void configureDynamicClient() {

		callAndStopOnFailure(AddQueryToRedirectUri.class);
		exposeEnvString("redirect_uri");

		super.configureDynamicClient();
	}

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.then(condition(ReplaceRedirectUriQueryInAuthorizationRequest.class));
	}

	@Override
	protected void processCallback() {
		throw new TestFailureException(getId(), "The authorization server called the registered redirect uri. This should not have happened as the client provided a bad redirect_uri in the request.");
	}
}
