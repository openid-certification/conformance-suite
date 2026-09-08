package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddRequiredNonMatchingCredentialToDcqlQuery;
import net.openid.conformance.condition.client.EnsureAuthorizationEndpointErrorIsAccessDenied;
import net.openid.conformance.condition.client.EnsureErrorResponseForUnsatisfiableDcqlQuery;
import net.openid.conformance.condition.client.EnsureNoVpTokenInAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.ExtractDCQLQueryFromClientConfiguration;
import net.openid.conformance.condition.common.ExpectUnsatisfiableDcqlQueryErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

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
public class VP1FinalWalletRequiredNonMatchingCredential extends AbstractVP1FinalWalletNegativeTestExpectingError {

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

	@Override
	protected void validateErrorResponse() {
		callAndContinueOnFailure(EnsureErrorResponseForUnsatisfiableDcqlQuery.class, ConditionResult.FAILURE, "OID4VP-1FINAL-6.4.2", "OID4VP-1FINAL-8.5");
		callAndContinueOnFailure(EnsureNoVpTokenInAuthorizationEndpointResponse.class, ConditionResult.FAILURE, "OID4VP-1FINAL-6.4.2");
		callAndContinueOnFailure(EnsureAuthorizationEndpointErrorIsAccessDenied.class, ConditionResult.WARNING, "OID4VP-1FINAL-8.5");
	}
}
