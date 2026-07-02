package net.openid.conformance.vp1finalwallet;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddRequiredNonMatchingCredentialToDcqlQuery;
import net.openid.conformance.condition.client.DecryptResponse;
import net.openid.conformance.condition.client.EnsureAuthorizationEndpointErrorIsAccessDenied;
import net.openid.conformance.condition.client.EnsureErrorResponseForUnsatisfiableDcqlQuery;
import net.openid.conformance.condition.client.EnsureNoVpTokenInAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.ExtractAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.ExtractBrowserApiAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.ExtractDCQLQueryFromClientConfiguration;
import net.openid.conformance.condition.client.ExtractVP1FinalBrowserApiResponse;
import net.openid.conformance.condition.client.ValidateAuthResponseContainsOnlyResponse;
import net.openid.conformance.condition.common.ExpectUnsatisfiableDcqlQueryErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-required-non-matching-credential",
	displayName = "OID4VP-1.0-FINAL: DCQL query with required non-matching credential_set",
	summary = """
		Sends a DCQL query with two credential entries wrapped in credential_sets: the real credential \
		and a second non-matching credential, both required. The wallet cannot satisfy the whole query, \
		so it must not return a vp_token — in particular it must not return an empty vp_token object, nor \
		a partial vp_token containing only the real credential. The wallet should return an 'access_denied' \
		error response, or reject the request and display an error, a screenshot of which must be uploaded. \
		The DCQL configuration must not already contain credential_sets.""",
	profile = "OID4VP-1FINAL"
)
public class VP1FinalWalletRequiredNonMatchingCredential extends AbstractVP1FinalWalletTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence steps = super.createAuthorizationRequestSequence();

		steps = steps.insertAfter(ExtractDCQLQueryFromClientConfiguration.class,
			condition(AddRequiredNonMatchingCredentialToDcqlQuery.class)
				.requirements("OID4VP-1FINAL-6.2", "OID4VP-1FINAL-6.4.2"));

		return steps;
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectUnsatisfiableDcqlQueryErrorPage.class, "OID4VP-1FINAL-6.4.2", "OID4VP-1FINAL-8.5");
		env.putString("error_callback_placeholder", env.getString("unsatisfiable_dcql_query_error"));
	}

	@Override
	protected void continueAfterRequestUriCalled() {
		eventLog.log(getName(),
			"Wallet has retrieved request_uri - the DCQL query contains a required credential the wallet cannot "
				+ "satisfy, so the wallet should return an 'access_denied' error response or display an error.");
		createPlaceholder();
		waitForPlaceholders();
	}

	// The wallet must return an error response instead of a vp_token, so the success-path
	// response processing is replaced with error response validation.
	@Override
	protected void processReceivedResponse() {
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

		callAndContinueOnFailure(EnsureErrorResponseForUnsatisfiableDcqlQuery.class, ConditionResult.FAILURE, "OID4VP-1FINAL-6.4.2", "OID4VP-1FINAL-8.5");
		callAndContinueOnFailure(EnsureNoVpTokenInAuthorizationEndpointResponse.class, ConditionResult.FAILURE, "OID4VP-1FINAL-6.4.2");
		callAndContinueOnFailure(EnsureAuthorizationEndpointErrorIsAccessDenied.class, ConditionResult.WARNING, "OID4VP-1FINAL-8.5");
	}

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
