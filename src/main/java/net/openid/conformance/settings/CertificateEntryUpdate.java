package net.openid.conformance.settings;

/**
 * A certificate entry as the settings page sends it.
 *
 * @param id null for a new entry
 * @param privateKeyPem null to keep the stored key of the entry with this {@code id}
 */
public record CertificateEntryUpdate(String id, String label, String certificateChainPem, String privateKeyPem) {

	@Override
	public String toString() {
		// the generated toString would print the private key
		return "CertificateEntryUpdate[id=" + id + ", label=" + label + "]";
	}
}
