package net.openid.conformance.fapi2baselineid2;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddAllSupportedStandardClaimsToAuthorizationEndpointRequestIdTokenAndUserinfoClaims;
import net.openid.conformance.condition.client.AddIdentityClaimsFromUserInfo;
import net.openid.conformance.condition.client.AddRandomLocationClaimsToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CallUserInfoEndpoint;
import net.openid.conformance.condition.client.CheckForUnexpectedClaimsInIdToken;
import net.openid.conformance.condition.client.CheckForUnexpectedClaimsInUserinfo;
import net.openid.conformance.condition.client.CheckIfOidcStandardClaimsSupported;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureIdentityClaimsContainRequestedClaims;
import net.openid.conformance.condition.client.EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources;
import net.openid.conformance.condition.client.EnsureUserInfoContainsSub;
import net.openid.conformance.condition.client.EnsureUserInfoUpdatedAtValid;
import net.openid.conformance.condition.client.ExtractIdentityClaimsFromIdToken;
import net.openid.conformance.condition.client.ExtractUserInfoFromUserInfoEndpointResponse;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToUserInfoEndpoint;
import net.openid.conformance.condition.client.ValidateUserInfoStandardClaims;
import net.openid.conformance.condition.client.VerifyUserInfoAndIdTokenInTokenEndpointSameSub;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "fapi2-baseline-id2-test-claims-parameter-identity-claims",
	displayName = "FAPI2-Baseline-ID2: test requesting identity claims using the claims parameter",
	summary = "This is an optional test that will be skipped if the server's metadata does not indicate support for requesting identity claims using the claims parameter, as supporting these claims is not mandatory in the specification.\n\nThe test will request all claims listed in the claims_supported server metadata are returned in the id_token and userinfo (using a variety of different forms of request, and including requesting some random unknown claims that should be ignored), and will warn if any are not returned in either - the server is free to decide whether to only make them available via the id_token, via the userinfo endpoint (if it has one), or via both.\n\nThe test should be performed using a user which has all supported claims present on the server.",
	profile = "FAPI2-Baseline-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPI2BaselineID2TestClaimsParameterIdentityClaims extends AbstractFAPI2BaselineID2ServerTestModule {

	private boolean isClaimsParameterSupported() {
		JsonElement claimsSupportedEl = env.getElementFromObject("server", "claims_parameter_supported");
		boolean claimsSupported;
		if (claimsSupportedEl == null) {
			claimsSupported = false;
		} else if (!claimsSupportedEl.isJsonPrimitive() || !claimsSupportedEl.getAsJsonPrimitive().isBoolean()) {
			throw new TestFailureException(getId(), "'claims_parameter_supported' in the server metadata is not a boolean");
		} else {
			claimsSupported = OIDFJSON.getBoolean(claimsSupportedEl);
		}
		return claimsSupported;
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		if (!isClaimsParameterSupported()) {
			fireTestSkipped("The 'claims_parameter_supported' server metadata indicates that it does not support the claims parameter (which is permitted behaviour), so claims behaviour cannot be tested.");
		}
		callAndStopOnFailure(CheckIfOidcStandardClaimsSupported.class, "OIDCC-5.1");
		if (!env.getBoolean(CheckIfOidcStandardClaimsSupported.envVarAtLeastOneOidcStandardClaimSupport)) {
			fireTestSkipped("The 'claims_supported' server metadata does not contain any standard OpenID Connect claims (which is permitted behaviour), and means claims behaviour cannot be tested.");
		}
		return super.makeCreateAuthorizationRequestSteps()
			.then(condition(AddAllSupportedStandardClaimsToAuthorizationEndpointRequestIdTokenAndUserinfoClaims.class).requirements("OIDCC-5.1", "OIDCC-5.5", "OIDCD-3"))
			.then(condition(AddRandomLocationClaimsToAuthorizationEndpointRequest.class).requirements("OIDCC-5.5"));
	}

	@Override
	protected void requestProtectedResource() {
		callAndStopOnFailure(ExtractIdentityClaimsFromIdToken.class);

		if (env.getElementFromObject("server", "userinfo_endpoint") != null) {
			eventLog.startBlock(currentClientString() + "Call userinfo endpoint");
			callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

			if (isDpop) {
				callAndStopOnFailure(SetProtectedResourceUrlToUserInfoEndpoint.class); // so the 'htu' value is correct
				call(sequence(CreateDpopSteps.class));
			}

			callAndStopOnFailure(CallUserInfoEndpoint.class, "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");

			call(exec().mapKey("endpoint_response", "userinfo_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "OIDCC-5.3.2");
			call(exec().unmapKey("endpoint_response"));

			callAndStopOnFailure(ExtractUserInfoFromUserInfoEndpointResponse.class);

			callAndContinueOnFailure(ValidateUserInfoStandardClaims.class, Condition.ConditionResult.FAILURE, "OIDCC-5.1");
			callAndContinueOnFailure(CheckForUnexpectedClaimsInUserinfo.class, Condition.ConditionResult.WARNING, "OIDCC-5.1");
			callAndContinueOnFailure(EnsureUserInfoContainsSub.class, Condition.ConditionResult.FAILURE, "OIDCC-5.3.2");
			callAndContinueOnFailure(EnsureUserInfoUpdatedAtValid.class, Condition.ConditionResult.FAILURE, "OIDCC-5.1");
			callAndContinueOnFailure(EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources.class, Condition.ConditionResult.FAILURE, "OIDCC-5.6.2");

			env.mapKey("token_endpoint_id_token", "id_token");
			callAndContinueOnFailure(VerifyUserInfoAndIdTokenInTokenEndpointSameSub.class, Condition.ConditionResult.FAILURE,  "OIDCC-5.3.2");

			callAndStopOnFailure(AddIdentityClaimsFromUserInfo.class);

			eventLog.endBlock();
		} else {
			eventLog.log(getName(), "No userinfo_endpoint in server metadata; skipping calling userinfo endpoint");
		}

		callAndContinueOnFailure(EnsureIdentityClaimsContainRequestedClaims.class, Condition.ConditionResult.WARNING, "OIDCC-5.5");

		// We don't include this check in the more general PerformStandardIdTokenChecks as it could be pretty noisy
		callAndContinueOnFailure(CheckForUnexpectedClaimsInIdToken.class, Condition.ConditionResult.WARNING, "OIDCC-5.1");
	}
}
