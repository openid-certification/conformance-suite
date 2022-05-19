package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CallUserInfoEndpoint;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources;
import net.openid.conformance.condition.client.EnsureUserInfoContainsSub;
import net.openid.conformance.condition.client.EnsureUserInfoUpdatedAtValid;
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
		extractUserInfoResponse();
		validateExtractedUserInfoResponse();
		fireTestFinished();
	}

	protected void extractUserInfoResponse() {
		call(exec().mapKey("endpoint_response", "userinfo_endpoint_response_full"));
		callAndContinueOnFailure(EnsureContentTypeJson.class, ConditionResult.FAILURE, "OIDCC-5.3.2");
		call(exec().unmapKey("endpoint_response"));
		callAndStopOnFailure(ExtractUserInfoFromUserInfoEndpointResponse.class);
	}

	protected void callUserInfoEndpoint() {
		callAndStopOnFailure(CallUserInfoEndpoint.class, "OIDCC-5.3.1");
		call(exec().mapKey("endpoint_response", "userinfo_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
	}

	protected void validateExtractedUserInfoResponse() {
		callAndContinueOnFailure(ValidateUserInfoStandardClaims.class, ConditionResult.FAILURE, "OIDCC-5.1");
		callAndContinueOnFailure(EnsureUserInfoContainsSub.class, ConditionResult.FAILURE, "OIDCC-5.3.2");
		callAndContinueOnFailure(EnsureUserInfoUpdatedAtValid.class, Condition.ConditionResult.FAILURE, "OIDCC-5.1");
		callAndContinueOnFailure(EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources.class, ConditionResult.FAILURE, "OIDCC-5.6.2");

		if (responseType.includesIdToken()) {
			callAndContinueOnFailure(VerifyUserInfoAndIdTokenInAuthorizationEndpointSameSub.class, ConditionResult.FAILURE, "OIDCC-5.3.2");

			if (responseType.includesCode()) {
				callAndContinueOnFailure(VerifyUserInfoAndIdTokenInTokenEndpointSameSub.class, ConditionResult.FAILURE,  "OIDCC-5.3.2");
			}
		}

	}

}
