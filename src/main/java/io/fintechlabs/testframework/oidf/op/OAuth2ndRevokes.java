package io.fintechlabs.testframework.oidf.op;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.client.*;
import io.fintechlabs.testframework.sequence.ConditionSequence;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.HandleHttp;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author jricher
 *
 */
@PublishTestModule(testName = "OAuth2ndRevokes",
	displayName = "OAuth use access token to hit userinfo twice",
	configurationFields = {		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.client_secret"})
public class OAuth2ndRevokes extends OAuth2nd {

	@Override
	protected ConditionSequence createConfigurationSequence() {
		return super.createConfigurationSequence()
			.then(condition(ExtractUserInfoEndpointAsResource.class));
	}

	@Override
	protected void implicitCallbackSequences() {
		call(processAuthorizationEndpointResponse());

		call(createTokenEndpointResponseSequence());

		call(exec().mapKey("resource", "userinfo_resource"));
		call(condition(CallProtectedResourceWithBearerToken.class));

		call(processAuthorizationEndpointResponse()
			.replace(CallTokenEndpoint.class, condition(CallTokenEndpointExpectingError.class)));

		call(condition(CallProtectedResourceWithBearerToken.class));
	}

}
