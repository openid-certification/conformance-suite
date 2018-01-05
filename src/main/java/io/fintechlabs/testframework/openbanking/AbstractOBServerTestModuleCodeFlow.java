package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.UserFacing;

public abstract class AbstractOBServerTestModuleCodeFlow extends AbstractOBServerTestModule {

	public AbstractOBServerTestModuleCodeFlow(String name, String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(name, id, owner, eventLog, browser, testInfo);
	}

	@Override
	protected void createAuthorizationRequest() {

		super.createAuthorizationRequest();

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		eventLog.log(getName(), "Path: " + path);
		eventLog.log(getName(), "Params: " + requestParts);

		// dispatch based on the path
		if (path.equals("callback")) {
			return handleCallback(requestParts);
		} else {
			return new ModelAndView("testError");
		}
	}

	@UserFacing
	private ModelAndView handleCallback(JsonObject requestParts) {

		setStatus(Status.RUNNING);

		env.put("callback_params", requestParts.get("params").getAsJsonObject());

		onAuthorizationCallbackResponse();

		setStatus(Status.FINISHED);
		fireTestSuccess();
		return new ModelAndView("complete", ImmutableMap.of("test", this));
	}

}
