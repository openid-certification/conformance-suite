package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.CreateWalletNonce;
import net.openid.conformance.condition.as.EnsureRequestUriHasNoFragment;
import net.openid.conformance.condition.as.EnsureRequestUriIsHttps;
import net.openid.conformance.condition.as.EnsureWalletNonceClaimMatchesPostedValue;
import net.openid.conformance.condition.as.PostToRequestUriAndExtractRequestObject;
import net.openid.conformance.condition.client.EnsureContentTypeApplicationOauthAuthzReqJwt;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-request-uri-method-post",
	displayName = "OID4VP-1.0-FINAL Verifier: request_uri_method=post",
	summary = """
		Tests that the verifier correctly handles a wallet that uses HTTP POST to fetch the request_uri \
		as specified in OID4VP section 5.10. If the verifier's authorization request does not include \
		request_uri_method=post, this test will be skipped.""",
	profile = "OID4VP-1FINAL",
	configurationFields = {
		"credential.signing_jwk"
	}
)
public class VP1FinalVerifierRequestUriMethodPost extends AbstractVP1FinalVerifierTest {

	@Override
	protected void fetchAndProcessRequestUri() {
		String requestUriMethod = env.getString("authorization_endpoint_http_request_params", "request_uri_method");

		if (!"post".equals(requestUriMethod)) {
			fireTestSkipped("The verifier's authorization request does not include request_uri_method=post, so this test is not applicable.");
			return;
		}

		callAndStopOnFailure(CreateWalletNonce.class, "OID4VP-1FINAL-5.10");
		callAndStopOnFailure(PostToRequestUriAndExtractRequestObject.class, "OID4VP-1FINAL-5.10");

		validateRequestUriPostResponse();

		callAndContinueOnFailure(EnsureWalletNonceClaimMatchesPostedValue.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.10");
		callAndContinueOnFailure(EnsureRequestUriIsHttps.class, ConditionResult.FAILURE, "JAR-5.2");
		callAndContinueOnFailure(EnsureRequestUriHasNoFragment.class, ConditionResult.FAILURE);
	}

	private void validateRequestUriPostResponse() {
		call(exec().mapKey("endpoint_response", "request_uri_post_response"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.10");
		callAndContinueOnFailure(EnsureContentTypeApplicationOauthAuthzReqJwt.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.10");
		call(exec().unmapKey("endpoint_response"));
	}

}
