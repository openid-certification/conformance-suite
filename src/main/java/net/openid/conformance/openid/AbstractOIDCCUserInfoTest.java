package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CallUserInfoEndpointWithBearerToken;
import net.openid.conformance.condition.client.CheckUserInfoEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources;
import net.openid.conformance.condition.client.EnsureUserInfoBirthDateValid;
import net.openid.conformance.condition.client.EnsureUserInfoContainsSub;
import net.openid.conformance.condition.client.ExtractUserInfoFromUserInfoEndpointResponse;
import net.openid.conformance.condition.client.ValidateUserInfoStandardClaims;
import net.openid.conformance.condition.client.VerifyUserInfoAndIdTokenInAuthorizationEndpointSameSub;
import net.openid.conformance.condition.client.VerifyUserInfoAndIdTokenInTokenEndpointSameSub;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

// can't call userinfo endpoint if there's no access token, so exclude response_type=id_token
@VariantNotApplicable(parameter = ResponseType.class, values={"id_token"})
public abstract class AbstractOIDCCUserInfoTest extends AbstractOIDCCServerTest {

	@Override
	protected void onPostAuthorizationFlowComplete() {
		callUserInfoEndpoint();
		callAndContinueOnFailure(CheckUserInfoEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-5.3.2");
		callAndStopOnFailure(ExtractUserInfoFromUserInfoEndpointResponse.class);
		validateUserInfoResponse();
		fireTestFinished();
	}

	protected void callUserInfoEndpoint() {
		callAndStopOnFailure(CallUserInfoEndpointWithBearerToken.class, "OIDCC-5.3.1");
	}

	protected void validateUserInfoResponse() {
		callAndContinueOnFailure(ValidateUserInfoStandardClaims.class, ConditionResult.FAILURE, "OIDCC-5.1");
		callAndContinueOnFailure(EnsureUserInfoContainsSub.class, ConditionResult.FAILURE, "OIDCC-5.3.2");
		callAndContinueOnFailure(EnsureUserInfoBirthDateValid.class, ConditionResult.FAILURE, "OIDCC-5.1");
		callAndContinueOnFailure(EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources.class, ConditionResult.FAILURE, "OIDCC-5.6.2");

		if (responseType.includesIdToken()) {
			callAndContinueOnFailure(VerifyUserInfoAndIdTokenInAuthorizationEndpointSameSub.class, ConditionResult.FAILURE, "OIDCC-5.3.2");

			if (responseType.includesCode()) {
				callAndContinueOnFailure(VerifyUserInfoAndIdTokenInTokenEndpointSameSub.class, ConditionResult.FAILURE,  "OIDCC-5.3.2");
			}
		}

	}

}
