package net.openid.conformance.vp;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.client.AddBadResponseUriToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddResponseUriToAuthorizationEndpointRequest;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "oid4vp-happy-flow-response-uri-not-client-id",
	displayName = "OID4VP: Unsigned request_uri",
	summary = "Makes a request where the response_uri is not the client_id. The wallet should display an error, a screenshot of which must be uploaded.",
	profile = "OID4VP-ID2",
	configurationFields = {
		"client.presentation_definition"
	}
)

public class VPID2ResponseUriNotClientId extends AbstractVPServerTest {
// FIXME for x509 dns the client id we try needs to be on a different hostname; but even this is permitted by the specs in some cases:
// "If the Wallet can establish trust in the Client Identifier authenticated through the certificate, e.g. because the Client Identifier is contained in a list of trusted Client Identifiers, it may allow the client to freely choose the redirect_uri value."
	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence createAuthorizationRequestSteps = super.createAuthorizationRequestSequence();

		createAuthorizationRequestSteps = createAuthorizationRequestSteps.
			replace(AddResponseUriToAuthorizationEndpointRequest.class, condition(AddBadResponseUriToAuthorizationEndpointRequest.class));

		return createAuthorizationRequestSteps;
	}

	@Override
	protected void createPlaceholder() {
		// FIXME use a better placeholder with a better message
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "OID4VP-6.2");

		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected Object handleRequestUriRequest() {
		Object o = super.handleRequestUriRequest();
		setStatus(Status.RUNNING);
		createPlaceholder();
		waitForPlaceholders();
		setStatus(Status.WAITING);
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
