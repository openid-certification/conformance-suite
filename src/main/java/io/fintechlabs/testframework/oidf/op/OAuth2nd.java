package io.fintechlabs.testframework.oidf.op;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;
import io.fintechlabs.testframework.condition.common.CreateRandomImplicitSubmitUrl;
import io.fintechlabs.testframework.sequence.ConditionSequence;
import io.fintechlabs.testframework.sequence.client.CreateAuthorizationEndpointRequest;
import io.fintechlabs.testframework.sequence.client.LoadServerAndClientConfiguration;
import io.fintechlabs.testframework.sequence.client.ProcessAuthorizationEndpointResponse;
import io.fintechlabs.testframework.sequence.client.ProcessTokenEndpointResponse;
import io.fintechlabs.testframework.testmodule.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author jricher
 *
 */
@PublishTestModule(testName = "sequenceTest",
	displayName = "Sequence Test",
	configurationFields = {		"server.discoveryUrl",
	"client.client_id",
	"client.scope",
	"client.client_secret"})
public class OAuth2nd extends AbstractTestModule {

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

	protected ConditionSequence createConfigurationSequence() {
		return sequence(LoadServerAndClientConfiguration.class);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#start()
	 */
	@Override
	public void start() {
		setStatus(Status.RUNNING);

		call(createStartSequence());

		String redirectTo = env.getString("redirect_to_authorization_endpoint");
		eventLog.log(getName(), args("msg", "Redirecting to url", "redirect_to", redirectTo));
		setStatus(Status.WAITING);
		browser.goToUrl(redirectTo);

	}

	protected ConditionSequence createStartSequence() {
		return sequence(CreateAuthorizationEndpointRequest.class);
	}

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

			call(processAuthorizationEndpointResponse());

			call(createTokenEndpointResponseSequence());

			call(processAuthorizationEndpointResponse()
				.replace(CallTokenEndpoint.class, condition(CallTokenEndpointExpectingError.class)));

			fireTestSuccess();

			setStatus(Status.FINISHED);
			return "done";

		});
		return ResponseEntity.noContent().build();
	}

	protected ConditionSequence createTokenEndpointResponseSequence() {
		return sequence(ProcessTokenEndpointResponse.class);
	}

	protected ConditionSequence processAuthorizationEndpointResponse() {
		return sequence(ProcessAuthorizationEndpointResponse.class);
	}

}
