package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddAudToRequestObject;
import net.openid.conformance.condition.client.AddIssToRequestObject;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestObjectSigningAlgValuesSupportedContainsRS256;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-request-uri-signed-rs256",
	displayName = "OIDCC: RS256 signed request object passed by reference (request_uri)",
	summary = "This test calls the authorization endpoint as normal, but passes a request_uri that points at an RS256 signed JWS. The authorization server must successfully complete the authorization - whilst the OpenID Connect Core section does not require support of RS256 signed request objects, the 'dynamic' certification profile does.",
	profile = "OIDCC",
	configurationFields = {
		"server.discoveryUrl"
	}
)
// https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_request_uri_Sig
@VariantNotApplicable(parameter = ClientRegistration.class, values={"static_client"})
public class OIDCCRequestUriSignedRS256 extends AbstractOIDCCRequestUriServerTest {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		callAndContinueOnFailure(CheckDiscEndpointRequestObjectSigningAlgValuesSupportedContainsRS256.class, Condition.ConditionResult.FAILURE, "OIDCD-3");
	}

	public static class CreateAuthorizationRedirectSteps extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

			// aud/iss weren't sent in the python, but are recommended by the spec so we send them
			callAndStopOnFailure(AddAudToRequestObject.class, "OIDCC-6.1");

			callAndStopOnFailure(AddIssToRequestObject.class, "OIDCC-6.1");

			callAndStopOnFailure(SignRequestObject.class);

			callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint.class);
		}

	}

	@Override
	protected void createAuthorizationRedirect() {
		call(new CreateAuthorizationRedirectSteps());
	}

}
