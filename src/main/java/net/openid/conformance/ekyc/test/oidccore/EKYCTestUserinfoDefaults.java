package net.openid.conformance.ekyc.test.oidccore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.ekyc.condition.client.AddVerifiedClaimsFromUserinfoToAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInUserinfoResponseAgainstRequest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ekyc-server-testbasedonuserinfo-defaults",
	displayName = "OpenID for IDA Server Test - Verified claims request based on userinfo provided in test configuration",
	summary = "This test builds the verified_claims request using the userinfo data provided in test configuration " +
		"and expects a happy path flow, i.e the request must succeed, and returned data must match the provided userinfo. " +
		"This test will be skipped if userinfo data is not provided.",
	profile = "OIDCC",
	configurationFields = {
		"ekyc_userinfo"
	}
)
public class EKYCTestUserinfoDefaults extends AbstractEKYCTestWithOIDCCore {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		JsonElement userinfo = env.getElementFromObject("config", "ekyc_userinfo");
		if (userinfo==null) {
			//the test will stop here
			fireTestSkipped("Skipping test as a userinfo json was not provided.");
		}
	}

	@Override
	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndContinueOnFailure(AddVerifiedClaimsFromUserinfoToAuthorizationEndpointRequest.class, Condition.ConditionResult.INFO);
	}

	@Override
	protected void processVerifiedClaimsInIdToken() {
		//we don't request anything in id_token
	}

	@Override
	protected void validateUserinfoVerifiedClaimsAgainstRequested() {
		callAndContinueOnFailure(ValidateVerifiedClaimsInUserinfoResponseAgainstRequest.class, Condition.ConditionResult.FAILURE, "IA-6");
	}
}
