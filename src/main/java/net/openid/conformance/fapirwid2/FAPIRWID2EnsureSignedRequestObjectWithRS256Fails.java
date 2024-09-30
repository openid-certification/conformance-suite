package net.openid.conformance.fapirwid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.ChangeClientJwksAlgToRS256;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestObjectError;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestObjectError;
import net.openid.conformance.condition.client.ExpectSignedRS256RequestObjectErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.util.JWKUtil;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-signed-request-object-with-RS256-fails",
	displayName = "FAPI-RW-ID2: ensure signed request object with RS256 fails",
	summary = "This test authenticates as normal except that the request object is signed using RS256. RS256 is not permitted by the FAPI-RW specification. The test must end with the authorization server showing an an error message that the request object is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
	profile = "FAPI-RW-ID2",
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
public class FAPIRWID2EnsureSignedRequestObjectWithRS256Fails extends AbstractFAPIRWID2ExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		// If ES256 keys are supplied, the test module should probably just immediately exit successfully
		// We don't need to check null for jwks and keys because it was checked the steps before
		// We get first key to compare with PS256 because we use it to sign request_object or client_assertion
		String alg = JWKUtil.getAlgFromClientJwks(env);
		if (!alg.equals("PS256")) { // FAPI only allows ES256 and PS256
			// This throws an exception: the test will stop here
			fireTestSkipped("This test requires RSA keys to be performed, the alg in client configuration is '%s' so this test is being skipped. If your server does not support PS256 then this will not prevent you certifying.".formatted(alg));
		}
		if(isPar) {
			allowPlainErrorResponseForJarm = true;
		}
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectSignedRS256RequestObjectErrorPage.class, "FAPI-RW-8.6");

		env.putString("error_callback_placeholder", env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected void createAuthorizationRequestObject() {
		// create a copy of the jwks so we can restore the original one when creating any client assertion
		env.putObject("client_jwks_rs256", env.getObject("client_jwks").deepCopy());
		env.mapKey("client_jwks", "client_jwks_rs256");
		callAndStopOnFailure(ChangeClientJwksAlgToRS256.class, "FAPI-RW-8.6");

		super.createAuthorizationRequestObject();

		env.unmapKey("client_jwks");
	}

	@Override
	protected void processParResponse() {
		// the server could reject this at the par endpoint, or at the authorization endpoint
		Integer http_status = env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status");
		if (http_status >= 200 && http_status < 300) {
			super.processParResponse();
			return;
		}

		callAndContinueOnFailure(EnsurePARInvalidRequestObjectError.class, Condition.ConditionResult.FAILURE, "JAR-6.2");

		fireTestFinished();
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		// We now have callback_query_params and callback_params (containing the hash) available, as well as authorization_endpoint_response (which test conditions should use if they're looking for the response)

		/* If we get an error back from the authorization server:
		 * - It must be a 'invalid_request_object' error
		 * - It must have the correct state we supplied
		 */

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(EnsureInvalidRequestObjectError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		fireTestFinished();
	}
}
