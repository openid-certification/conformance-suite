package net.openid.conformance.condition.client;

public class EnsurePARInvalidRequestOrInvalidDpopProof extends AbstractEnsureSpecifiedErrorFromPushedAuthorizationEndpointResponse {
	@Override
	protected String[] getExpectedError() {
		return new String[]{
			"invalid_request",
			"invalid_dpop_proof"
		};
	}
}
