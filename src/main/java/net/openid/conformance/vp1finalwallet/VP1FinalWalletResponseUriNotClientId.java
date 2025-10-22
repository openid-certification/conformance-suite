package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.AddBadResponseUriToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddResponseUriToAuthorizationEndpointRequest;
import net.openid.conformance.condition.common.ExpectResponseUriErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-response-uri-not-client-id",
	displayName = "OID4VP-1.0-FINAL: response_uri not valid",
	summary = "Makes a request where the response_uri is not the client_id. The wallet must display an error, a screenshot of which must be uploaded.",
	profile = "OID4VP-1FINAL",
	configurationFields = {
	}
)
// For x509 dns the client_id we try would need to be on a different hostname; but even this is permitted by the specs in some cases:
// "If the Wallet can establish trust in the Client Identifier authenticated through the certificate, e.g. because the Client Identifier is contained in a list of trusted Client Identifiers, it may allow the client to freely choose the redirect_uri value."
// So we just don't do this test for x509_san_dns for now
@VariantNotApplicable(parameter = VP1FinalWalletClientIdPrefix.class, values={"x509_san_dns", "decentralized_identifier", "pre_registered"})

// For BrowserAPI Response URI isn't used
@VariantNotApplicable(parameter = VP1FinalWalletResponseMode.class, values={"dc_api", "dc_api.jwt"})

public class VP1FinalWalletResponseUriNotClientId extends AbstractVP1FinalWalletTest {
	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence createAuthorizationRequestSteps = super.createAuthorizationRequestSequence();

		createAuthorizationRequestSteps = createAuthorizationRequestSteps.
			replace(AddResponseUriToAuthorizationEndpointRequest.class, condition(AddBadResponseUriToAuthorizationEndpointRequest.class));

		return createAuthorizationRequestSteps;
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectResponseUriErrorPage.class, "OID4VP-ID2-6.2");

		env.putString("error_callback_placeholder", env.getString("response_uri_error"));
	}

	@Override
	protected void continueAfterRequestUriCalled() {
		eventLog.log(getName(), "Wallet has retrieved request_uri - the response_uri is invalid, so the wallet should display an error, a screenshot of which must be uploaded for the test to transition to 'FINISHED'.");
		createPlaceholder();
		waitForPlaceholders();
	}

	@Override
	protected Object handleDirectPost(String requestId) {
		throw new TestFailureException(getId(), "Direct post (response_uri) endpoint has been called but was not in the request");
	}
}
