package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.ExpectIdTokenHintRequiredErrorPage;
import net.openid.conformance.condition.client.RemoveIdTokenHintFromEndSessionEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Corresponds to https://www.heenan.me.uk/~joseph/2020-06-05-test_desc_op.html#OP_RpInitLogout_No_id_token_hint
// https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-RpInitLogout-No-id_token_hint.json
@PublishTestModule(
	testName = "oidcc-rp-initiated-logout-no-id-token-hint",
	displayName = "OIDCC: rp initiated logout - no id_token_hint",
	summary = "This test performs a normal authorization flow at the OP, then sends the user to the end_session_endpoint with a post_logout_redirect_uri but no id_token_hint.\n\nThe OP must not redirect back and must either show an error screen or confirm with the user if they want to logout - a screenshot of which should be uploaded.",
	profile = "OIDCC"
)
public class OIDCCRpInitiatedLogoutNoIdTokenHint extends AbstractOIDCCRpInitiatedLogout {

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.equals("post_logout_redirect")) {
			throw new TestFailureException(getId(), "OP has incorrectly called the registered post_logout_redirect_uri even though no id_token_hint was provided.");
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	@Override
	protected void customiseEndSessionEndpointRequest() {
		callAndStopOnFailure(RemoveIdTokenHintFromEndSessionEndpointRequest.class);
	}

	@Override
	protected String createLogoutPlaceholder() {
		callAndStopOnFailure(ExpectIdTokenHintRequiredErrorPage.class, "OIDCRIL-2");

		return env.getString("id_token_hint_required_error");
	}

}
