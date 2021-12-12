package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

// Equivalent of https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_nonce_NoReq_code
@PublishTestModule(
	testName = "oidcc-ensure-request-without-nonce-succeeds-for-code-flow",
	displayName = "OIDCC: ensure request without nonce succeeds (non-implicit flows)",
	summary = "This test should end with the authorization server issuing an authorization code, even though a nonce was not supplied. nonce is required for all flows that return an id_token from the authorization endpoint, see https://bitbucket.org/openid/connect/issues/972/nonce-requirement-in-hybrid-auth-request / https://bitbucket.org/openid/connect/issues/1052/make-clear-that-nonce-is-always-required and the latest OpenID Connect errata draft, https://openid.net/specs/openid-connect-core-1_0-27.html#NonceNotes",
	profile = "OIDCC"
)
@VariantNotApplicable(parameter = ResponseType.class, values = {
	"id_token",
	"id_token token",
	"code id_token",
	"code id_token token"
})
public class OIDCCEnsureRequestWithoutNonceSucceedsForCodeFlow extends AbstractOIDCCServerTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.skip(AddNonceToAuthorizationEndpointRequest.class, "NOT adding nonce to request object");
	}

	@Override
	protected void performPostAuthorizationFlow() {
		onPostAuthorizationFlowComplete();
	}
}
