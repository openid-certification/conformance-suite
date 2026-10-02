package net.openid.conformance.settings;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks a merged Chile CMF section before it is saved. Every field is optional; what is present
 * must be usable. Expired certificates are accepted because a negative case may be one on purpose.
 * Messages name fields by their labels on the settings page.
 */
public final class CMFChileSettingsValidator {

	static final String POSITIVE_CERTIFICATES_LABEL = "Positive DCR client certificates";
	static final String NEGATIVE_CERTIFICATES_LABEL = "Negative DCR client certificates";

	private CMFChileSettingsValidator() {
	}

	public static List<SettingsError> validate(CMFChileSettings settings) {
		List<SettingsError> errors = new ArrayList<>();
		validateHttpsUrl(CMFChileSettings.DIRECTORY_TOKEN_ENDPOINT, "Directory token endpoint URL",
			settings.directoryTokenEndpoint(), errors);
		validateHttpsUrl(CMFChileSettings.SOFTWARE_STATEMENT_ENDPOINT, "Software statement endpoint URL",
			settings.softwareStatementEndpoint(), errors);
		validateJwks(settings.clientJwks(), errors);
		validateCertificates(CMFChileSettings.POSITIVE_CERTIFICATES, POSITIVE_CERTIFICATES_LABEL,
			settings.positiveCertificates(), errors);
		validateCertificates(CMFChileSettings.NEGATIVE_CERTIFICATES, NEGATIVE_CERTIFICATES_LABEL,
			settings.negativeCertificates(), errors);
		return List.copyOf(errors);
	}

	private static void validateHttpsUrl(String field, String label, String url, List<SettingsError> errors) {
		if (url == null) {
			return;
		}
		boolean valid;
		try {
			URI uri = new URI(url);
			valid = "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
		} catch (URISyntaxException e) {
			valid = false;
		}
		if (!valid) {
			errors.add(new SettingsError(field, "'" + label + "' must be an absolute https:// URL"));
		}
	}

	private static void validateJwks(String jwksJson, List<SettingsError> errors) {
		if (jwksJson == null) {
			return;
		}
		JWKSet jwks;
		try {
			jwks = JWKSet.parse(jwksJson);
		} catch (ParseException e) {
			errors.add(new SettingsError(CMFChileSettings.CLIENT_JWKS, "'Client JWKS' is not a valid JWKS: " + e.getMessage()));
			return;
		}
		List<JWK> keys = jwks.getKeys();
		if (keys.isEmpty()) {
			errors.add(new SettingsError(CMFChileSettings.CLIENT_JWKS, "'Client JWKS' contains no keys"));
			return;
		}
		for (int i = 0; i < keys.size(); i++) {
			JWK key = keys.get(i);
			if (key.getKeyID() == null) {
				errors.add(new SettingsError(CMFChileSettings.CLIENT_JWKS, "'Client JWKS' key " + (i + 1) + " has no 'kid'"));
			}
			if (!key.isPrivate()) {
				String name = key.getKeyID() == null ? "key " + (i + 1) : "key '" + key.getKeyID() + "'";
				errors.add(new SettingsError(CMFChileSettings.CLIENT_JWKS,
					"'Client JWKS' " + name + " has no private key; each key must include its private part"));
			}
		}
	}

	private static void validateCertificates(String listField, String listLabel, List<CertificateEntry> entries,
			List<SettingsError> errors) {
		Set<String> labels = new HashSet<>();
		for (int i = 0; i < entries.size(); i++) {
			CertificateEntry entry = entries.get(i);
			String field = listField + "[" + i + "]";
			boolean hasLabel = entry.label() != null && !entry.label().isBlank();
			String name = hasLabel ? "'" + entry.label() + "'" : "entry " + (i + 1);
			String where = name + " in '" + listLabel + "'";

			if (!hasLabel) {
				errors.add(new SettingsError(field + ".label", "'Label' is required for " + where));
			} else if (!labels.add(entry.label())) {
				errors.add(new SettingsError(field + ".label", "'Label' " + name + " is used more than once in '" + listLabel + "'"));
			}

			List<X509Certificate> chain = null;
			try {
				chain = PemParsing.parseCertificateChain(entry.certificateChainPem());
			} catch (IllegalArgumentException e) {
				errors.add(new SettingsError(field + ".certificateChainPem",
					"'Certificate chain' of " + where + " is invalid: " + e.getMessage()));
			}

			String keyField = field + ".privateKeyPem";
			if (entry.privateKeyPem() == null || entry.privateKeyPem().isBlank()) {
				errors.add(new SettingsError(keyField, "'Private key' is required for " + where));
				continue;
			}
			PrivateKey key;
			try {
				key = PemParsing.parsePrivateKey(entry.privateKeyPem());
			} catch (IllegalArgumentException e) {
				errors.add(new SettingsError(keyField, "'Private key' of " + where + " is invalid: " + e.getMessage()));
				continue;
			}
			if (chain == null) {
				continue;
			}
			try {
				if (!PemParsing.keyMatchesCertificate(key, chain.get(0))) {
					errors.add(new SettingsError(keyField,
						"'Private key' of " + where + " does not match the first certificate in its chain"));
				}
			} catch (IllegalArgumentException e) {
				errors.add(new SettingsError(keyField, "'Private key' of " + where + " is invalid: " + e.getMessage()));
			}
		}
	}
}
