package net.openid.conformance.variant;

/**
 * Variant parameter for credential issuance mode in VCI tests.
 *
 * IMMEDIATE: Credentials are returned directly in the credential response.
 * DEFERRED: A transaction_id is returned, and credentials must be retrieved from the deferred credential endpoint.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-9">OID4VCI Section 9 - Deferred Credential Issuance</a>
 */
@VariantParameter(
	name = "vci_credential_issuance_mode",
	displayName = "Credential Issuance Mode",
	description = "Whether credentials are issued immediately or via deferred issuance. " +
		"'immediate' returns credentials directly. 'deferred' returns a transaction_id and the wallet must poll the deferred credential endpoint.",
	defaultValue = "immediate"
)
public enum VCICredentialIssuanceMode {

	IMMEDIATE,

	DEFERRED;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
