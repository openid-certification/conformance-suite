package net.openid.conformance.vp1finalwallet;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.DecryptResponse;
import net.openid.conformance.condition.client.ExtractAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.ExtractBrowserApiAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.ExtractVP1FinalBrowserApiResponse;
import net.openid.conformance.condition.client.ValidateAuthResponseContainsOnlyResponse;
import net.openid.conformance.testmodule.TestFailureException;

/**
 * Base class for negative wallet tests where the authorization request is authentic (validly
 * signed, correct client_id) so the wallet has a trusted response endpoint. Per
 * https://github.com/openid/OpenID4VP/pull/790 such a wallet may either return an error response
 * (to the response_uri, or as a fulfilled Digital Credentials API response whose data contains an
 * 'error' member) or abort without responding — unlike the 'terminate request processing' cases
 * (e.g. invalid request object signature) where no response must be sent and modules instead fail
 * on any call to the response endpoint.
 *
 * Subclasses implement validateErrorResponse() with the conditions (and severities) that validate
 * the error response, and retain the screenshot placeholder flow as the alternative pass for
 * wallets that abort without responding.
 */
public abstract class AbstractVP1FinalWalletNegativeTestExpectingError extends AbstractVP1FinalWalletTest {

	// The wallet must return an error response instead of a vp_token, so the success-path
	// response processing is replaced with error response validation.
	@Override
	protected final void processReceivedResponse() {
		boolean encrypted = env.getElementFromObject("original_authorization_endpoint_response", "response") != null;
		switch (responseMode) {
			case DIRECT_POST:
				callAndStopOnFailure(ExtractAuthorizationEndpointResponse.class, ConditionResult.FAILURE);
				break;
			case DC_API:
				callAndStopOnFailure(ExtractBrowserApiAuthorizationEndpointResponse.class, ConditionResult.FAILURE);
				break;
			case DIRECT_POST_JWT:
				if (encrypted) {
					processEncryptedErrorResponse();
				} else {
					logUnencryptedErrorResponse();
					callAndStopOnFailure(ExtractAuthorizationEndpointResponse.class, ConditionResult.FAILURE);
				}
				break;
			case DC_API_JWT:
				if (encrypted) {
					processEncryptedErrorResponse();
				} else {
					logUnencryptedErrorResponse();
					callAndStopOnFailure(ExtractBrowserApiAuthorizationEndpointResponse.class, ConditionResult.FAILURE);
				}
				break;
		}

		validateErrorResponse();
	}

	/**
	 * Validate the error response the wallet returned, which has been extracted into
	 * 'authorization_endpoint_response'. Implementations supply the conditions, severities and
	 * requirement references appropriate to the specific negative test.
	 */
	protected abstract void validateErrorResponse();

	private void processEncryptedErrorResponse() {
		callAndContinueOnFailure(ValidateAuthResponseContainsOnlyResponse.class, ConditionResult.FAILURE, "OID4VP-1FINAL-8.3");
		callAndStopOnFailure(DecryptResponse.class, "OID4VP-1FINAL-8.3");
	}

	private void logUnencryptedErrorResponse() {
		eventLog.log(getName(),
			"The response is not encrypted; OID4VP permits a wallet that is unable to generate an encrypted "
				+ "response to return an unencrypted error response.");
	}

	@Override
	protected void populateDirectPostResponseWithRedirectUri() {
		// the wallet returned an error response, so there is no presentation flow to continue
		// via a redirect_uri, even for ISO mDL
		populateDirectPostResponse();
	}

	@Override
	protected void processBrowserApiResponse() {
		JsonObject result = parseBrowserApiResponseBody();

		if (result.has("exception")) {
			browserApiRejectionReceived(result.get("exception"));
			return;
		}
		if (result.has("bad_response_type")) {
			throw new TestFailureException(getId(),
				"Browser API returned an object of unknown type: " + result.get("bad_response_type"));
		}

		// A fulfilled response: per "OpenID4VP over the Digital Credentials API" a protocol error
		// is returned as a data object containing an 'error' member, which processReceivedResponse
		// validates. A successful vp_token response fails validation there.
		callAndStopOnFailure(ExtractVP1FinalBrowserApiResponse.class);

		processReceivedResponse();

		fireTestFinished();
	}
}
