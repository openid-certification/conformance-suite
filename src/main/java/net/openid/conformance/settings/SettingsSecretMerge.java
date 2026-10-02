package net.openid.conformance.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Applies an update from the settings page to the stored section. The page never receives the
 * stored secrets, so it cannot send them back: an absent client secret, JWKS or private key means
 * "keep what is stored", and only the explicit clear flags remove the stored client secret or JWKS. The result is not validated here; an entry that is new and has no key
 * comes out with a null key, which {@link CMFChileSettingsValidator} reports.
 */
public final class SettingsSecretMerge {

	private SettingsSecretMerge() {
	}

	public static CMFChileSettings merge(CMFChileSettings stored, CMFChileSettingsUpdate update) {
		String clientSecret;
		if (update.clearClientSecret()) {
			clientSecret = null;
		} else if (isBlank(update.clientSecret())) {
			clientSecret = stored.clientSecret();
		} else {
			clientSecret = update.clientSecret();
		}

		String clientJwks;
		if (update.clearClientJwks()) {
			clientJwks = null;
		} else if (update.clientJwks() == null) {
			clientJwks = stored.clientJwks();
		} else {
			clientJwks = update.clientJwks();
		}

		return new CMFChileSettings(
			blankToNull(update.directoryTokenEndpoint()),
			blankToNull(update.softwareStatementEndpoint()),
			blankToNull(update.clientId()),
			clientSecret,
			clientJwks,
			mergeEntries(stored.positiveCertificates(), update.positiveCertificates()),
			mergeEntries(stored.negativeCertificates(), update.negativeCertificates()),
			stored.updatedAt(),
			stored.updatedBy());
	}

	/**
	 * Entries are matched by id within the same list only. A stored entry absent from the update is
	 * dropped.
	 */
	private static List<CertificateEntry> mergeEntries(List<CertificateEntry> stored, List<CertificateEntryUpdate> updates) {
		Map<String, CertificateEntry> unmatched = new HashMap<>();
		for (CertificateEntry entry : stored) {
			unmatched.put(entry.id(), entry);
		}

		List<CertificateEntry> merged = new ArrayList<>();
		for (CertificateEntryUpdate update : updates) {
			// removing on match makes an id repeated within one update match only once
			CertificateEntry existing = update.id() == null ? null : unmatched.remove(update.id());
			String privateKey;
			if (!isBlank(update.privateKeyPem())) {
				privateKey = update.privateKeyPem();
			} else {
				privateKey = existing == null ? null : existing.privateKeyPem();
			}
			merged.add(new CertificateEntry(
				existing == null ? UUID.randomUUID().toString() : existing.id(),
				update.label() == null ? null : update.label().strip(),
				update.certificateChainPem(),
				privateKey));
		}
		return merged;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private static String blankToNull(String value) {
		return isBlank(value) ? null : value.strip();
	}
}
