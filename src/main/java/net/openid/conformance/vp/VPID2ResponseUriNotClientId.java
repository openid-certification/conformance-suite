package net.openid.conformance.vp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AddBadResponseUriToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddResponseUriToAuthorizationEndpointRequest;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@PublishTestModule(
	testName = "oid4vp-happy-flow-response-uri-not-client-id",
	displayName = "OID4VP: Unsigned request_uri",
	summary = "Makes a request where the response_uri is not the client_id. The wallet should display an error, a screenshot of which should be uploaded.",
	profile = "OID4VP-ID2",
	configurationFields = {
		"client.presentation_definition"
	}
)

public class VPID2ResponseUriNotClientId extends AbstractVPServerTest {

	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence createAuthorizationRequestSteps = super.createAuthorizationRequestSequence();

		createAuthorizationRequestSteps = createAuthorizationRequestSteps.
			replace(AddResponseUriToAuthorizationEndpointRequest.class, condition(AddBadResponseUriToAuthorizationEndpointRequest.class));

		return createAuthorizationRequestSteps;
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "OID4VP-6.2");

		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected Object handleRequestUriRequest() {
		Object o = super.handleRequestUriRequest();
		createPlaceholder();
		waitForPlaceholders();
		return o;
	}

	@Override
	protected Object handleDirectPost(String requestId) {
		throw new TestFailureException(getId(), "Direct post (response_uri) endpoint has been called but was not in the request");
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		return super.handleHttp(path, req, res, session, requestParts);
	}
}
