package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.AddRedirectUriAlongsideResponseUri;
import net.openid.conformance.condition.client.AddResponseUriToAuthorizationEndpointRequest;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-redirect-uri-with-direct-post",
	displayName = "OID4VP-1.0-FINAL: redirect_uri sent with response_mode=direct_post",
	summary = "Sends a redirect_uri parameter in the authorization request alongside response_uri when using direct_post. "
		+ "The wallet should reject this request and display an error, a screenshot of which must be uploaded.",
	profile = "OID4VP-1FINAL"
)
@VariantNotApplicable(parameter = VP1FinalWalletResponseMode.class, values = {"dc_api", "dc_api.jwt"})
public class VP1FinalWalletRedirectUriWithDirectPost extends AbstractVP1FinalWalletTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence steps = super.createAuthorizationRequestSequence();

		steps = steps.insertAfter(AddResponseUriToAuthorizationEndpointRequest.class,
			condition(AddRedirectUriAlongsideResponseUri.class));

		return steps;
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "OID4VP-1FINAL-5");
		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected void continueAfterRequestUriCalled() {
		eventLog.log(getName(), "Wallet has retrieved request_uri - the request includes both redirect_uri and response_uri, so the wallet should display an error.");
		createPlaceholder();
		waitForPlaceholders();
	}

	@Override
	protected Object handleDirectPost(String requestId) {
		throw new TestFailureException(getId(), "Direct post endpoint was called but the wallet should have rejected the request due to redirect_uri being present with direct_post");
	}
}
