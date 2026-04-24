package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddRequestUriMethodPostToRedirectUrl;
import net.openid.conformance.condition.client.EnsureIncomingRequestContentTypeIsFormUrlEncoded;
import net.openid.conformance.condition.client.ExtractWalletMetadataAndNonceFromRequestUriPost;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;
import org.jetbrains.annotations.NotNull;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-request-uri-method-post",
	displayName = "OID4VP-1.0-FINAL: request_uri_method=post",
	summary = """
		Sends an authorization request with request_uri_method=post, which tells the wallet to use HTTP POST \
		when fetching the request_uri. If the wallet does not support POST, it may fall back to GET, which is \
		permitted by the specification.""",
	profile = "OID4VP-1FINAL"
)

@VariantNotApplicable(parameter = VP1FinalWalletResponseMode.class, values = {"dc_api", "dc_api.jwt"})
public class VP1FinalWalletRequestUriMethodPost extends AbstractVP1FinalWalletTest {

	private boolean walletUsedGetInsteadOfPost = false;

	@NotNull
	@Override
	protected ConditionSequence createAuthorizationRedirectStepsUnsignedRequestUri() {
		return super.createAuthorizationRedirectStepsUnsignedRequestUri()
			.then(condition(AddRequestUriMethodPostToRedirectUrl.class));
	}

	@NotNull
	@Override
	protected ConditionSequence createAuthorizationRedirectStepsSignedRequestUri() {
		return super.createAuthorizationRedirectStepsSignedRequestUri()
			.then(condition(AddRequestUriMethodPostToRedirectUrl.class));
	}

	@Override
	protected void validateRequestUriFetchMethod() {
		String incomingMethod = env.getString("incoming_request", "method");
		if ("POST".equals(incomingMethod)) {
			eventLog.log(getName(), "Wallet correctly used HTTP POST to fetch request_uri");
			callAndContinueOnFailure(EnsureIncomingRequestContentTypeIsFormUrlEncoded.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.10");
			callAndContinueOnFailure(ExtractWalletMetadataAndNonceFromRequestUriPost.class, ConditionResult.INFO, "OID4VP-1FINAL-5.10");
		} else {
			walletUsedGetInsteadOfPost = true;
		}
	}

	@Override
	protected Object handleDirectPost(String requestId) {
		if (walletUsedGetInsteadOfPost) {
			setStatus(Status.RUNNING);
			fireTestSkipped("Wallet used GET instead of POST for request_uri. " +
				"The specification permits this as a fallback when the wallet does not support POST.");
		}
		return super.handleDirectPost(requestId);
	}

}
