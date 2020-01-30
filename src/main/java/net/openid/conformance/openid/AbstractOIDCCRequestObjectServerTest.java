package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddAudToRequestObject;
import net.openid.conformance.condition.client.AddIssToRequestObject;
import net.openid.conformance.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.SerializeRequestObjectWithNullAlgorithm;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class AbstractOIDCCRequestObjectServerTest extends AbstractOIDCCServerTest {

	public static class CreateAuthorizationRedirectSteps extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

			callAndStopOnFailure(AddAudToRequestObject.class);

			callAndStopOnFailure(AddIssToRequestObject.class);

			callAndStopOnFailure(SerializeRequestObjectWithNullAlgorithm.class);

			callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);
		}

	}

	@Override
	protected void createAuthorizationRedirect() {
		call(new CreateAuthorizationRedirectSteps());
	}

	protected void skipTestIfRequestObjectNotSupported() {
		String error = env.getString("authorization_endpoint_response", "error");
		if (error != null && error.equals("request_not_supported")) {
			fireTestSkipped("The 'request_not_supported' error from the authorization endpoint indicates that it does not support request objects, so request objects cannot be tested.");
		}
	}
}
