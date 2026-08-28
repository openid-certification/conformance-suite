package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddUnknownTypeTransactionDataToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidTransactionDataError;
import net.openid.conformance.condition.client.EnsureNoVpTokenInAuthorizationEndpointResponse;
import net.openid.conformance.condition.common.ExpectUnknownTransactionDataTypeErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-unknown-transaction-data-type",
	displayName = "OID4VP-1.0-FINAL: Authorization request with unknown transaction_data type",
	summary = """
		Sends an authorization request whose transaction_data parameter contains an entry with a 'type' value the \
		wallet cannot recognize. Per OID4VP 1.0 §5.1 and §8.4 (as clarified by \
		https://github.com/openid/OpenID4VP/pull/790) the wallet MUST reject the request and MUST NOT return a \
		vp_token; it should either return an 'invalid_transaction_data' error response, or abort without responding \
		and display an error, a screenshot of which must be uploaded.""",
	profile = "OID4VP-1FINAL"
)
public class VP1FinalWalletUnknownTransactionDataType extends AbstractVP1FinalWalletNegativeTestExpectingError {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence steps = super.createAuthorizationRequestSequence();

		steps = steps.then(condition(AddUnknownTypeTransactionDataToAuthorizationEndpointRequest.class)
			.requirements("OID4VP-1FINAL-5.1", "OID4VP-1FINAL-8.4"));

		return steps;
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectUnknownTransactionDataTypeErrorPage.class, "OID4VP-1FINAL-5.1", "OID4VP-1FINAL-8.4");
		env.putString("error_callback_placeholder", env.getString("unknown_transaction_data_type_error"));
	}

	@Override
	protected void continueAfterRequestUriCalled() {
		eventLog.log(getName(),
			"Wallet has retrieved request_uri - the request contains an unknown transaction_data type, so the wallet "
				+ "should return an 'invalid_transaction_data' error response or display an error.");
		createPlaceholder();
		waitForPlaceholders();
	}

	@Override
	protected void validateErrorResponse() {
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.1", "OID4VP-1FINAL-8.4");
		callAndContinueOnFailure(EnsureNoVpTokenInAuthorizationEndpointResponse.class, ConditionResult.FAILURE, "OID4VP-1FINAL-8.4");
		callAndContinueOnFailure(EnsureInvalidTransactionDataError.class, ConditionResult.FAILURE, "OID4VP-1FINAL-8.4");
	}
}
