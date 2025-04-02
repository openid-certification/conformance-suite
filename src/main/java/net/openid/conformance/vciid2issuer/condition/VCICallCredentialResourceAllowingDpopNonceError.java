package net.openid.conformance.vciid2issuer.condition;

import net.openid.conformance.condition.client.CallProtectedResourceAllowingDpopNonceError;

public class VCICallCredentialResourceAllowingDpopNonceError extends CallProtectedResourceAllowingDpopNonceError {

	@Override
	protected boolean requireJsonResponseBody() {
		return true;
	}
}
