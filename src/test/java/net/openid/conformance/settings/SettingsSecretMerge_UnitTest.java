package net.openid.conformance.settings;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SettingsSecretMerge_UnitTest {

	private static final CertificateEntry STORED_POSITIVE = new CertificateEntry("pos-1", "primary", "CERT-P", "KEY-P");
	private static final CertificateEntry STORED_NEGATIVE = new CertificateEntry("neg-1", "expired", "CERT-N", "KEY-N");

	private static final CMFChileSettings STORED = new CMFChileSettings(
		"https://old.example.cl/token", "old-client", "old-secret", "{\"keys\":[]}",
		List.of(STORED_POSITIVE), List.of(STORED_NEGATIVE), Instant.parse("2026-01-01T00:00:00Z"), "Old Admin");

	private static CMFChileSettingsUpdate update(String secret, boolean clear, String jwks,
			List<CertificateEntryUpdate> positive, List<CertificateEntryUpdate> negative) {
		return new CMFChileSettingsUpdate("https://new.example.cl/token", "new-client", secret, clear, jwks, positive, negative);
	}

	private static CMFChileSettingsUpdate keepEverything() {
		return update(null, false, null,
			List.of(new CertificateEntryUpdate("pos-1", "primary", "CERT-P", null)),
			List.of(new CertificateEntryUpdate("neg-1", "expired", "CERT-N", null)));
	}

	@Test
	public void takesPlainFieldsFromTheUpdate() {
		CMFChileSettings merged = SettingsSecretMerge.merge(STORED, keepEverything());

		assertThat(merged.directoryTokenEndpoint()).isEqualTo("https://new.example.cl/token");
		assertThat(merged.clientId()).isEqualTo("new-client");
	}

	@Test
	public void blankPlainFieldsBecomeUnset() {
		CMFChileSettingsUpdate blank = new CMFChileSettingsUpdate("  ", "", null, false, null, List.of(), List.of());

		CMFChileSettings merged = SettingsSecretMerge.merge(STORED, blank);

		assertThat(merged.directoryTokenEndpoint()).isNull();
		assertThat(merged.clientId()).isNull();
	}

	@Test
	public void keepsTheAuditFieldsOfTheStoredSettings() {
		CMFChileSettings merged = SettingsSecretMerge.merge(STORED, keepEverything());

		assertThat(merged.updatedAt()).isEqualTo(STORED.updatedAt());
		assertThat(merged.updatedBy()).isEqualTo("Old Admin");
	}

	@Test
	public void anAbsentSecretKeepsTheStoredOne() {
		assertThat(SettingsSecretMerge.merge(STORED, keepEverything()).clientSecret()).isEqualTo("old-secret");
	}

	@Test
	public void anEmptySecretKeepsTheStoredOne() {
		CMFChileSettingsUpdate update = update("", false, null, List.of(), List.of());

		assertThat(SettingsSecretMerge.merge(STORED, update).clientSecret()).isEqualTo("old-secret");
	}

	@Test
	public void aNewSecretReplacesTheStoredOne() {
		CMFChileSettingsUpdate update = update("new-secret", false, null, List.of(), List.of());

		assertThat(SettingsSecretMerge.merge(STORED, update).clientSecret()).isEqualTo("new-secret");
	}

	@Test
	public void theClearFlagRemovesTheSecretEvenWhenOneIsSent() {
		CMFChileSettingsUpdate update = update("new-secret", true, null, List.of(), List.of());

		assertThat(SettingsSecretMerge.merge(STORED, update).clientSecret()).isNull();
	}

	@Test
	public void anAbsentJwksKeepsTheStoredOne() {
		assertThat(SettingsSecretMerge.merge(STORED, keepEverything()).clientJwks()).isEqualTo("{\"keys\":[]}");
	}

	@Test
	public void aJwksReplacesTheStoredOne() {
		CMFChileSettingsUpdate update = update(null, false, "{\"keys\":[{\"kty\":\"oct\"}]}", List.of(), List.of());

		assertThat(SettingsSecretMerge.merge(STORED, update).clientJwks()).isEqualTo("{\"keys\":[{\"kty\":\"oct\"}]}");
	}

	@Test
	public void aKnownEntryWithoutAKeyKeepsItsStoredKeyAndId() {
		CMFChileSettings merged = SettingsSecretMerge.merge(STORED, keepEverything());

		assertThat(merged.positiveCertificates()).containsExactly(STORED_POSITIVE);
		assertThat(merged.negativeCertificates()).containsExactly(STORED_NEGATIVE);
	}

	@Test
	public void aKnownEntryWithANewKeyAndLabelIsUpdatedInPlace() {
		CMFChileSettingsUpdate update = update(null, false, null,
			List.of(new CertificateEntryUpdate("pos-1", " renamed ", "CERT-P2", "KEY-P2")), List.of());

		assertThat(SettingsSecretMerge.merge(STORED, update).positiveCertificates())
			.containsExactly(new CertificateEntry("pos-1", "renamed", "CERT-P2", "KEY-P2"));
	}

	@Test
	public void aNewEntryGetsAFreshIdAndNoKeyUnlessOneIsSent() {
		CMFChileSettingsUpdate update = update(null, false, null, List.of(), List.of(
			new CertificateEntryUpdate(null, "untrusted CA", "CERT-U", null),
			new CertificateEntryUpdate(null, "revoked", "CERT-R", "KEY-R")));

		List<CertificateEntry> merged = SettingsSecretMerge.merge(STORED, update).negativeCertificates();

		assertThat(merged).hasSize(2);
		assertThat(merged.get(0).id()).isNotBlank().isNotEqualTo("neg-1");
		assertThat(merged.get(0).privateKeyPem()).isNull();
		assertThat(merged.get(1).privateKeyPem()).isEqualTo("KEY-R");
		assertThat(merged.get(1).id()).isNotEqualTo(merged.get(0).id());
	}

	@Test
	public void anEntryLeftOutOfTheUpdateIsDeleted() {
		CMFChileSettingsUpdate update = update(null, false, null,
			List.of(new CertificateEntryUpdate("pos-1", "primary", "CERT-P", null)), List.of());

		assertThat(SettingsSecretMerge.merge(STORED, update).negativeCertificates()).isEmpty();
	}

	@Test
	public void anIdFromTheOtherListDoesNotMatch() {
		CMFChileSettingsUpdate update = update(null, false, null,
			List.of(new CertificateEntryUpdate("neg-1", "moved", "CERT-N", null)), List.of());

		CertificateEntry merged = SettingsSecretMerge.merge(STORED, update).positiveCertificates().get(0);

		assertThat(merged.id()).isNotEqualTo("neg-1");
		assertThat(merged.privateKeyPem()).isNull();
	}

	@Test
	public void aRepeatedIdMatchesOnlyOnce() {
		CMFChileSettingsUpdate update = update(null, false, null, List.of(
			new CertificateEntryUpdate("pos-1", "primary", "CERT-P", null),
			new CertificateEntryUpdate("pos-1", "copy", "CERT-P", null)), List.of());

		List<CertificateEntry> merged = SettingsSecretMerge.merge(STORED, update).positiveCertificates();

		assertThat(merged.get(0)).isEqualTo(STORED_POSITIVE);
		assertThat(merged.get(1).id()).isNotEqualTo("pos-1");
		assertThat(merged.get(1).privateKeyPem()).isNull();
	}

	@Test
	public void mergesOntoAnEmptySection() {
		CMFChileSettings merged = SettingsSecretMerge.merge(CMFChileSettings.empty(), keepEverything());

		assertThat(merged.clientSecret()).isNull();
		assertThat(merged.positiveCertificates().get(0).privateKeyPem()).isNull();
	}
}
