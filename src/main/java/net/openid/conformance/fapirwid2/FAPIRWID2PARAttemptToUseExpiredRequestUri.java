package net.openid.conformance.fapirwid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.EnsureInvalidRequestUriError;
import net.openid.conformance.condition.client.ExpectInvalidRequestUriErrorPage;
import net.openid.conformance.condition.client.WaitForExpiry;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

//PAR-2.2.0 : If the verification is successful, the server MUST generate a request URI and return a
// JSON response that contains request_uri and expires_in members at the top level with 201 Created
// HTTP response code.
@PublishTestModule(
	testName = "fapi-rw-id2-par-attempt-to-use-expired-request_uri",
	displayName = "PAR : try to use an expired request_uri",
	summary = "This test tries to use a request_uri which has expired and expects authorization server to return an error. The test will call the PAR endpoint to create a new request_uri, then sleep until the expiry time for that request_uri before attempting to use it (this means this test may take some minutes to run, depending on the lifetime of the request_uri).",
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
@VariantNotApplicable(parameter = FAPIAuthRequestMethod.class, values = {
	"by_value"
})
public class FAPIRWID2PARAttemptToUseExpiredRequestUri extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		allowPlainErrorResponseForJarm = true;
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectInvalidRequestUriErrorPage.class, "PAR-2.2");

		env.putString("error_callback_placeholder", env.getString("request_uri_invalid_error"));

		eventLog.endBlock();
	}

	@Override
	protected void performPARRedirectWithRequestUri() {
		//wait for expiry of request_uri
		eventLog.startBlock("Attempting use of request_uri after expiry and testing if Authorization server returns error in callback");

		waitForExpiresIn();

		callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint.class);

		performRedirectAndWaitForPlaceholdersOrCallback();
	}


	protected void waitForExpiresIn() {
		//expires_in : A JSON number that represents the lifetime of the request URI in seconds.
		// The request URI lifetime is at the discretion of the AS.
		Long seconds = env.getLong("expires_in");
		if (seconds > 30*60) {
			fireTestSkipped(
				"The expires in value is longer than 30 minutes so is too long for the suite to wait for it to expire.");
			return;
		}
		callAndStopOnFailure(WaitForExpiry.class);
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		verifyError();

		eventLog.endBlock();

		fireTestFinished();
	}

	protected void verifyError() {
		callAndContinueOnFailure(EnsureInvalidRequestUriError.class, Condition.ConditionResult.FAILURE, "PAR-2.2");
	}


}
