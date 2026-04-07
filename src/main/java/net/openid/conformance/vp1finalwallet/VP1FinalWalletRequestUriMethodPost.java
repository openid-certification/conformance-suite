package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.AddRequestUriMethodPostToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddRequestUriMethodPostToRedirectUrl;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;
import org.jetbrains.annotations.NotNull;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-request-uri-method-post",
	displayName = "OID4VP-1.0-FINAL: request_uri_method=post",
	summary = "Sends an authorization request with request_uri_method=post, which tells the wallet to use HTTP POST " +
		"when fetching the request_uri. If the wallet does not support POST, it may fall back to GET, which is " +
		"permitted by the specification.",
	profile = "OID4VP-1FINAL"
)

@VariantNotApplicable(parameter = VP1FinalWalletResponseMode.class, values = {"dc_api", "dc_api.jwt"})
public class VP1FinalWalletRequestUriMethodPost extends AbstractVP1FinalWalletTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence createAuthorizationRequestSteps = super.createAuthorizationRequestSequence();

		createAuthorizationRequestSteps = createAuthorizationRequestSteps.
			then(condition(AddRequestUriMethodPostToAuthorizationEndpointRequest.class));

		return createAuthorizationRequestSteps;
	}

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

}
