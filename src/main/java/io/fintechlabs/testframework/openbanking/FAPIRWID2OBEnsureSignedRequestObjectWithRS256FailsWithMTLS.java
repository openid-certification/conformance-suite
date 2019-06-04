package io.fintechlabs.testframework.openbanking;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import io.fintechlabs.testframework.condition.client.SetAccountScopeOnTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.TestCanOnlyBePerformedForPS256Alg;
import io.fintechlabs.testframework.testmodule.OIDFJSON;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-ob-ensure-signed-request-object-with-RS256-fails-with-mtls",
	displayName = "FAPI-RW-ID2-OB: ensure signed request object with RS256 fails (MTLS authentication)",
	summary = "This test should end with the authorisation server showing an an error message that the request object is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
	profile = "FAPI-RW-ID2-OB",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.jwks",
		"resource.resourceUrl",
		"resource.resourceUrlAccountRequests",
		"resource.resourceUrlAccountsResource",
		"resource.institution_id"
	}
)
public class FAPIRWID2OBEnsureSignedRequestObjectWithRS256FailsWithMTLS extends AbstractFAPIRWID2OBEnsureSignedRequestObjectWithRS256Fails {

	@Override
	protected void createClientCredentialsRequest() {

		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		callAndStopOnFailure(SetAccountScopeOnTokenEndpointRequest.class);

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}

	@Override
	protected void createAuthorizationCodeRequest() {

		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}

	@Override
	protected boolean logEndTestIfAlgIsNotPS256() {

		// ES256 keys are supplied, but we can't do this and the test module should probably just immediately exit successfully
		// We don't need to check null for jwks and keys because it was checked the steps before
		// We get first key to compare with PS256 because we use it to sign request_object or client_assertion
		JsonObject jwks = env.getObject("client_jwks");
		JsonArray keys = jwks.get("keys").getAsJsonArray();
		JsonObject key = keys.get(0).getAsJsonObject();
		String alg = OIDFJSON.getString(key.get("alg"));
		if (!alg.equals("PS256")) {
			callAndContinueOnFailure(TestCanOnlyBePerformedForPS256Alg.class, Condition.ConditionResult.FAILURE);
			fireTestFinished();
			return true;
		}

		return false;
	}
}
