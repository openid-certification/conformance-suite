package net.openid.conformance.ekyc.test.oidccore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.ekyc.condition.client.AddUserProvidedClaimsRequestToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "ekyc-server-testuserprovidedrequest",
	displayName = "eKYC Server Test - Using verified_claims request provided in test configuration",
	summary = "This test uses the verified_claims request provided in ekyc.verified_claims_request field " +
		"and expects a happy path flow, i.e the request must succeed.",
	profile = "OIDCC",
	configurationFields = {
		"ekyc.verified_claims_request_list",
		"ekyc.unverified_claims_names",
		"ekyc.verified_claims_names",
		"ekyc.request_schemas",
		"ekyc.response_schemas"
	}
)
public class EKYCTestWithUserProvidedRequest extends AbstractEKYCTestWithOIDCCore {

	private JsonArray requestList;

	@Override
	public void start() {
		// fetch request list which can be single object or list (in which case, we will perform the flow multiple times)
		// stuff the config into an array if it's a single object
		this.requestList = OIDFJSON.packJsonElementIntoJsonArray(env.getElementFromObject("config", "ekyc.verified_claims_request_list"));
		if(requestList.isEmpty()) { // make sure list is not empty
			throw new TestFailureException(getId(), "ekyc.verified_claims_request_list is not configured");
		}
		super.start();
	}

	@Override
	protected void performAuthorizationFlow() {
		// perform authorization flow using requestList as a queue
		if(!requestList.isEmpty()) {
			JsonElement requestElement = requestList.remove(0); // dequeue and save the first item
			if(requestElement.isJsonObject()) {  // each element must be a JSON object
				env.putObject("config", "ekyc.verified_claims_request", requestElement.getAsJsonObject());
				super.performAuthorizationFlow();
			} else {
				throw new TestFailureException(getId(), "ekyc.verified_claims_request items must be JSON objects");
			}
		}
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if(this.requestList.isEmpty()) {
			// end the test
			super.onPostAuthorizationFlowComplete();
		} else {
			// perform flow again
			performAuthorizationFlow();
		}
	}

	@Override
	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndContinueOnFailure(AddUserProvidedClaimsRequestToAuthorizationEndpointRequest.class, Condition.ConditionResult.WARNING, "IA-6");
	}

	@Override
	protected void processVerifiedClaimsInIdToken() {
		JsonElement userProvidedClaimsRequest = env.getElementFromObject("config", "ekyc.verified_claims_request.id_token.verified_claims");
		if (userProvidedClaimsRequest!=null) {
			//otherwise there won't be anything to process and will throw an error
			super.processVerifiedClaimsInIdToken();
		}
	}

	@Override
	protected void processVerifiedClaimsInUserinfo() {
		JsonElement userProvidedClaimsRequest = env.getElementFromObject("config", "ekyc.verified_claims_request.userinfo.verified_claims");
		if (userProvidedClaimsRequest!=null) {
			//otherwise there won't be anything to process and will throw an error
			super.processVerifiedClaimsInUserinfo();
		}
	}
}
