package net.openid.conformance.vcpresentation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddResponseUriToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.EnsureIncomingRequestContentTypeIsFormUrlEncoded;
import net.openid.conformance.condition.client.ExtractVpToken;
import net.openid.conformance.condition.client.ParseVpTokenAsSdJwt;
import net.openid.conformance.condition.client.SerializeRequestObjectWithNullAlgorithm;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseModeToDirectPost;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeFromEnvironment;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToVpToken;
import net.openid.conformance.condition.common.EnsureIncomingTls12WithSecureCipherOrTls13;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsPost;
import net.openid.conformance.openid.AbstractOIDCCRequestUriServerTest;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@PublishTestModule(
	testName = "oidc4vp-happy-flow",
	displayName = "OIDC4VP: Unsigned request_uri",
	summary = "TBC",
	profile = "OIDC4VP",
	configurationFields = {
		"client.presentation_definition"
	}
)

public class VCPHappyFlowRequestUriUnsigned extends AbstractOIDCCRequestUriServerTest {
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence().
			replace(SetAuthorizationEndpointRequestResponseTypeFromEnvironment.class,
				sequenceOf(condition(SetAuthorizationEndpointRequestResponseTypeToVpToken.class),
					condition(AddResponseUriToAuthorizationEndpointRequest.class),
					condition(SetAuthorizationEndpointRequestResponseModeToDirectPost.class)));
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.randomAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, Condition.ConditionResult.WARNING);

		call(exec().unmapKey("client_request"));

		setStatus(Status.WAITING);

		if (path.equals("responseuri")) {
			return handleDirectPost(requestId);
		}
		return super.handleHttp(path, req, res, session, requestParts);
	}

	private Object handleDirectPost(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Direct post endpoint").mapKey("incoming_request", requestId));
		callAndContinueOnFailure(EnsureIncomingRequestMethodIsPost.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureIncomingRequestContentTypeIsFormUrlEncoded.class, Condition.ConditionResult.FAILURE);
		env.putObject("authorization_endpoint_response", (JsonObject) env.getElementFromObject("incoming_request", "body_form_params")); // FIXME put in a condition
		callAndStopOnFailure(ExtractVpToken.class, Condition.ConditionResult.FAILURE);

		// FIXME: verify query empty
		// FIXME: extract presentation_submission if it's a proper parameter
		// FIXME: verify no other parameters

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.2.2.5", "JARM-4.4-2");
		callAndStopOnFailure(ParseVpTokenAsSdJwt.class, Condition.ConditionResult.FAILURE);

		// FIXME: verify holder binding

		// FIXME: verify sig on sd jwt (lissi used did:jwk though)

		// FIXME: verify credential contents?

		// FIXME: verify iat / exp / whatever

		//setStatus(Status.WAITING);
		fireTestFinished();

		// as per https://openid.bitbucket.io/connect/openid-4-verifiable-presentations-1_0.html#section-6.2
		JsonObject response = new JsonObject();
		response.addProperty("redirect_uri", env.getString("redirect_uri"));

		return ResponseEntity.ok()
			.contentType(DATAUTILS_MEDIATYPE_APPLICATION_JOSE)
			.body(response.toString());
	}

	public static class CreateAuthorizationRedirectSteps extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

			callAndStopOnFailure(SerializeRequestObjectWithNullAlgorithm.class);

			callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class);
		}

	}

	@Override
	protected void createAuthorizationRedirect() {
		call(new CreateAuthorizationRedirectSteps());
	}
}
