package net.openid.conformance.settings;

/**
 * A stored client certificate: its chain and private key as the admin pasted them.
 *
 * @param id server-assigned, stable across saves so an unchanged private key need not be re-sent
 */
public record CertificateEntry(String id, String label, String certificateChainPem, String privateKeyPem) {

	@Override
	public String toString() {
		// the generated toString would print the private key
		return "CertificateEntry[id=" + id + ", label=" + label + "]";
	}
}
