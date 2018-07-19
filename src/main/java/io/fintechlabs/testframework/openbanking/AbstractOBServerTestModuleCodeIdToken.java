package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.CheckForAuthorizationEndpointErrorInQueryForHybridFLow;
import io.fintechlabs.testframework.condition.client.CheckForSubscriberInIdToken;
import io.fintechlabs.testframework.condition.client.ExtractAtHash;
import io.fintechlabs.testframework.condition.client.ExtractCHash;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractImplicitHashToCallbackResponse;
import io.fintechlabs.testframework.condition.client.ExtractSHash;
import io.fintechlabs.testframework.condition.client.OBValidateIdTokenIntentId;
import io.fintechlabs.testframework.condition.client.RejectAuthCodeInUrlQuery;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import io.fintechlabs.testframework.condition.client.ValidateAtHash;
import io.fintechlabs.testframework.condition.client.ValidateCHash;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenNonce;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateSHash;
import io.fintechlabs.testframework.condition.common.CreateRandomImplicitSubmitUrl;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;

public abstract class AbstractOBServerTestModuleCodeIdToken extends AbstractOBServerTestModule {

	private static final Logger logger = LoggerFactory.getLogger(AbstractOBServerTestModuleCodeIdToken.class);

	public AbstractOBServerTestModuleCodeIdToken(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(id, owner, eventLog, browser, testInfo);
	}

	@Override
	protected void createAuthorizationRequest() {

		super.createAuthorizationRequest();

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);
	}

	@Override
	protected void requestAuthorizationCode() {

		super.requestAuthorizationCode();

		call(ExtractSHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");

		skipIfMissing(new String[] { "state_hash" }, new String[] {}, ConditionResult.INFO,
			ValidateSHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");

		call(ExtractCHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		skipIfMissing(new String[] { "c_hash" }, new String[] {}, ConditionResult.FAILURE ,
			ValidateCHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		call(ExtractAtHash.class, ConditionResult.INFO, "OIDCC-3.3.2.11");

		skipIfMissing(new String[] { "at_hash" }, new String[] {}, ConditionResult.INFO,
			ValidateAtHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		logIncomingHttpRequest(path, requestParts);

		// dispatch based on the path

		if (path.equals("callback")) {
			return handleCallback(requestParts);
		} else if (path.equals(env.getString("implicit_submit", "path"))) {
			return handleImplicitSubmission(requestParts);
		} else {
			throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
		}
	}

	@UserFacing
	private Object handleCallback(JsonObject requestParts) {

		setStatus(Status.RUNNING);

		env.put("callback_query_params", requestParts.get("params").getAsJsonObject());

		call(RejectAuthCodeInUrlQuery.class, ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		skipIfMissing(new String[] { "callback_query_params" }, new String[] {}, ConditionResult.INFO,
				CheckForAuthorizationEndpointErrorInQueryForHybridFLow.class, ConditionResult.FAILURE, "OIDCC-3.3.2.6");
		
		callAndStopOnFailure(CreateRandomImplicitSubmitUrl.class);

		setStatus(Status.WAITING);

		String submissionUrl = env.getString("implicit_submit", "fullUrl");
		logger.info("Sending JS to user's browser to submit URL fragment (hash) to " + submissionUrl);

		return new ModelAndView("implicitCallback",
			ImmutableMap.of(
				"implicitSubmitUrl", env.getString("implicit_submit", "fullUrl"),
				"returnUrl", "/log-detail.html?log=" + getId()
			));
	}

	private Object handleImplicitSubmission(JsonObject requestParts) {

		// process the callback
		setStatus(Status.RUNNING);

		JsonElement body = requestParts.get("body");

		if (body != null) {
			String hash = body.getAsString();

			logger.info("URL fragment (hash): " + hash);

			env.putString("implicit_hash", hash);
		} else {
			logger.warn("No hash/URL fragment submitted");

			env.putString("implicit_hash", ""); // Clear any old value
		}

		callAndStopOnFailure(ExtractImplicitHashToCallbackResponse.class);

		return onAuthorizationCallbackResponse();
	}

	/* Check the ID token and state hash before moving on to the rest of the test
	 */
	@Override
	protected Object performPostAuthorizationFlow() {
		callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class, "FAPI-2-5.2.2-3");

		callAndStopOnFailure(ValidateIdToken.class, "FAPI-2-5.2.2-3");

		callAndStopOnFailure(ValidateIdTokenNonce.class,"OIDCC-2");

		callAndStopOnFailure(OBValidateIdTokenIntentId.class,"OIDCC-2");

		callAndStopOnFailure(ValidateIdTokenSignature.class, "FAPI-2-5.2.2-3");

		callAndStopOnFailure(CheckForSubscriberInIdToken.class, "FAPI-1-5.2.2-24", "OB-5.2.2-8");

		call(ExtractSHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");

		skipIfMissing(new String[] { "state_hash" }, new String[] {}, ConditionResult.INFO,
			ValidateSHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");
		
		call(ExtractCHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		skipIfMissing(new String[] { "c_hash" }, new String[] {}, ConditionResult.INFO,
			ValidateCHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");
		
		call(ExtractAtHash.class, ConditionResult.INFO, "OIDCC-3.3.2.11");

		skipIfMissing(new String[] { "at_hash" }, new String[] {}, ConditionResult.INFO,
			ValidateAtHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");		
		
		

		return super.performPostAuthorizationFlow();
		
	}

}
