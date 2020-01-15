package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

// Equivalent of https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_nonce_NoReq_code
@PublishTestModule(
	testName = "oidcc-ensure-request-without-nonce-succeeds-for-code-flow",
	displayName = "OIDCC: ensure request without nonce succeeds (non-implicit flows)",
	summary = "This test should end with the authorisation server issuing an authorization code, even though a nonce was not supplied.",
	profile = "OIDCC",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client2.scope"
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values = {
	"id_token",
	"id_token token",
	"code id_token",
	"code id_token token"
})
public class OIDCCEnsureRequestWithoutNonceSucceedsForCodeFlow extends AbstractOIDCCServerTest {

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
				.skip(AddNonceToAuthorizationEndpointRequest.class,
						"NOT adding nonce to request object"));
	}

	@Override
	protected void performPostAuthorizationFlow() {
		onPostAuthorizationFlowComplete();
	}
}
