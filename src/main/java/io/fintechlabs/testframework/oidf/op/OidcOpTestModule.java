package io.fintechlabs.testframework.oidf.op;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.common.CreateRandomImplicitSubmitUrl;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.HandleHttp;
import io.fintechlabs.testframework.testmodule.TestExecutionUnit;
import io.fintechlabs.testframework.testmodule.UserFacing;

/**
 * @author jricher
 *
 */
public abstract class OidcOpTestModule extends AbstractTestModule {

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#configure(com.google.gson.JsonObject, java.lang.String)
	 */
	@Override
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		call(createConfigurationSequence());
		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}


	/**
	 * The base URL of the test module is `base_url` and the configuration object
	 * is in `config`. Test will set status to CONFIGURED on successful completion
	 * and setupDone event is fired.
	 *
	 * @return
	 */
	protected abstract TestExecutionUnit createConfigurationSequence();


	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#start()
	 */
	@Override
	public void start() {
		setStatus(Status.RUNNING);

		call(createStartSequence());

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		if (!Strings.isNullOrEmpty(redirectTo)) {
			eventLog.log(getName(), args("msg", "Redirecting to url", "redirect_to", redirectTo));
			setStatus(Status.WAITING);
			browser.goToUrl(redirectTo);
		} else {
			fireTestSuccess();
			setStatus(Status.FINISHED);
		}
	}


	/**
	 * If `redirect_to_authorization_endpoint` string is in the environment after completion,
	 * test will redirect there and set status to WAITING. Otherwise just set the status to FINISHED.
	 */
	protected abstract TestExecutionUnit createStartSequence();

	/**
	 * Always try to capture the query fragment regardless of response type.
	 */
	@HandleHttp("callback")
	@UserFacing
	public Object handleCallback(HttpServletRequest request, HttpServletResponse response, HttpSession session, JsonObject requestParts){
		setStatus(Status.RUNNING);

		env.putObject("callback_query_params", requestParts.get("params").getAsJsonObject());

		call(condition(CreateRandomImplicitSubmitUrl.class));
		setStatus(Status.WAITING);

		return new ModelAndView("implicitCallback",
			args("implicitSubmitUrl", env.getString("implicit_submit", "fullUrl"),
				"returnUrl", "/log-detail.html?log=" + getId()));

	}

	@HandleHttp("implicit/**")
	public Object handleImplicitCallback(HttpServletRequest request, HttpServletResponse response, HttpSession session, JsonObject requestParts){
		getTestExecutionManager().runInBackground(() -> {
			setStatus(Status.RUNNING);

			JsonElement body = requestParts.get("body");

			if (body != null) {
				String hash = body.getAsString();

				//logger.info("Hash: " + hash);

				env.putString("implicit_hash", hash);
			} else {
				//logger.warn("No hash submitted");

				env.putString("implicit_hash", ""); // Clear any old value
			}

/*			// if we're on the first round:
			// mark that we're doing the 2nd call (somehow)
			if(!Boolean.TRUE.equals(env.getBoolean("second_auth_code"))) {
				call(sequence(ProcessAuthorizationEndpointResponse.class));

				call(sequence(ProcessTokenEndpointResponse.class));

				call(exec().startBlock("Second Auth Code"));
				env.putBoolean("second_auth_code", true);
				// call authz endpoint again
				sequence(CreateAuthorizationEndpointRequest.class);
				// continue...
				String redirectTo = env.getString("redirect_to_authorization_endpoint");
				eventLog.log(getName(), args("msg", "Redirecting to url", "redirect_to", redirectTo));
				setStatus(Status.WAITING);
				browser.goToUrl(redirectTo);
				// if we're on the 2nd round:
				// done
			} else {
				call(sequence(ProcessAuthorizationEndpointResponse.class)
					.replace(CallTokenEndpoint.class, condition(CallTokenEndpointExpectingError.class)));
				call(exec().endBlock());
				fireTestSuccess();

				setStatus(Status.FINISHED);
			}
			*/

			call(createCallbackSequence());

			// if we got here without an error being thrown it's a success

			fireTestSuccess();

			setStatus(Status.FINISHED);
			return "done";

		});
		return ResponseEntity.noContent().build();
	}


	/**
	 * Query parameters will be in `callback_query_params` object, implicit hash
	 * string will be in `implicit_hash`. Sets status to FINISHED on success and fires
	 * testSuccess event.
	 */
	protected abstract TestExecutionUnit createCallbackSequence();

}
