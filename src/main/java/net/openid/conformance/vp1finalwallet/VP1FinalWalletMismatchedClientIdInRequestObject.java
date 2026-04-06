package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.AddMismatchedClientIdToRequestObject;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-mismatched-client-id",
	displayName = "OID4VP-1.0-FINAL: Mismatched client_id in request object and URL",
	summary = "Sends a request where the client_id in the request object differs from the client_id in the URL parameter. "
		+ "Per OID4VP section 5, these MUST be identical. The wallet must reject the request and display an error, "
		+ "a screenshot of which must be uploaded.",
	profile = "OID4VP-1FINAL"
)
public class VP1FinalWalletMismatchedClientIdInRequestObject extends AbstractVP1FinalWalletTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence createAuthorizationRequestSteps = super.createAuthorizationRequestSequence();

		createAuthorizationRequestSteps = createAuthorizationRequestSteps.
			insertBefore(getRequestUriRedirectCondition(), condition(AddMismatchedClientIdToRequestObject.class));

		return createAuthorizationRequestSteps;
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
