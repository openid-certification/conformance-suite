package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestParameterSupported;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.SerializeRequestObjectWithNullAlgorithm;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class AbstractOIDCCRequestObjectServerTest extends AbstractOIDCCServerTest {

	public static class CreateAuthorizationRedirectSteps extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

			callAndStopOnFailure(SerializeRequestObjectWithNullAlgorithm.class);

			callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);
		}

	}

	@Override
	protected void createAuthorizationRedirect() {
		call(new CreateAuthorizationRedirectSteps());
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		String error = env.getString("authorization_endpoint_response", "error");
		if (error != null && error.equals("request_not_supported")) {
			// we don't check if state is correct here, as state was only passed inside the request object and hence
			// we can't expect the OP to return it
			fireTestSkipped("The 'request_not_supported' error from the authorization endpoint indicates that it does not support request objects, so request objects cannot be tested.");
		}

		callAndContinueOnFailure(CheckDiscEndpointRequestParameterSupported.class, Condition.ConditionResult.WARNING);

		super.onAuthorizationCallbackResponse();
	}

}
