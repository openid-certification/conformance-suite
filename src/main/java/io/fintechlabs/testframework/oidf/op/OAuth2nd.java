package io.fintechlabs.testframework.oidf.op;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.sequence.ConditionSequence;
import io.fintechlabs.testframework.sequence.client.CreateAuthorizationEndpointRequest;
import io.fintechlabs.testframework.sequence.client.LoadServerAndClientConfiguration;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.ConditionCallBuilder;
import io.fintechlabs.testframework.testmodule.HandleHttp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author jricher
 *
 */
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

	private ConditionSequence createConfigurationSequence() {
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

	private ConditionSequence createStartSequence() {
		return sequence(CreateAuthorizationEndpointRequest.class);
	}

	@HandleHttp("callback")
	public Object handleCallback(HttpServletRequest request, HttpServletResponse response, HttpSession session, JsonObject requestParts){


		return null;
	}

}
