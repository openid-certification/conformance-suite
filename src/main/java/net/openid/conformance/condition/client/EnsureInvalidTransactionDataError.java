package net.openid.conformance.condition.client;

/**
 * Checks the authorization response uses the 'invalid_transaction_data' error code, defined in
 * OID4VP 1.0 section 8.4 for (amongst others) a transaction_data entry with an unknown or
 * unsupported type. https://github.com/openid/OpenID4VP/pull/790 clarifies that any response a
 * wallet returns for an unsupported transaction_data request must use this error code.
 */
public class EnsureInvalidTransactionDataError extends AbstractEnsureSpecifiedErrorFromAuthorizationEndpointResponse {

	@Override
	protected String getExpectedError() {
		return "invalid_transaction_data";
	}
}
