package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.AddMismatchedClientIdToRequestObject;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-mismatched-client-id",
	displayName = "OID4VP-1.0-FINAL: Mismatched client_id in request object and URL",
	summary = """
		Sends a request where the client_id in the request object differs from the client_id in the URL parameter. \
		Per OID4VP section 5, these MUST be identical. The wallet must reject the request and display an error, \
		a screenshot of which must be uploaded.""",
	profile = "OID4VP-1FINAL"
)
@VariantNotApplicable(parameter = VP1FinalWalletResponseMode.class, values = {"dc_api", "dc_api.jwt"})
@VariantNotApplicable(parameter = VP1FinalWalletRequestMethod.class, values = {"url_query", "request_uri_multisigned"})
public class VP1FinalWalletMismatchedClientIdInRequestObject extends AbstractVP1FinalWalletTest {

	@Override
	protected ConditionSequence createAuthorizationRedirectStepsUnsignedRequestUri() {
		return super.createAuthorizationRedirectStepsUnsignedRequestUri()
			.insertAfter(ConvertAuthorizationEndpointRequestToRequestObject.class,
				condition(AddMismatchedClientIdToRequestObject.class));
	}

	@Override
	protected ConditionSequence createAuthorizationRedirectStepsSignedRequestUri() {
		return super.createAuthorizationRedirectStepsSignedRequestUri()
			.insertAfter(ConvertAuthorizationEndpointRequestToRequestObject.class,
				condition(AddMismatchedClientIdToRequestObject.class));
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "OID4VP-1FINAL-5");

		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected void continueAfterRequestUriCalled() {
		eventLog.log(getName(), "Wallet has retrieved request_uri - the client_id in the request object is mismatched, so the wallet should display an error, a screenshot of which must be uploaded for the test to transition to 'FINISHED'.");
		createPlaceholder();
		waitForPlaceholders();
	}

	@Override
	protected Object handleDirectPost(String requestId) {
		throw new TestFailureException(getId(), "Direct post (response_uri) endpoint has been called but the wallet should have rejected the request due to client_id mismatch");
	}
}
