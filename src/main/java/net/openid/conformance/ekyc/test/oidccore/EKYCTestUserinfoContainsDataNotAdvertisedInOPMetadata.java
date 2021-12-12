package net.openid.conformance.ekyc.test.oidccore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureInvalidRequestError;
import net.openid.conformance.ekyc.condition.client.AddVerifiedClaimsFromUserinfoNotFoundInOPMetadataToAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.ExtractVerifiedClaimsFromUserinfoResponse;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ekyc-server-test-userinfo-notfoundinop",
	displayName = "OpenID for IDA Server Test - Provided userinfo contains elements not advertised in OP metadata",
	summary = "This test builds the verified_claims request using the userinfo data provided in test configuration " +
		"and expects a happy path flow, i.e the request must succeed, and returned data must match the provided userinfo. " +
		"This test will be skipped if userinfo data is not provided in configuration.",
	profile = "OIDCC",
	configurationFields = {
		"ekyc_userinfo"
	}
)
public class EKYCTestUserinfoContainsDataNotAdvertisedInOPMetadata extends AbstractEKYCTestWithOIDCCore {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		JsonElement userinfo = env.getElementFromObject("config", "ekyc_userinfo");
		if (userinfo==null) {
			//the test will stop here
			fireTestSkipped("Skipping test as a userinfo json was not provided.");
		}
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		JsonElement error = env.getElementFromObject("authorization_endpoint_response", "error");
		if(error==null) {
			super.onAuthorizationCallbackResponse();
		} else {
			//server returned an error, this may happen if we requested something that's completely wrong.
			//error conditions and what needs to be done in such cases are not clearly defined
			//TODO clarify if this should always fail or if it's up to the OP. and if allowing only invalid_request is correct
			callAndContinueOnFailure(EnsureInvalidRequestError.class, Condition.ConditionResult.FAILURE);
			fireTestFinished();
		}
	}

	@Override
	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndStopOnFailure(AddVerifiedClaimsFromUserinfoNotFoundInOPMetadataToAuthorizationEndpointRequest.class);
		String dataNotFoundInOpmetadata = env.getString("userinfo_contains_data_notfoundin_opmetadata");
		if("no".equals(dataNotFoundInOpmetadata)) {
			//the test will stop here. nothing to test
			fireTestSkipped("Skipping test as userinfo does not contain claims and/or verification data not found in OP metadata");
		}
	}

	@Override
	protected void processVerifiedClaimsInIdToken() {
		//we don't request anything in id_token
	}

	@Override
	protected void validateUserinfoVerifiedClaimsAgainstRequested() {
		//don't do anything as the OP is expected to NOT to return all requested data.
		// if it returns data not advertised in OP metadata then ensureReturnedVerifiedClaimsMatchOPMetadata will fail
	}

	@Override
	protected void processVerifiedClaimsInUserinfo() {
		callAndContinueOnFailure(ExtractVerifiedClaimsFromUserinfoResponse.class, Condition.ConditionResult.FAILURE, "IA-5");
		JsonObject verifiedClaimsResponse = env.getObject("verified_claims_response");
		//The OP may not include verified_claims at all,
		// if it would be empty because we requested unsupported things then the OP must omit verified_claims completely
		if(verifiedClaimsResponse!=null) {
			validateVerifiedClaimsResponseSchema();
			ensureReturnedVerifiedClaimsMatchOPMetadata(true);
			validateUserinfoVerifiedClaimsAgainstRequested();
		}
	}
}
