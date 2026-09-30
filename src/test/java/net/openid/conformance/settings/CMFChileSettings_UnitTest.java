package net.openid.conformance.settings;

import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CMFChileSettings_UnitTest {

	private static CMFChileSettings settings;

	@BeforeAll
	public static void createSettings() throws Exception {
		settings = TestPki.validSettings().withAudit(Instant.parse("2026-09-30T12:00:00Z"), "Admin User");
	}

	@Test
	public void roundTripsThroughAMongoDocument() {
		Document document = settings.toDocument();

		assertThat(document.get("_id")).isEqualTo(CMFChileSettings.SECTION_ID);
		assertThat(CMFChileSettings.fromDocument(document)).isEqualTo(settings);
	}

	@Test
	public void roundTripsAnEmptySection() {
		assertThat(CMFChileSettings.fromDocument(CMFChileSettings.empty().toDocument())).isEqualTo(CMFChileSettings.empty());
	}

	@Test
	public void readsADocumentWithoutCertificateLists() {
		CMFChileSettings read = CMFChileSettings.fromDocument(new Document("_id", CMFChileSettings.SECTION_ID)
			.append(CMFChileSettings.CLIENT_ID, "abc"));

		assertThat(read.clientId()).isEqualTo("abc");
		assertThat(read.positiveCertificates()).isEmpty();
		assertThat(read.negativeCertificates()).isEmpty();
		assertThat(read.updatedAt()).isNull();
	}

	@Test
	public void emptyHasNoValues() {
		CMFChileSettings empty = CMFChileSettings.empty();

		assertThat(empty.directoryTokenEndpoint()).isNull();
		assertThat(empty.clientSecret()).isNull();
		assertThat(empty.positiveCertificates()).isEmpty();
	}

	@Test
	public void withAuditSetsWhenAndWho() {
		assertThat(settings.updatedAt()).isEqualTo(Instant.parse("2026-09-30T12:00:00Z"));
		assertThat(settings.updatedBy()).isEqualTo("Admin User");
	}

	@Test
	public void changedFieldsListsOnlyWhatDiffers() {
		CMFChileSettings changed = new CMFChileSettings(settings.directoryTokenEndpoint(), "other-client",
			"other-secret", settings.clientJwks(), settings.positiveCertificates(), List.of(),
			Instant.now(), "Someone Else");

		assertThat(changed.changedFields(settings))
			.containsExactly(CMFChileSettings.CLIENT_ID, CMFChileSettings.CLIENT_SECRET, CMFChileSettings.NEGATIVE_CERTIFICATES);
	}

	@Test
	public void changedFieldsAgainstEmptyListsEverythingSet() {
		assertThat(settings.changedFields(CMFChileSettings.empty())).containsExactly(
			CMFChileSettings.DIRECTORY_TOKEN_ENDPOINT, CMFChileSettings.CLIENT_ID, CMFChileSettings.CLIENT_SECRET,
			CMFChileSettings.CLIENT_JWKS, CMFChileSettings.POSITIVE_CERTIFICATES, CMFChileSettings.NEGATIVE_CERTIFICATES);
	}

	@Test
	public void toStringOmitsSecretsAndKeys() {
		String text = settings.toString();

		assertThat(text).doesNotContain("the-client-secret");
		assertThat(text).doesNotContain("PRIVATE KEY");
		assertThat(text).doesNotContain("\"d\"");
		assertThat(text).contains("oidf-conformance");
	}

	@Test
	public void certificateEntryToStringOmitsTheKey() {
		String text = settings.positiveCertificates().get(0).toString();

		assertThat(text).doesNotContain("PRIVATE KEY");
		assertThat(text).contains("primary");
	}
}
