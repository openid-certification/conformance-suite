package net.openid.conformance.openid;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.BuildPlainRedirectToAuthorizationEndpointSpaceAsPlus;
import net.openid.conformance.condition.client.CheckMatchingCallbackParameters;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureAuthorizationEndpointAcceptedSpaceAsPlus;
import net.openid.conformance.condition.client.ExtractAccessTokenFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import net.openid.conformance.condition.client.ValidateIssIfPresentInAuthorizationResponse;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-ensure-space-as-plus-in-scope-succeeds",
	displayName = "OIDCC: ensure a space encoded as '+' in scope is accepted",
	summary = "This test performs a happy flow with the space in the 'scope' parameter encoded as '+' instead of"
		+ " '%20'. RFC 6749 section 4.1.1 requires the authorization request query to use the"
		+ " application/x-www-form-urlencoded format, in which both are encodings of a space (appendix B's own"
		+ " example uses '+'), so the server must decode 'openid+email' as 'openid email'. If the authorization"
		+ " server returns an error the test ends with a warning; otherwise the flow is expected to complete as in"
		+ " the 'scope email' test.",
	profile = "OIDCC"
)
public class OIDCCEnsureSpaceAsPlusInScopeSucceeds extends OIDCCScopeEmail {

	@Override
	protected void createAuthorizationRedirect() {
		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpointSpaceAsPlus.class, "RFC6749-4.1.1", "RFC6749A-B");
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		callAndContinueOnFailure(CheckMatchingCallbackParameters.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateIssIfPresentInAuthorizationResponse.class, Condition.ConditionResult.FAILURE, "OAuth2-iss-2");
		callAndContinueOnFailure(EnsureAuthorizationEndpointAcceptedSpaceAsPlus.class, Condition.ConditionResult.WARNING, "RFC6749-4.1.1", "RFC6749A-B");
		if (!Strings.isNullOrEmpty(env.getString("authorization_endpoint_response", "error"))) {
			// the server rejected the request; nothing further can be tested
			fireTestFinished();
			return;
		}
		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
		if (responseType.includesCode()) {
			callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);
		}
		if (responseType.includesToken()) {
			callAndStopOnFailure(ExtractAccessTokenFromAuthorizationResponse.class);
		}
		handleSuccessfulAuthorizationEndpointResponse();
	}

}
