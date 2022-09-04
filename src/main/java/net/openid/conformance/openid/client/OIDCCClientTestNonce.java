package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.ExtractNonceFromAuthorizationRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-client-test-nonce-unless-code-flow",
	displayName = "OIDCC: Relying party test, always send the nonce parameter while using implicit or hybrid flow.",
	summary = "The client is expected to send the nonce parameter." +
		" Corresponds to rp-nonce-unless-code-flow test in the old suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values={"code"})
public class OIDCCClientTestNonce extends AbstractOIDCCClientTest {

	@Override
	protected void validateResponseTypeAuthorizationRequestParameter() {
		super.validateResponseTypeAuthorizationRequestParameter();
	}

	@Override
	protected void extractNonceFromAuthorizationEndpointRequestParameters() {
		callAndStopOnFailure(ExtractNonceFromAuthorizationRequest.class,"OIDCC-3.1.2.1");
	}
}
