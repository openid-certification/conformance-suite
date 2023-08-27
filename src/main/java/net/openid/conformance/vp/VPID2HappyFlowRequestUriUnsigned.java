package net.openid.conformance.vp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.condition.client.CheckAudInBindingJwt;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInVpAuthorizationResponse;
import net.openid.conformance.condition.client.CheckIatInBindingJwt;
import net.openid.conformance.condition.client.CheckNonceInBindingJwt;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.CheckTypInBindingJwt;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.EnsureIncomingRequestContentTypeIsFormUrlEncoded;
import net.openid.conformance.condition.client.EnsureIncomingUrlQueryIsEmpty;
import net.openid.conformance.condition.client.ExtractAuthorizationEndpointResponseFromFormBody;
import net.openid.conformance.condition.client.ExtractVpToken;
import net.openid.conformance.condition.client.ParseVpTokenAsSdJwt;
import net.openid.conformance.condition.client.SerializeRequestObjectWithNullAlgorithm;
import net.openid.conformance.condition.client.ValidateCredentialJWTIat;
import net.openid.conformance.condition.client.ValidateSdJwtHolderBinding;
import net.openid.conformance.condition.common.CreateRandomRequestUri;
import net.openid.conformance.condition.common.EnsureIncomingTls12WithSecureCipherOrTls13;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsPost;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@PublishTestModule(
	testName = "oid4vp-happy-flow",
	displayName = "OID4VP: Unsigned request_uri",
	summary = "TBC",
	profile = "OID4VP-ID2",
	configurationFields = {
		"client.presentation_definition"
	}
)

public class VPID2HappyFlowRequestUriUnsigned extends AbstractVPServerTest {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(CreateRandomRequestUri.class, "OIDCC-6.2");
		super.onConfigure(config, baseUrl);
		browser.setShowQrCodes(true);
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
		callAndContinueOnFailure(EnsureIncomingUrlQueryIsEmpty.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(ExtractAuthorizationEndpointResponseFromFormBody.class, Condition.ConditionResult.FAILURE);
		// vp token may be an object containing multiple tokens, https://openid.net/specs/openid-4-verifiable-presentations-1_0-ID2.html#section-6.1
		// however I think we would only get multiple tokens if they were explicitly requested, so we can safely assme only a single token here
		callAndStopOnFailure(ExtractVpToken.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(CheckForUnexpectedParametersInVpAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.2.2.5");
		callAndStopOnFailure(ParseVpTokenAsSdJwt.class, Condition.ConditionResult.FAILURE);

		// FIXME: extract / verify presentation_submission

		eventLog.startBlock(currentClientString() + "Verify credential JWT");
		// as per https://www.ietf.org/id/draft-ietf-oauth-sd-jwt-vc-00.html#section-4.2.2.2 these must must not be selectively disclosed
		// FIXME check iss is a valid uri
		callAndContinueOnFailure(ValidateCredentialJWTIat.class, Condition.ConditionResult.FAILURE, "SDJWTVC-4.2.2.2");
		// FIXME nbf
		// FIXME exp
		// cnf is checked when holder binding is checked below
		// FIXME type
		// FIXME status

		eventLog.startBlock(currentClientString() + "Verify holder binding JWT");
		// https://www.ietf.org/archive/id/draft-ietf-oauth-selective-disclosure-jwt-05.html#name-key-binding-jwt

		callAndContinueOnFailure(ValidateSdJwtHolderBinding.class, Condition.ConditionResult.FAILURE, "SDJWT-5.10");

		callAndContinueOnFailure(CheckTypInBindingJwt.class, Condition.ConditionResult.FAILURE, "SDJWT-5.10");
		// alg is checked during signature validation

		callAndContinueOnFailure(CheckIatInBindingJwt.class, Condition.ConditionResult.FAILURE, "SDJWT-5.10");
		callAndContinueOnFailure(CheckAudInBindingJwt.class, Condition.ConditionResult.FAILURE, "SDJWT-5.10");
		callAndContinueOnFailure(CheckNonceInBindingJwt.class, Condition.ConditionResult.FAILURE, "SDJWT-5.10");

		// FIXME: verify sig on sd jwt (lissi use did:jwk though)

		// FIXME: verify credential contents?

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
