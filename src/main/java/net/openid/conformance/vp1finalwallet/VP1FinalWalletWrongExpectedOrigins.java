package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.AddExpectedOriginsToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddWrongExpectedOriginsToAuthorizationEndpointRequest;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-wrong-expected-origins",
	displayName = "OID4VP-1.0-FINAL: Signed DC API request with wrong expected_origins",
	summary = """
		Sends a signed DC API request with an incorrect expected_origins value. The wallet must validate \
		expected_origins and reject requests where the value does not match the actual origin. \
		The wallet should display an error, a screenshot of which must be uploaded.""",
	profile = "OID4VP-1FINAL"
)
@VariantNotApplicable(parameter = VP1FinalWalletResponseMode.class, values = {"direct_post", "direct_post.jwt", "dc_api"})
@VariantNotApplicable(parameter = VP1FinalWalletRequestMethod.class, values = {"request_uri_unsigned"})
public class VP1FinalWalletWrongExpectedOrigins extends AbstractVP1FinalWalletTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence steps = super.createAuthorizationRequestSequence();

		steps.replace(AddExpectedOriginsToAuthorizationEndpointRequest.class,
			condition(AddWrongExpectedOriginsToAuthorizationEndpointRequest.class));

		return steps;
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "OID4VP-1FINALA-A.2");
		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected void continueAfterRequestUriCalled() {
		eventLog.log(getName(), "Wallet has received signed DC API request with wrong expected_origins - the wallet should display an error.");
		createPlaceholder();
		waitForPlaceholders();
	}

	@Override
	protected Object handleDirectPost(String requestId) {
		throw new TestFailureException(getId(), "Direct post endpoint was called but the wallet should have rejected the request due to wrong expected_origins");
	}

	@Override
	protected void processBrowserApiResponse() {
		handleBrowserApiResponseAsNegativeTest(
			"Browser API returned a successful response but the wallet should have rejected the request due to wrong expected_origins");
	}
}
