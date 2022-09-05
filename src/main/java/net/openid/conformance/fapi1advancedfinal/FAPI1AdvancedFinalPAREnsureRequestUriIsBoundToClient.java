package net.openid.conformance.fapi1advancedfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddClientIdToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.EnsureInvalidRequestInvalidRequestObjectOrInvalidRequestUriError;
import net.openid.conformance.condition.client.ExpectInvalidRequestUriErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

//PAR-2.2.1 : The request_uri MUST be bound to the client that posted the authorization request.
@PublishTestModule(
	testName = "fapi1-advanced-final-par-attempt-to-use-request_uri-for-different-client",
	displayName = "PAR : try to use request_uri from client1 for client2",
	summary = "This test tries to use a request_uri (meant for client1) with client2 and expects authorization server to return an  error",
	profile = "FAPI1-Advanced-Final",
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
public class FAPI1AdvancedFinalPAREnsureRequestUriIsBoundToClient extends AbstractFAPI1AdvancedFinalServerTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		allowPlainErrorResponseForJarm = true;
	}

	@Override
	protected void performPARRedirectWithRequestUri() {
		eventLog.startBlock("Attempting to send client2's clientId with request_uri to AS and expect it returns error in callback");

		switchToSecondClient();
		callAndStopOnFailure(AddClientIdToAuthorizationEndpointRequest.class, "PAR-2.2.1");

		callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint.class);

		performRedirectAndWaitForPlaceholdersOrCallback();
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectInvalidRequestUriErrorPage.class, "PAR-3-3");

		env.putString("error_callback_placeholder", env.getString("request_uri_invalid_error"));

		eventLog.endBlock();
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		verifyError();

		eventLog.endBlock();

		fireTestFinished();
	}

	protected void verifyError() {
		callAndContinueOnFailure(EnsureInvalidRequestInvalidRequestObjectOrInvalidRequestUriError.class, Condition.ConditionResult.FAILURE, "PAR-3-3");
	}

}
