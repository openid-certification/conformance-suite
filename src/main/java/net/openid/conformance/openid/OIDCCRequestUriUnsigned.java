package net.openid.conformance.openid;

import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.SerializeRequestObjectWithNullAlgorithm;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-request-uri-unsigned",
	displayName = "OIDCC: Unsigned request_uri",
	summary = "This test calls the authorization endpoint as normal, but passes a request_uri that points at an unsigned jwt. The authorization server must successfully complete the authorization, as request_uri is a mandatory to support feature for dynamic OPs as per OpenID Connect Core section 15.2.",
	profile = "OIDCC",
	configurationFields = {
		"server.discoveryUrl"
	}
)
// https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_request_uri_Unsigned
@VariantNotApplicable(parameter = ClientRegistration.class, values={"static_client"})
public class OIDCCRequestUriUnsigned extends AbstractOIDCCRequestUriServerTest {


	public static class CreateAuthorizationRedirectSteps extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

			callAndStopOnFailure(SerializeRequestObjectWithNullAlgorithm.class);

			callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint.class);
		}

	}

	@Override
	protected void createAuthorizationRedirect() {
		call(new CreateAuthorizationRedirectSteps());
	}

}
