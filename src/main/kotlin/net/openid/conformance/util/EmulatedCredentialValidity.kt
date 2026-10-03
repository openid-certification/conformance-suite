package net.openid.conformance.util

import java.time.Duration

/**
 * How long the credentials the suite mints - SD-JWT VCs and mdoc MSOs, presented by the mock
 * wallet or issued by the emulated issuer - are valid for, which depends on whether they can be
 * revoked.
 *
 * A credential that references a status list or identifier list is long-lived, as the list is
 * what lets it be revoked before it expires; each format picks its own period. One that carries
 * no revocation information is valid for [WITHOUT_REVOCATION_INFORMATION], whatever its format.
 */
object EmulatedCredentialValidity {

	/**
	 * Inside the "24 hours or less" for which CIR (EU) 2024/2979 (EAA-6.2.10.1-02.1) exempts an
	 * attestation from the revocation requirement, so a verifier applying the regulation can
	 * accept the credential. Measured from the start of the credential's validity period.
	 */
	@JvmField
	val WITHOUT_REVOCATION_INFORMATION: Duration = Duration.ofHours(23)
}
