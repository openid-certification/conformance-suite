package net.openid.conformance.fapi;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.ExtractImplicitHashToCallbackResponse;
import net.openid.conformance.condition.common.CreateRandomImplicitSubmitUrl;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** The module is a general purpose module for collecting the result from the authorization endpoint
 *
 * TestModules using this class will receive the full result of the redirect in processCallback().
 */
public abstract class AbstractRedirectServerTestModule extends AbstractTestModule {
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		if (path.equals("callback")) {
			return handleCallback(requestParts);
		} else if (path.equals(env.getString("implicit_submit", "path"))) {
			return handleImplicitSubmission(requestParts);
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}

	}

	protected final void performRedirect() {
		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), args("msg", "Redirecting to authorization endpoint",
			"redirect_to", redirectTo,
			"http", "redirect"));

		setStatus(Status.WAITING);

		browser.goToUrl(redirectTo);
	}

	protected final void performRedirectAndWaitForErrorCallback() {
		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), args("msg", "Redirecting to authorization endpoint",
			"redirect_to", redirectTo,
			"http", "redirect"));

		createPlaceholder();

		setStatus(Status.WAITING);

		waitForPlaceholders();

		browser.goToUrl(redirectTo, env.getString("error_callback_placeholder"));
	}

	protected void createPlaceholder() {
		// Use for create new placeholder in subclass
		throw new TestFailureException(getId(), "Placeholder must be created for test " + getName());
	}

	@UserFacing
	private Object handleCallback(JsonObject requestParts) {

		setStatus(Status.RUNNING);

		env.putObject("callback_query_params", requestParts.get("query_string_params").getAsJsonObject());

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

	/**
	 * Called after the redirect response has been fully received
	 *
	 * 'callback_params' and 'callback_query_params' will be available in the environment
	 */
	abstract protected void processCallback();

	private Object handleImplicitSubmission(JsonObject requestParts) {

		getTestExecutionManager().runInBackground(() -> {

			// process the callback
			setStatus(Status.RUNNING);

			JsonElement body = requestParts.get("body");

			if (body != null) {
				String hash = OIDFJSON.getString(body);

				logger.info("URL fragment (hash): " + hash);

				env.putString("implicit_hash", hash);
			} else {
				logger.warn("No hash/URL fragment submitted");

				env.putString("implicit_hash", ""); // Clear any old value
			}

			callAndStopOnFailure(ExtractImplicitHashToCallbackResponse.class);

			eventLog.log(getName(), args(
				"msg", "Authorization endpoint response captured",
				"http", "redirect-in",
				"url_query", env.getObject("callback_query_params"),
				"url_fragment", env.getObject("callback_params")));

			processCallback();

			return "done";
		});

		return redirectToLogDetailPage();

	}

}
