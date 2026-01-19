package net.openid.conformance.variant;

/**
 * Variant parameter for credential response encryption in VCI tests.
 *
 * PLAIN: Credentials are returned unencrypted (default).
 * ENCRYPTED: Credentials are returned encrypted as JWE per OID4VCI Section 11.2.3.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-11.2.3">OID4VCI Section 11.2.3 - Credential Response Encryption</a>
 */
@VariantParameter(
	name = "vci_credential_encryption",
	displayName = "Credential Response Encryption",
	description = "Whether credential responses should be encrypted. " +
		"'plain' returns credentials unencrypted. 'encrypted' returns credentials as JWE.",
	defaultValue = "plain"
)
public enum VCICredentialEncryption {

	PLAIN,

	ENCRYPTED;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
