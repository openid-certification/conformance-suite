package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestUriParameterSupported;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.SerializeRequestObjectWithNullAlgorithm;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-request-uri-unsigned-supported-correctly-or-rejected-as-unsupported",
	displayName = "OIDCC: Unsigned request_uri",
	summary = "This test calls the authorization endpoint as normal, but passes a request_uri that points at an unsigned jwt. The authorization server must successfully complete the authorization, or return a request_uri_not_supported error. This test will be skipped if none is not listed in the server's discovery endpoint request_object_signing_alg_values_supported.",
	profile = "OIDCC"
)
// https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_request_uri_Unsigned
@VariantNotApplicable(parameter = ClientRegistration.class, values={"static_client"})
public class OIDCCRequestUriUnsignedSupportedCorrectlyOrRejectedAsUnsupported extends AbstractOIDCCRequestUriServerTest {

	public static class CreateAuthorizationRedirectSteps extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

			callAndStopOnFailure(SerializeRequestObjectWithNullAlgorithm.class);

			callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint.class);
		}

	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		skipTestIfNoneUnsupported();
	}

	@Override
	protected void createAuthorizationRedirect() {
		call(new CreateAuthorizationRedirectSteps());
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		String error = env.getString("authorization_endpoint_response", "error");
		if (error != null && error.equals("request_uri_not_supported")) {
			// we don't check if state is correct here, as state was only passed inside the request object and hence
			// we can't expect the OP to return it
			fireTestSkipped("The 'request_uri_not_supported' error from the authorization endpoint indicates that it does not support request_uri (which is permitted behaviour), so request_uri cannot be tested.");
		}

		if (serverSupportsDiscovery()) {
			callAndContinueOnFailure(CheckDiscEndpointRequestUriParameterSupported.class, Condition.ConditionResult.WARNING);
		}

		super.onAuthorizationCallbackResponse();
	}

}
