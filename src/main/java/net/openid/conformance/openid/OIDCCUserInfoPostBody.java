package net.openid.conformance.openid;

import net.openid.conformance.condition.client.CallUserInfoEndpointWithBearerTokenInBody;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-UserInfo-Body
@PublishTestModule(
	testName = "oidcc-userinfo-post-body",
	displayName = "OIDCC: make POST request to UserInfo endpoint with access token in body",
	summary = "This tests makes an authenticated POST request to the UserInfo endpoint with the access token in the body and validates the response",
	profile = "OIDCC",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client2.scope",
		"resource.resourceUrl"
	}
)
public class OIDCCUserInfoPostBody extends AbstractOIDCCUserInfoTest {

	protected void callUserInfoEndpoint() {
		callAndStopOnFailure(CallUserInfoEndpointWithBearerTokenInBody.class, "OIDCC-5.3.1");
		if (env.getInteger("userinfo_endpoint_response_code") == 405) {
			fireTestSkipped("Userinfo endpoint does not support POST; this cannot be tested");
		}
	}

}
