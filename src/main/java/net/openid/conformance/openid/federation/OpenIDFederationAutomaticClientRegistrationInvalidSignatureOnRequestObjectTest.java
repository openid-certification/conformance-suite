package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObject;
import net.openid.conformance.condition.client.InvalidateRequestObjectSignature;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.condition.common.ExpectInvalidRequestOrInvalidRequestObjectErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;

@PublishTestModule(
		testName = "openid-federation-automatic-client-registration-invalid-signature-on-request-object",
		displayName = "openid-federation-automatic-client-registration-invalid-signature-on-request-object",
		summary = "The test acts as an RP wanting to perform automatic client registration with an OP, " +
			"with JAR and HTTP GET to the authorization endpoint. The request object has an invalid signature and " +
			"must be rejected." +
			"<br/><br/>" +
			"If the server does not return an invalid_request, invalid_request_object or a similar well-defined " +
			"and appropriate error back to the client, it must show an error page saying the request is invalid due to " +
			"an invalid signature — upload a screenshot of the error page.",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationInvalidSignatureOnRequestObjectTest extends OpenIDFederationAutomaticClientRegistrationTest {

	@Override
	protected FAPIAuthRequestMethod getRequestMethod() {
		return FAPIAuthRequestMethod.BY_VALUE;
	}

	@Override
	protected HttpMethod getHttpMethodForAuthorizeRequest() {
		return HttpMethod.GET;
	}

	@Override
	protected void signRequestObject() {
		callAndContinueOnFailure(SignRequestObject.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(InvalidateRequestObjectSignature.class, Condition.ConditionResult.FAILURE);
	}

	@Override
	protected void createPlaceholder() {
		callAndContinueOnFailure(ExpectInvalidRequestOrInvalidRequestObjectErrorPage.class, Condition.ConditionResult.FAILURE);
		env.putString("error_callback_placeholder", env.getString("invalid_request_error"));
	}

	@Override
	protected void redirect(HttpMethod method) {
		performRedirectAndWaitForPlaceholdersOrCallback("error_callback_placeholder", method.name());
	}

	@Override
	protected void processCallback() {
		env.mapKey("authorization_endpoint_response", "callback_query_params");
		performGenericAuthorizationEndpointErrorResponseValidation();
		callAndContinueOnFailure(CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObject.class, Condition.ConditionResult.WARNING);
		fireTestFinished();
	}

}
