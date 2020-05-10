package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallUserInfoEndpointWithBearerToken;
import net.openid.conformance.condition.client.EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources;
import net.openid.conformance.condition.client.EnsureUserInfoBirthDateValid;
import net.openid.conformance.condition.client.EnsureUserInfoContainsSub;
import net.openid.conformance.condition.client.EnsureUserInfoUpdatedAtValid;
import net.openid.conformance.condition.client.ExtractUserInfoFromUserInfoEndpointResponse;
import net.openid.conformance.condition.client.OIDCCCheckScopesSupportedContainScopeTest;
import net.openid.conformance.condition.client.ValidateUserInfoStandardClaims;
import net.openid.conformance.condition.client.VerifyScopesReturnedInAuthorizationEndpointIdToken;
import net.openid.conformance.condition.client.VerifyScopesReturnedInUserInfoClaims;
import net.openid.conformance.condition.client.VerifyUserInfoAndIdTokenInAuthorizationEndpointSameSub;
import net.openid.conformance.condition.client.VerifyUserInfoAndIdTokenInTokenEndpointSameSub;

/**
 * Base class for tests that request that the OP return particular claims
 */
public class AbstractOIDCCReturnedClaimsServerTest extends AbstractOIDCCServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		if (serverSupportsDiscovery()) {
			callAndContinueOnFailure(OIDCCCheckScopesSupportedContainScopeTest.class);

			Boolean scopesSupportedFlag = env.getBoolean("scopes_not_supported_flag");
			if (scopesSupportedFlag != null && scopesSupportedFlag) {
				fireTestSkipped("scopes_supported from the discovery endpoint indicates the server doesn't support the scopes required for this test; so the test has been skipped");
			}
		}
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {

		// Verify scopes returned in userinfo endpoint if we have access token
		// and otherwise in returned id_token from authorization endpoint
		if (responseType.includesCode() || responseType.includesToken()) {
			callUserInfoEndpoint();
			callAndStopOnFailure(ExtractUserInfoFromUserInfoEndpointResponse.class);
			validateUserInfoResponse();
		} else {
			validateIdTokenForResponseTypeIdToken();
		}

		fireTestFinished();
	}

	protected void validateIdTokenForResponseTypeIdToken() {
		callAndContinueOnFailure(VerifyScopesReturnedInAuthorizationEndpointIdToken.class, Condition.ConditionResult.WARNING, "OIDCC-5.4");
	}

	protected void callUserInfoEndpoint() {
		callAndStopOnFailure(CallUserInfoEndpointWithBearerToken.class, "OIDCC-5.3.1");
	}

	protected void validateUserInfoResponse() {
		callAndContinueOnFailure(ValidateUserInfoStandardClaims.class, Condition.ConditionResult.FAILURE, "OIDCC-5.1");
		callAndContinueOnFailure(EnsureUserInfoContainsSub.class, Condition.ConditionResult.FAILURE, "OIDCC-5.3.2");
		callAndContinueOnFailure(EnsureUserInfoBirthDateValid.class, Condition.ConditionResult.FAILURE, "OIDCC-5.1");
		callAndContinueOnFailure(EnsureUserInfoUpdatedAtValid.class, Condition.ConditionResult.FAILURE, "OIDCC-5.1");
		callAndContinueOnFailure(EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources.class, Condition.ConditionResult.FAILURE, "OIDCC-5.6.2");

		if (responseType.includesIdToken()) {
			callAndContinueOnFailure(VerifyUserInfoAndIdTokenInAuthorizationEndpointSameSub.class, Condition.ConditionResult.FAILURE, "OIDCC-5.3.2");

			if (responseType.includesCode()) {
				callAndContinueOnFailure(VerifyUserInfoAndIdTokenInTokenEndpointSameSub.class, Condition.ConditionResult.FAILURE,  "OIDCC-5.3.2");
			}
		}
		callAndContinueOnFailure(VerifyScopesReturnedInUserInfoClaims.class, Condition.ConditionResult.WARNING, "OIDCC-5.4");
	}

}
