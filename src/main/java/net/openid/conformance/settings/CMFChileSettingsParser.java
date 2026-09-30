package net.openid.conformance.settings;

import com.nimbusds.jose.jwk.JWKSet;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns the stored section into the parsed form test modules use. Stored data passed
 * {@link CMFChileSettingsValidator} when it was saved, so anything that fails to parse here means
 * the stored document is corrupt.
 */
public final class CMFChileSettingsParser {

	private CMFChileSettingsParser() {
	}

	/**
	 * @throws IllegalStateException naming the field, if the stored section is corrupt
	 */
	public static CMFChileDirectorySettings parse(CMFChileSettings settings) {
		return new CMFChileDirectorySettings(
			settings.directoryTokenEndpoint(),
			settings.clientId(),
			settings.clientSecret(),
			parseJwks(settings.clientJwks()),
			parseCertificates(CMFChileSettings.POSITIVE_CERTIFICATES, settings.positiveCertificates()),
			parseCertificates(CMFChileSettings.NEGATIVE_CERTIFICATES, settings.negativeCertificates()));
	}

	private static JWKSet parseJwks(String jwks) {
		if (jwks == null) {
			return null;
		}
		try {
			return JWKSet.parse(jwks);
		} catch (ParseException e) {
			throw corrupt(CMFChileSettings.CLIENT_JWKS, e);
		}
	}

	private static List<ClientCertificate> parseCertificates(String listField, List<CertificateEntry> entries) {
		List<ClientCertificate> certificates = new ArrayList<>();
		for (int i = 0; i < entries.size(); i++) {
			CertificateEntry entry = entries.get(i);
			try {
				certificates.add(new ClientCertificate(entry.label(),
					PemParsing.parseCertificateChain(entry.certificateChainPem()),
					PemParsing.parsePrivateKey(entry.privateKeyPem())));
			} catch (IllegalArgumentException e) {
				throw corrupt(listField + "[" + i + "] ('" + entry.label() + "')", e);
			}
		}
		return certificates;
	}

	private static IllegalStateException corrupt(String field, Exception cause) {
		return new IllegalStateException("The stored Chile CMF server settings are corrupt: " + field + ": "
			+ cause.getMessage() + ". An admin must re-save them on the Server settings page.", cause);
	}
}
