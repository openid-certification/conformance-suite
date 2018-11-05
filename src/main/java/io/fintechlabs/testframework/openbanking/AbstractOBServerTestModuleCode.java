package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.ExtractImplicitHashToCallbackResponse;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import io.fintechlabs.testframework.condition.common.CreateRandomImplicitSubmitUrl;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;

public abstract class AbstractOBServerTestModuleCode extends AbstractOBServerTestModule {

	private static final Logger logger = LoggerFactory.getLogger(AbstractOBServerTestModuleCodeIdToken.class);

	@Override
	protected ResponseMode getResponseMode() {
		return ResponseMode.QUERY;
	}

	@Override
	protected void createAuthorizationRequest() {

		super.createAuthorizationRequest();

		logCodeFlowWarning();

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);
	}

	@Override
	protected void performTokenEndpointIdTokenExtraction() {
		performTokenEndpointIdTokenExtractionCode();
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
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

		env.putObject("callback_query_params", requestParts.get("params").getAsJsonObject());

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

		// FIXME: putting the url query into the callback_params is necessary until we change test conditions to look at
		// authorization_endpoint_response instead of callback_params
		env.putObject("callback_params", env.getObject("callback_query_params"));

		return onAuthorizationCallbackResponse();
	}

	protected void logCodeFlowWarning() {
		eventLog.log(getName(), args(
			"msg", "Risks have been identified with \"code\" flow that can be mitigated with hybrid (code id_token) flow",
			"result", ConditionResult.WARNING,
			"requirements", Sets.newHashSet("OB-3.4")
		));
		updateResultFromConditionFailure(ConditionResult.WARNING);
	}

}
