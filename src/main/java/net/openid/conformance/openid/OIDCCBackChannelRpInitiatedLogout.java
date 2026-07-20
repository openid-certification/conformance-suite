package net.openid.conformance.openid;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddBackchannelLogoutSessionRequiredTrueToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddBackchannelLogoutUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInBackchannelLogoutRequest;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInPostLogoutRedirect;
import net.openid.conformance.condition.client.CheckIdTokenSidMatchesLogoutToken;
import net.openid.conformance.condition.client.CheckIdTokenSubMatchesLogoutToken;
import net.openid.conformance.condition.client.CheckLogoutTokenHasSubOrSid;
import net.openid.conformance.condition.client.CheckLogoutTokenNoNonce;
import net.openid.conformance.condition.client.CheckPostLogoutState;
import net.openid.conformance.condition.client.CreateBackchannelLogoutUri;
import net.openid.conformance.condition.client.ExtractLogoutTokenFromBackchannelLogoutRequest;
import net.openid.conformance.condition.client.ValidateLogoutTokenClaims;
import net.openid.conformance.condition.client.ValidateLogoutTokenFromBackchannelLogoutRequestEncryption;
import net.openid.conformance.condition.client.ValidateLogoutTokenSignature;
import net.openid.conformance.condition.common.EnsureIncomingTls12WithSecureCipherOrTls13;
import net.openid.conformance.testmodule.PublishTestModule;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Corresponds to https://www.heenan.me.uk/~joseph/2020-06-05-test_desc_op.html#OP_BackChannel_RpInitLogout
// https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-BackChannel-RpInitLogout.json
@PublishTestModule(
	testName = "oidcc-backchannel-rp-initiated-logout",
	displayName = "OIDCC: Backchannel rp initiated logout",
	summary = "This test performs a normal authorization flow at the OP, then sends the user to the end_session_endpoint. It validates the OP correctly calls the backchannel_logout_uri and sends the user to the post_logout_redirect_uri, then tries another authentication with prompt=none which must return an error (as the user has been logged out).\n\nIf using static client registration you must register backchannel_logout_uri to the same url as the redirect url, but replacing the portion after the alias with /backchannel_logout and similarly register /post_logout_redirect as a post_logout_redirect_uri.",
	profile = "OIDCC"
)
public class OIDCCBackChannelRpInitiatedLogout extends AbstractOIDCCRpInitiatedLogout {
	private JsonObject postLogoutRedirectRequestParts = null;
	private JsonObject backchannelLogoutRequestParts = null;

	@Override
	protected void configureClient() {
		callAndStopOnFailure(CreateBackchannelLogoutUri.class, "OIDCBCL-2.2");
		super.configureClient();
	}

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(AddBackchannelLogoutSessionRequiredTrueToDynamicRegistrationRequest.class, "OIDCBCL-2.2");
		callAndStopOnFailure(AddBackchannelLogoutUriToDynamicRegistrationRequest.class, "OIDCBCL-2.2");
	}

	protected void validateLogoutResults(){
		String bcLogoutEnvKey = "backchannel_logout_request";

		env.putObject("post_logout_redirect", postLogoutRedirectRequestParts);
		env.putObject(bcLogoutEnvKey, backchannelLogoutRequestParts);

		eventLog.startBlock("Verify backchannel logout request");

		env.mapKey("client_request", bcLogoutEnvKey);
		// This is a mixture of must & recommended in BCP195, but BCP195 is not a normative reference of OIDCC so only raise a warning
		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, Condition.ConditionResult.WARNING, "BCP195-3.1.1");

		skipIfMissing(new String[]{"client_jwks"}, null, Condition.ConditionResult.INFO,
			ValidateLogoutTokenFromBackchannelLogoutRequestEncryption.class, Condition.ConditionResult.WARNING, "OIDCBCL-2.4");
		callAndStopOnFailure(ExtractLogoutTokenFromBackchannelLogoutRequest.class, "OIDCBCL-2.5");
		callAndContinueOnFailure(CheckForUnexpectedParametersInBackchannelLogoutRequest.class, Condition.ConditionResult.WARNING, "OIDCBCL-2.5");
		callAndContinueOnFailure(ValidateLogoutTokenSignature.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");
		callAndContinueOnFailure(ValidateLogoutTokenClaims.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");
		skipIfElementMissing("logout_token", "claims.sub",
			Condition.ConditionResult.INFO, CheckIdTokenSubMatchesLogoutToken.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");
		skipIfElementMissing("logout_token", "claims.sid",
			Condition.ConditionResult.INFO, CheckIdTokenSidMatchesLogoutToken.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");
		callAndContinueOnFailure(CheckLogoutTokenNoNonce.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");
		callAndContinueOnFailure(CheckLogoutTokenHasSubOrSid.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");

		env.unmapKey("client_request");

		eventLog.endBlock();

		eventLog.startBlock("Verify frontchannel post logout redirect");
		callAndContinueOnFailure(CheckPostLogoutState.class, Condition.ConditionResult.FAILURE, "OIDCRIL-2");
		callAndContinueOnFailure(CheckForUnexpectedParametersInPostLogoutRedirect.class, Condition.ConditionResult.WARNING, "OIDCRIL-3");

		eventLog.endBlock();

		// do the prompt=none authorization request to check logout happened
		performAuthorizationFlow();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.equals("backchannel_logout")) {
			return handleBackchannelLogout(requestParts);
		} else if (path.equals("post_logout_redirect")) {
			return handlePostLogoutRedirect(requestParts);
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	protected Object handlePostLogoutRedirect(JsonObject requestParts) {
		setStatus(Status.RUNNING);
		postLogoutRedirectRequestParts = requestParts;
		if (backchannelLogoutRequestParts == null) {
			eventLog.log(getName(), args("msg", "Received front channel redirect; waiting for back channel request"));
		} else {
			validateLogoutResultsInBackground();
		}
		setStatus(Status.WAITING);
		return new ModelAndView("resultCaptured",
			ImmutableMap.of(
				"returnUrl", "/log-detail.html?log=" + getId()
			));
	}

	protected Object handleBackchannelLogout(JsonObject requestParts) {
		setStatus(Status.RUNNING);
		backchannelLogoutRequestParts = requestParts;
		if (postLogoutRedirectRequestParts == null) {
			eventLog.log(getName(), args("msg", "Received backchannel request; waiting for front channel redirect"));
		} else {
			validateLogoutResultsInBackground();
		}
		setStatus(Status.WAITING);

		// as per https://openid.net/specs/openid-connect-backchannel-1_0.html#BCResponse
		// we always return a successful response as this test expects a valid request
		HttpHeaders headers = new HttpHeaders();
		headers.set("Cache-Control", "no-store");
		return new ResponseEntity<Object>("", headers, HttpStatus.OK);
	}

	private void validateLogoutResultsInBackground() {
		getTestExecutionManager().runInBackground(() -> {
			setStatus(Status.RUNNING);
			validateLogoutResults();
			return "done";
		});
	}

}
