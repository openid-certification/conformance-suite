package net.openid.conformance.ekyc.test.oidccore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.ekyc.condition.client.AddOnlyOneSimpleVerifiedClaimToAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.AddUserProvidedClaimsRequestToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ekyc-server-testuserprovidedrequest",
	displayName = "eKYC Server Test - Using verified_claims request provided in test configuration",
	summary = "This test uses the verified_claims request provided in verified_claims_request field " +
		"and expects a happy path flow, i.e the request must succeed.",
	profile = "OIDCC",
	configurationFields = {
		"ekyc_verified_claims_request"
	}
)
public class EKYCTestWithUserProvidedRequest extends BaseEKYCTestWithOIDCCore {

	@Override
	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndContinueOnFailure(AddUserProvidedClaimsRequestToAuthorizationEndpointRequest.class, "IA-6");
	}

	@Override
	protected void processVerifiedClaimsInIdToken() {
		JsonElement userProvidedClaimsRequest = env.getElementFromObject("config", "ekyc_verified_claims_request.id_token.verified_claims");
		if (userProvidedClaimsRequest!=null) {
			//otherwise there won't be anything to process and will throw an error
			super.processVerifiedClaimsInIdToken();
		}
	}

	@Override
	protected void processVerifiedClaimsInUserinfo() {
		JsonElement userProvidedClaimsRequest = env.getElementFromObject("config", "ekyc_verified_claims_request.userinfo.verified_claims");
		if (userProvidedClaimsRequest!=null) {
			//otherwise there won't be anything to process and will throw an error
			super.processVerifiedClaimsInUserinfo();
		}
	}
}
