package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.AddInvalidClientIdPrefixToRequestObject;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-invalid-client-id-prefix",
	displayName = "OID4VP-1.0-FINAL: Invalid client_id prefix scheme",
	summary = "Sends a request with an unrecognized client_id prefix scheme (e.g. 'invalid_scheme:'). "
		+ "The wallet must reject requests with unknown client_id prefix values. "
		+ "The wallet should display an error, a screenshot of which must be uploaded.",
	profile = "OID4VP-1FINAL"
)
public class VP1FinalWalletInvalidClientIdPrefix extends AbstractVP1FinalWalletTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence steps = super.createAuthorizationRequestSequence();

		steps = steps.insertBefore(getRequestUriRedirectCondition(),
			condition(AddInvalidClientIdPrefixToRequestObject.class));

		return steps;
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "OID4VP-1FINAL-5.9");
		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected void continueAfterRequestUriCalled() {
		eventLog.log(getName(), "Wallet has retrieved request_uri - the client_id has an invalid prefix scheme, so the wallet should display an error.");
		createPlaceholder();
		waitForPlaceholders();
	}

	@Override
	protected Object handleDirectPost(String requestId) {
		throw new TestFailureException(getId(), "Direct post endpoint was called but the wallet should have rejected the request due to invalid client_id prefix");
	}
}
