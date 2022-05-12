package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallUserInfoEndpoint;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.SetResourceMethodToPost;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-UserInfo-Header
@PublishTestModule(
	testName = "oidcc-userinfo-post-header",
	displayName = "OIDCC: make POST request to UserInfo endpoint with bearer header",
	summary = "This tests makes an authenticated POST request to the UserInfo endpoint with the access token in a header and validates the response",
	profile = "OIDCC"
)
public class OIDCCUserInfoPostHeader extends AbstractOIDCCUserInfoTest {

	@Override
	protected void callUserInfoEndpoint() {
		callAndStopOnFailure(SetResourceMethodToPost.class);
		callAndStopOnFailure(CallUserInfoEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCC-5.3.1");
		call(exec().mapKey("endpoint_response", "userinfo_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
	}

}
