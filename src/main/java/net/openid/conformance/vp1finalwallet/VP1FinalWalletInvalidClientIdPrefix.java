package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddInvalidClientIdPrefixToMultiSignedClientIds;
import net.openid.conformance.condition.client.AddInvalidClientIdPrefixToRequestObject;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.VariantNotApplicableWhen;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-invalid-client-id-prefix",
	displayName = "OID4VP-1.0-FINAL: Invalid client_id prefix scheme",
	summary = """
		Sends a request with an unrecognized client_id prefix scheme (e.g. 'invalid_scheme:'). \
		Per OID4VP section 5.9.2 the wallet must either refuse the request or fall back to treating \
		the full client_id as referring to a pre-registered client, which is not known to the wallet, \
		so either way the wallet cannot complete the flow. \
		The wallet should display an error, a screenshot of which must be uploaded.""",
	profile = "OID4VP-1FINAL"
)
@VariantNotApplicableWhen(
	parameter = VP1FinalWalletRequestMethod.class,
	values = {"request_uri_unsigned"},  // unsigned DC API requests contain no client_id to corrupt (OID4VP Appendix A.2)
	whenParameter = VP1FinalWalletResponseMode.class,
	hasValues = {"dc_api", "dc_api.jwt"}
)
public class VP1FinalWalletInvalidClientIdPrefix extends AbstractVP1FinalWalletTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		// For multi-signed requests client_id lives in each signature's protected header rather than
		// the shared payload (OID4VP Appendix A.3.2.2), so corrupt the two client_id strings the signer
		// reads instead of the request object. This runs before signRequestObject().
		Class<? extends Condition> corruptClientId = switch (requestMethod) {
			case REQUEST_URI_MULTISIGNED -> AddInvalidClientIdPrefixToMultiSignedClientIds.class;
			default -> AddInvalidClientIdPrefixToRequestObject.class;
		};
		return super.createAuthorizationRequestSequence()
			.then(condition(corruptClientId));
	}

	@Override
	protected void performRedirect() {
		// an invalid client_id prefix is detectable from the authorization request
		// parameters alone, so a conformant wallet may reject the request without ever dereferencing
		// request_uri (url_query mode has no request_uri fetch at all). Expose the failure-photo
		// upload as soon as the request has been sent to the wallet, rather than gating it on the
		// request_uri fetch. The placeholder must be created before the redirect moves the test to
		// WAITING (which releases the lock, so conditions can no longer be called from this thread) -
		// this helper does that in the right order.
		performRedirectAndWaitForPlaceholdersOrCallback();
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "OID4VP-1FINAL-5.9");
		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected void continueAfterRequestUriCalled() {
		// The placeholder and waitForPlaceholders() are already established in performRedirect(), so
		// here we only record that the wallet chose to fetch the request object before rejecting.
		eventLog.log(getName(), "Wallet has retrieved request_uri - the client_id has an invalid prefix scheme, so the wallet should display an error.");
	}

	@Override
	protected Object handleDirectPost(String requestId) {
		throw new TestFailureException(getId(), "Direct post endpoint was called but the wallet should have rejected the request due to invalid client_id prefix");
	}

	@Override
	protected void processBrowserApiResponse() {
		handleBrowserApiResponseAsNegativeTest(
			"Browser API returned a successful response but the wallet should have rejected the request due to invalid client_id prefix");
	}
}
