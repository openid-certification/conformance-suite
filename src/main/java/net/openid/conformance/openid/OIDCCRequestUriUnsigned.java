package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddRequestUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestUriParameterSupported;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.SerializeRequestObjectWithNullAlgorithm;
import net.openid.conformance.condition.common.CreateRandomRequestUri;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
public class OIDCCRequestUriUnsigned extends AbstractOIDCCServerTest {

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();

		callAndStopOnFailure(CreateRandomRequestUri.class, "OIDCC-6.2");
		callAndStopOnFailure(AddRequestUriToDynamicRegistrationRequest.class);
	}


	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {

		super.onConfigure(config, baseUrl);
		callAndContinueOnFailure(CheckDiscEndpointRequestUriParameterSupported.class, Condition.ConditionResult.WARNING, "OIDCD-3");

	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		if (path.equals(env.getString("request_uri", "path"))) {
			return handleRequestUriRequest(requestParts);
		}
		return super.handleHttp(path, req, res, session, requestParts);

	}

	private Object handleRequestUriRequest(JsonObject requestParts) {
		String requestObject = env.getString("request_object");

		return ResponseEntity.ok()
			.contentType(DATAUTILS_MEDIATYPE_APPLICATION_JOSE)
			.body(requestObject);
	}

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
