package net.openid.conformance.openid.client;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.as.EnsureMaxAgeEqualsZeroAndPromptNone;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.web.servlet.ModelAndView;

@PublishTestModule(
	testName = "oidcc-client-test-form-post-error",
	displayName = "OIDCC: Relying party test. Form post error handling test.",
	summary = "The client is expected to construct and send an Authentication Request " +
		" with response mode set to form_post, max_age=0 and prompt=none which results in the " +
		" test suite returning an error because the requested conditions cannot be met. " +
		" The client is expected to consume the HTML form post authorization error response, " +
		" and show an error screen to the user." +
		" Corresponds to rp-response_mode-form_post-error test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ResponseMode.class, values = "default")
public class OIDCCClientTestFormPostError extends AbstractOIDCCClientTest {

	@Override
	protected void endTestIfRequiredAuthorizationRequestParametersAreMissing() {
		callAndStopOnFailure(EnsureMaxAgeEqualsZeroAndPromptNone.class);
	}

	/**
	 * we override the response and always return an error response
	 * @return
	 */
	@Override
	protected Object generateFormPostResponse() {
		JsonObject originalResponseParams = env.getObject("authorization_endpoint_response_params");
		JsonObject errorResponseParams = new JsonObject();
		if(originalResponseParams.has("state")) {
			errorResponseParams.add("state", originalResponseParams.get("state"));
		}
		errorResponseParams.addProperty("error", "login_required");
		errorResponseParams.addProperty("error_description", "Error response using form_post");

		String formActionUrl = OIDFJSON.getString(originalResponseParams.remove("redirect_uri"));

		return new ModelAndView("formPostResponseMode",
			ImmutableMap.of(
				"formAction", formActionUrl,
				"formParameters", errorResponseParams
			));
	}

	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		boolean fireTestFinishedCalled = false;
		if(receivedAuthorizationRequest) {
			fireTestFinished();
			fireTestFinishedCalled = true;
		}
		return fireTestFinishedCalled;
	}
}
