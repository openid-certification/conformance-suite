package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddNonExistentClaimToDcqlQuery;
import net.openid.conformance.condition.client.EnsureAuthorizationEndpointErrorIsAccessDenied;
import net.openid.conformance.condition.client.EnsureErrorResponseForUnsatisfiableDcqlQuery;
import net.openid.conformance.condition.client.EnsureNoVpTokenInAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.ExtractDCQLQueryFromClientConfiguration;
import net.openid.conformance.condition.common.ExpectUnsatisfiableDcqlQueryErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-claim-not-in-credential",
	displayName = "OID4VP-1.0-FINAL: DCQL query for a claim the credential does not contain",
	summary = """
		Sends the normal DCQL query with one additional claim that the credential cannot contain. \
		Without claim_sets all requested claims are required, and per OID4VP section 6.4.1 a wallet that \
		cannot deliver all requested claims MUST NOT return the credential; as it is the only credential \
		requested, section 6.4.2 means no credentials may be returned. The wallet must therefore not \
		return a vp_token — neither one containing the credential with the remaining claims, nor an empty \
		vp_token object. The wallet should return an 'access_denied' error response, or reject the request \
		and display an error, a screenshot of which must be uploaded. \
		The DCQL configuration must request exactly one credential, with at least one claim, and must not \
		contain credential_sets or claim_sets.""",
	profile = "OID4VP-1FINAL"
)
public class VP1FinalWalletClaimNotInCredential extends AbstractVP1FinalWalletNegativeTestExpectingError {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence steps = super.createAuthorizationRequestSequence();

		steps = steps.insertAfter(ExtractDCQLQueryFromClientConfiguration.class,
			condition(AddNonExistentClaimToDcqlQuery.class)
				.requirements("OID4VP-1FINAL-6.4.1", "OID4VP-1FINAL-6.4.2"));

		return steps;
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectUnsatisfiableDcqlQueryErrorPage.class, "OID4VP-1FINAL-6.4.1", "OID4VP-1FINAL-6.4.2", "OID4VP-1FINAL-8.5");
		env.putString("error_callback_placeholder", env.getString("unsatisfiable_dcql_query_error"));
	}

	@Override
	protected void continueAfterRequestUriCalled() {
		eventLog.log(getName(),
			"Wallet has retrieved request_uri - the DCQL query requests a claim the credential does not contain, "
				+ "so the wallet should return an 'access_denied' error response or display an error.");
		createPlaceholder();
		waitForPlaceholders();
	}

	@Override
	protected void validateErrorResponse() {
		callAndContinueOnFailure(EnsureErrorResponseForUnsatisfiableDcqlQuery.class, ConditionResult.FAILURE, "OID4VP-1FINAL-6.4.1", "OID4VP-1FINAL-6.4.2", "OID4VP-1FINAL-8.5");
		callAndContinueOnFailure(EnsureNoVpTokenInAuthorizationEndpointResponse.class, ConditionResult.FAILURE, "OID4VP-1FINAL-6.4.1", "OID4VP-1FINAL-6.4.2");
		// OID4VP 8.5 defines access_denied for "the Wallet did not have the requested Credentials", which is
		// the closest defined code; another error code is not a violation, so only warn
		callAndContinueOnFailure(EnsureAuthorizationEndpointErrorIsAccessDenied.class, ConditionResult.WARNING, "OID4VP-1FINAL-8.5");
	}
}
