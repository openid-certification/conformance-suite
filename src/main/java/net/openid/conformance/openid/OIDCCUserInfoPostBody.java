package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallUserInfoEndpointWithBearerTokenInBody;
import net.openid.conformance.condition.client.UserInfoEndpointWithAccessTokenInBodyNotSupported;
import net.openid.conformance.testmodule.PublishTestModule;
import org.springframework.http.HttpStatus;

// Corresponds to OP-UserInfo-Body
@PublishTestModule(
	testName = "oidcc-userinfo-post-body",
	displayName = "OIDCC: make POST request to UserInfo endpoint with access token in body",
	summary = "This tests makes an authenticated POST request to the UserInfo endpoint with the access token in the body and validates the response. Support for passing an access token in the request body is not required by the standards - if is acceptable for servers not to implement this form, and the test will complete with a 'warning' if the server returns a http error response.",
	profile = "OIDCC"
)
public class OIDCCUserInfoPostBody extends AbstractOIDCCUserInfoTest {

	@Override
	protected void onPostAuthorizationFlowComplete() {

		callUserInfoEndpoint();

		int statusCode = env.getInteger("userinfo_endpoint_response_full", "status");
		if (!HttpStatus.valueOf(statusCode).is2xxSuccessful()) {
			callAndContinueOnFailure(UserInfoEndpointWithAccessTokenInBodyNotSupported.class, Condition.ConditionResult.WARNING);
		} else {
			extractUserInfoResponse();
			validateExtractedUserInfoResponse();
		}

		fireTestFinished();
	}

	@Override
	protected void callUserInfoEndpoint() {
		callAndStopOnFailure(CallUserInfoEndpointWithBearerTokenInBody.class, "OIDCC-5.3.1");
	}

}
