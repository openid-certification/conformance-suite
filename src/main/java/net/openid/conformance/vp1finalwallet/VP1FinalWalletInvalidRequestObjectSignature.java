package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.condition.client.CreateMultiSignedRequestObject;
import net.openid.conformance.condition.client.InvalidateMultiSignedRequestObjectSignatures;
import net.openid.conformance.condition.client.InvalidateRequestObjectSignature;
import net.openid.conformance.condition.common.ExpectInvalidRequestObjectSignatureErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.VariantNotApplicable;
import org.jetbrains.annotations.NotNull;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-invalid-request-object-signature",
	displayName = "OID4VP-1.0-FINAL: Request object signature not valid",
	summary = "Makes a request where the signature on the request object JWS is invalid. For multi-signed requests, all signatures are invalidated. The wallet should display an error, a screenshot of which must be uploaded.",
	profile = "OID4VP-1FINAL",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = VP1FinalWalletRequestMethod.class, values={"request_uri_unsigned", "url_query"})
public class VP1FinalWalletInvalidRequestObjectSignature extends AbstractVP1FinalWalletTest {

	@NotNull
	@Override
	protected ConditionSequence createAuthorizationRedirectStepsSignedRequestUri() {
		ConditionSequence seq = super.createAuthorizationRedirectStepsSignedRequestUri();

		seq = seq.insertBefore(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class,
			condition(InvalidateRequestObjectSignature.class));

		return seq;
	}

	@NotNull
	@Override
	protected ConditionSequence createAuthorizationRedirectStepsMultiSignedRequestUri() {
		ConditionSequence seq = super.createAuthorizationRedirectStepsMultiSignedRequestUri();

		seq = seq.insertAfter(CreateMultiSignedRequestObject.class,
			condition(InvalidateMultiSignedRequestObjectSignatures.class));

		return seq;
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectInvalidRequestObjectSignatureErrorPage.class, "OID4VP-1FINAL-8.5");
		env.putString("error_callback_placeholder", env.getString("invalid_request_object_signature_error"));
	}

	@Override
	protected void continueAfterRequestUriCalled() {
		eventLog.log(getName(), "Wallet has retrieved request_uri - the request object signature is invalid, so the wallet should display an error, a screenshot of which must be uploaded for the test to transition to 'FINISHED'.");
		createPlaceholder();
		waitForPlaceholders();
	}

	@Override
	protected Object handleDirectPost(String requestId) {
		throw new TestFailureException(getId(), "Direct post (response_uri) endpoint has been called but the wallet should have stopped because of the invalid signature on the request object.");
	}

}
