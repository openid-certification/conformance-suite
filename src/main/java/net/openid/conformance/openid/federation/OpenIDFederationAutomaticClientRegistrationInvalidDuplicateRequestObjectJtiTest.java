package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObjectOrInvalidClient;
import net.openid.conformance.condition.common.ExpectInvalidRequestOrInvalidRequestObjectErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;

@PublishTestModule(
	testName = "openid-federation-automatic-client-registration-invalid-duplicate-request-object-jti",
	displayName = "OpenID Federation OP test: Invalid request object due to reused jti",
	summary = "The test acts as an RP wanting to perform automatic client registration with an OP, " +
		"deliberately not including a jti in the authorization request object." +
		"<br/><br/>" +
		"The test will make two authorization requests - the second one with the same jti claim in the " +
		"request object as the first one. " +
		"If the server does not return an invalid_request, invalid_request_object or a similar well-defined " +
		"and appropriate error back to the client on the second request, it must show an error page saying " +
		"the request is invalid due to an already used jti claim — upload a screenshot of the error page.",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationInvalidDuplicateRequestObjectJtiTest extends OpenIDFederationAutomaticClientRegistrationTest {

	private boolean secondRound = false;

	@Override
	protected FAPIAuthRequestMethod getRequestMethod() {
		return FAPIAuthRequestMethod.BY_VALUE;
	}

	@Override
	protected HttpMethod getHttpMethodForAuthorizeRequest() {
		return HttpMethod.GET;
	}

	@Override
	protected void buildRequestObject() {
		String jtiFromPreviousRound = env.getString("request_object_claims", "jti");
		super.buildRequestObject();
		if (secondRound) {
			env.putString("request_object_claims", "jti", jtiFromPreviousRound);
		}
	}

	@Override
	protected void createPlaceholder() {
		callAndContinueOnFailure(ExpectInvalidRequestOrInvalidRequestObjectErrorPage.class, Condition.ConditionResult.FAILURE);
		env.putString("error_callback_placeholder", env.getString("invalid_request_error"));
	}

	@Override
	protected void redirect(HttpMethod method) {
		if (secondRound) {
			performRedirectAndWaitForPlaceholdersOrCallback("error_callback_placeholder", method.name());
		} else {
			performRedirect(method.name());
		}
	}

	@Override
	protected void processCallback() {
		if (secondRound) {
			env.mapKey("authorization_endpoint_response", "callback_query_params");
			performGenericAuthorizationEndpointErrorResponseValidation();
			callAndContinueOnFailure(CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObjectOrInvalidClient.class,
				Condition.ConditionResult.WARNING, "OIDFED-12.1.3");
			fireTestFinished();
		} else {
			secondRound = true;
			makeAuthorizationRequest();
		}
	}

}
