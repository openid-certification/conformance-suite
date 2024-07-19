package net.openid.conformance.testmodule;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.ExtractImplicitHashToCallbackResponse;
import net.openid.conformance.condition.common.CreateRandomImplicitSubmitUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** The module is a general purpose module for collecting the result from the authorization endpoint
 *
 * TestModules using this class will receive the full result of the redirect in processCallback().
 */
public abstract class AbstractRedirectServerTestModule extends AbstractTestModule {
	private static final Logger logger = LoggerFactory.getLogger(AbstractRedirectServerTestModule.class);

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

	protected void redirect(String redirectTo) {
		browser.goToUrl(redirectTo);
	}

	protected void performRedirect() {
		performRedirect("GET");
	}

	protected void performRedirect(String method) {
		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), args("msg", "Redirecting to authorization endpoint",
			"redirect_to", redirectTo,
			"method", method,
			"http", "redirect"));

		setStatus(Status.WAITING);

		redirect(redirectTo);
	}

	protected final void performRedirectAndWaitForPlaceholdersOrCallback() {
		performRedirectAndWaitForPlaceholdersOrCallback("error_callback_placeholder");
	}

	protected final void performRedirectAndWaitForPlaceholdersOrCallback(String placeholderKey) {
		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), args("msg", "Redirecting to authorization endpoint",
			"redirect_to", redirectTo,
			"http", "redirect"));

		createPlaceholder();

		setStatus(Status.WAITING);

		waitForPlaceholders();

		browser.goToUrl(redirectTo, env.getString(placeholderKey));
	}

	// performs the redirect with a placeholder to fill in, but does NOT start 'waitForPlaceholders()' so
	// this is used when the test will continue running after the redirect
	protected final void performRedirectWithPlaceholder() {
		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), args("msg", "Redirecting to authorization endpoint",
			"redirect_to", redirectTo,
			"http", "redirect"));

		createPlaceholder();

		setStatus(Status.WAITING);

		browser.goToUrl(redirectTo, env.getString("error_callback_placeholder"));
	}

	protected void createPlaceholder() {
		// Use for create new placeholder in subclass
		throw new TestFailureException(getId(), "Placeholder must be created for test " + getName());
	}

	@UserFacing
	private Object handleCallback(JsonObject requestParts) {

		setStatus(Status.RUNNING);

		final JsonElement body_form_params = requestParts.get("body_form_params");
		if (body_form_params != null) {
			env.putObject("callback_body_form_params", body_form_params.getAsJsonObject());
		}
		env.putObject("callback_query_params", requestParts.get("query_string_params").getAsJsonObject());
		env.putObject("callback_headers", requestParts.get("headers").getAsJsonObject());
		env.putString("callback_http_method", OIDFJSON.getString(requestParts.get("method")));

		callAndStopOnFailure(CreateRandomImplicitSubmitUrl.class);

		setStatus(Status.WAITING);

		String submissionUrl = env.getString("implicit_submit", "fullUrl");
		logger.info(getId() + ": Sending JS to user's browser to submit URL fragment (hash) to " + submissionUrl);

		return new ModelAndView("implicitCallback",
			ImmutableMap.of(
				"implicitSubmitUrl", env.getString("implicit_submit", "fullUrl"),
				"returnUrl", "/log-detail.html?log=" + getId()
			));
	}

	/**
	 * Called after the redirect response has been fully received
	 *
	 * These will be available in the environment:
	 *
	 * 'callback_params': fragment passed to redirect uri
	 * 'callback_query_params': url query passed to redirect uri
	 * 'callback_http_method': http method used at redirect uri (usually GET or POST)
	 * 'callback_body_form_params': any form encoded body passed to redirect uri
	 */
	protected abstract void processCallback();

	private Object handleImplicitSubmission(JsonObject requestParts) {

		getTestExecutionManager().runInBackground(() -> {

			// process the callback
			setStatus(Status.RUNNING);

			JsonElement body = requestParts.get("body");

			if (body != null) {
				String hash = OIDFJSON.getString(body);

				logger.info(getId() + ": URL fragment (hash): " + hash);

				env.putString("implicit_hash", hash);
			} else {
				logger.warn(getId()+": No hash/URL fragment submitted");

				env.putString("implicit_hash", ""); // Clear any old value
			}

			callAndStopOnFailure(ExtractImplicitHashToCallbackResponse.class);

			eventLog.log(getName(), args(
				"msg", "Authorization endpoint response captured",
				"http", "redirect-in",
				"http_method", env.getString("callback_http_method"),
				"url_query", env.getObject("callback_query_params"),
				"url_fragment", env.getObject("callback_params"),
				"headers", env.getObject("callback_headers"),
				"post_body", env.getObject("callback_body_form_params")));

			processCallback();

			return "done";
		});

		return new ResponseEntity<Object>("", HttpStatus.NO_CONTENT);
	}

}
