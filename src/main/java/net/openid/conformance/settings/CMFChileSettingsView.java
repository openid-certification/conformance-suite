package net.openid.conformance.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

/**
 * The form of the Chile CMF section sent to the settings page. It says whether each secret is set
 * but never contains the client secret, private JWKS members or private-key PEMs. Certificates are
 * public and are returned whole, with their subject, issuer and expiry.
 */
public final class CMFChileSettingsView {

	private CMFChileSettingsView() {
	}

	public static JsonObject toJson(CMFChileSettings settings) {
		JsonObject view = new JsonObject();
		view.addProperty(CMFChileSettings.DIRECTORY_TOKEN_ENDPOINT, settings.directoryTokenEndpoint());
		view.addProperty(CMFChileSettings.CLIENT_ID, settings.clientId());
		view.addProperty("clientSecretSet", settings.clientSecret() != null);
		view.add(CMFChileSettings.CLIENT_JWKS, jwksSummary(settings.clientJwks()));
		view.add(CMFChileSettings.POSITIVE_CERTIFICATES, certificates(settings.positiveCertificates()));
		view.add(CMFChileSettings.NEGATIVE_CERTIFICATES, certificates(settings.negativeCertificates()));
		view.addProperty(CMFChileSettings.UPDATED_AT, settings.updatedAt() == null ? null : settings.updatedAt().toString());
		view.addProperty(CMFChileSettings.UPDATED_BY, settings.updatedBy());
		// unset plain fields are omitted rather than sent as null
		view.entrySet().removeIf(member -> member.getValue().isJsonNull());
		return view;
	}

	private static JsonObject jwksSummary(String jwksJson) {
		JsonObject summary = new JsonObject();
		JsonArray keys = new JsonArray();
		summary.addProperty("set", jwksJson != null);
		if (jwksJson != null) {
			try {
				for (JWK key : JWKSet.parse(jwksJson).getKeys()) {
					JsonObject keySummary = new JsonObject();
					keySummary.addProperty("kid", key.getKeyID());
					keySummary.addProperty("kty", key.getKeyType().getValue());
					if (key.getAlgorithm() != null) {
						keySummary.addProperty("alg", key.getAlgorithm().getName());
					}
					if (key.getKeyUse() != null) {
						keySummary.addProperty("use", key.getKeyUse().identifier());
					}
					keys.add(keySummary);
				}
			} catch (ParseException e) {
				summary.addProperty("unreadable", true);
			}
		}
		summary.add("keys", keys);
		return summary;
	}

	private static JsonArray certificates(List<CertificateEntry> entries) {
		JsonArray array = new JsonArray();
		for (CertificateEntry entry : entries) {
			JsonObject view = new JsonObject();
			view.addProperty("id", entry.id());
			view.addProperty("label", entry.label());
			view.addProperty("certificateChainPem", entry.certificateChainPem());
			view.addProperty("privateKeySet", entry.privateKeyPem() != null);
			try {
				X509Certificate leaf = PemParsing.parseCertificateChain(entry.certificateChainPem()).get(0);
				view.addProperty("subject", leaf.getSubjectX500Principal().getName());
				view.addProperty("issuer", leaf.getIssuerX500Principal().getName());
				view.addProperty("notAfter", leaf.getNotAfter().toInstant().toString());
			} catch (IllegalArgumentException e) {
				view.addProperty("unreadable", true);
			}
			array.add(view);
		}
		return array;
	}
}
