package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObjectOrInvalidClient;
import net.openid.conformance.condition.common.ExpectInvalidRequestOrInvalidRequestObjectErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;

@PublishTestModule(
		testName = "openid-federation-automatic-client-registration-invalid-missing-request-object-exp",
		displayName = "openid-federation-automatic-client-registration-invalid-missing-request-object-exp",
		summary = "The test acts as an RP wanting to perform automatic client registration with an OP, " +
			"deliberately not including exp in the authorization request object." +
			"<br/><br/>" +
			"If the server does not return an invalid_request, invalid_request_object or a similar well-defined " +
			"and appropriate error back to the client, it must show an error page saying the request is invalid due to " +
			"a missing exp claim — upload a screenshot of the error page.",
		profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationInvalidMissingRequestObjectExpTest extends OpenIDFederationAutomaticClientRegistrationTest {

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
		super.buildRequestObject();
		callAndContinueOnFailure(RemoveExpFromRequestObject.class, Condition.ConditionResult.FAILURE);
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
		callAndContinueOnFailure(CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObjectOrInvalidClient.class, Condition.ConditionResult.WARNING);
		fireTestFinished();
	}

}
