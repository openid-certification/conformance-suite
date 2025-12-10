package net.openid.conformance.vci10issuer.condition;

import java.util.List;
import java.util.stream.Stream;

public enum VciErrorCode {

	/**
	 * The Credential Request is missing a required parameter, includes an unsupported parameter or parameter value, repeats the same parameter, or is otherwise malformed.
	 */
	INVALID_CREDENTIAL_REQUEST("invalid_credential_request"),

	/**
	 * Requested Credential Configuration is unknown.
 	 */
	INVALID_CREDENTIAL_CONFIGURATION("invalid_credential_configuration"),

	/**
	 * Requested Credential identifier is unknown.
 	 */
	INVALID_CREDENTIAL_IDENTIFIER("invalid_credential_identifier"),

	/**
	 * The proofs parameter in the Credential Request is invalid: (1) if the field is missing, or (2) one of the provided key proofs is invalid, or (3) if at least one of the key proofs does not contain a c_nonce value (refer to Section 7.2).
	 */
	INVALID_PROOF("invalid_proof"),

	/**
	 * The proofs parameter in the Credential Request uses an invalid nonce: at least one of the key proofs contains an invalid c_nonce value. The wallet should retrieve a new c_nonce value (refer to Section 7).
 	 */
	INVALID_NONCE("invalid_nonce"),

	/**
	 * This error occurs when the encryption parameters in the Credential Request are either invalid or missing. In the latter case, it indicates that the Credential Issuer requires the Credential Response to be sent encrypted, but the Credential Request does not contain the necessary encryption parameters.
 	 */
	INVALID_ENCRYPTION_PARAMETERS("invalid_encryption_parameters"),

	/**
	 * The Credential Request has not been accepted by the Credential Issuer. The Wallet SHOULD treat this error as unrecoverable, meaning if received from a Credential Issuer the Credential cannot be issued.
 	 */
	CREDENTIAL_REQUEST_DENIED("credential_request_denied");

	private final String errorCode;

	VciErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public static List<String> errorCodes() {
		return Stream.of(VciErrorCode.values()).map(VciErrorCode::getErrorCode).toList();
	}
}
