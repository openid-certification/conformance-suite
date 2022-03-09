package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.condition.client.WaitFor30Seconds;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-codereuse-30seconds",
	displayName = "OIDCC: Authorization code reuse with a 30 second delay",
	summary = "This test tries using an authorization code for a second time, 30 seconds after the first use. The server must return an invalid_grant error as the authorization code has already been used. The originally issued access token should be revoked (as per RFC6749-4.1.2) - a warning is issued if the access token still works.",
	profile = "OIDCC"
)
// Equivalent of https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_OAuth_2nd_30s
public class OIDCCAuthCodeReuseAfter30Seconds extends AbstractOIDCCAuthCodeReuse {

	@Override
	protected void testReuseOfAuthorizationCode() {
		callAndStopOnFailure(WaitFor30Seconds.class);
		super.testReuseOfAuthorizationCode();
	}

	@Override
	protected void checkResponse() {
		super.checkResponse();
		eventLog.endBlock();
		eventLog.startBlock("Testing if access token was revoked after authorization code reuse (the AS 'should' have revoked the access token)");
		callAndStopOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE, "RFC6749-4.1.2");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));

		callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, Condition.ConditionResult.WARNING, "RFC6749-4.1.2", "RFC6750-3.1");

		call(exec().unmapKey("endpoint_response"));
	}
}
