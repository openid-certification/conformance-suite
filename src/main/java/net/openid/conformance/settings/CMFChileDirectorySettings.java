package net.openid.conformance.settings;

import com.nimbusds.jose.jwk.JWKSet;

import java.util.List;

/**
 * The Chile CMF Directorio section as test modules see it, already parsed. Instances are
 * immutable and shared between concurrently running tests.
 *
 * <p>The client secret, the private JWKS members and the certificates' private keys belong to the
 * suite operator: a module must not put them in the test environment, pass them to
 * {@code log(...)}/{@code args(...)}, or otherwise let them reach the test log.
 *
 * @param directoryTokenEndpoint null when not configured
 * @param clientId null when not configured
 * @param clientSecret null when not configured
 * @param clientJwks with private keys; null when not configured
 */
public record CMFChileDirectorySettings(
	String directoryTokenEndpoint,
	String clientId,
	String clientSecret,
	JWKSet clientJwks,
	List<ClientCertificate> positiveCertificates,
	List<ClientCertificate> negativeCertificates) {

	public CMFChileDirectorySettings {
		positiveCertificates = List.copyOf(positiveCertificates);
		negativeCertificates = List.copyOf(negativeCertificates);
	}

	@Override
	public String toString() {
		// the generated toString would print the client secret and the private JWKS
		return "CMFChileDirectorySettings[directoryTokenEndpoint=" + directoryTokenEndpoint
			+ ", clientId=" + clientId
			+ ", positiveCertificates=" + positiveCertificates
			+ ", negativeCertificates=" + negativeCertificates + "]";
	}
}
