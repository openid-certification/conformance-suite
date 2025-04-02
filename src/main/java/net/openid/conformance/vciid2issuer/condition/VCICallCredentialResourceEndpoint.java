package net.openid.conformance.vciid2issuer.condition;

import net.openid.conformance.condition.client.CallProtectedResource;

public class VCICallCredentialResourceEndpoint extends CallProtectedResource {

	@Override
	protected boolean requireJsonResponseBody() {
		return true;
	}
}
