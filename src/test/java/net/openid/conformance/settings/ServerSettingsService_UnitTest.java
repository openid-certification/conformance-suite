package net.openid.conformance.settings;

import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ServerSettingsService_UnitTest {

	/** Counts reads so the tests can tell a cache hit from a load. */
	private static final class InMemoryRepository implements ServerSettingsRepository {
		private final Map<String, Document> documents = new HashMap<>();
		private int finds;

		@Override
		public Optional<Document> find(String sectionId) {
			finds++;
			Document document = documents.get(sectionId);
			return Optional.ofNullable(document == null ? null : new Document(document));
		}

		@Override
		public void save(String sectionId, Document document) {
			documents.put(sectionId, new Document(document));
		}
	}

	private static CMFChileSettings valid;

	private InMemoryRepository repository;
	private ServerSettingsService service;

	@BeforeAll
	public static void createSettings() throws Exception {
		valid = TestPki.validSettings();
	}

	@BeforeEach
	public void setUp() {
		repository = new InMemoryRepository();
		service = new ServerSettingsService(repository);
	}

	private static CMFChileSettingsUpdate keepAllWithClientId(CMFChileSettings stored, String clientId) {
		return new CMFChileSettingsUpdate(stored.directoryTokenEndpoint(), stored.softwareStatementEndpoint(), clientId, null, false, null, false,
			stored.positiveCertificates().stream()
				.map(e -> new CertificateEntryUpdate(e.id(), e.label(), e.certificateChainPem(), null)).toList(),
			stored.negativeCertificates().stream()
				.map(e -> new CertificateEntryUpdate(e.id(), e.label(), e.certificateChainPem(), null)).toList());
	}

	@Test
	public void directorySettingsAreEmptyWhenNothingWasSaved() {
		assertThat(service.getCMFChileDirectorySettings()).isEmpty();
		assertThat(service.getCMFChileDirectorySettings()).isEmpty();

		assertThat(repository.finds).isEqualTo(1);
	}

	@Test
	public void storedSettingsAreEmptyWhenNothingWasSaved() {
		assertThat(service.getCMFChileSettings()).isEqualTo(CMFChileSettings.empty());
	}

	@Test
	public void directorySettingsAreLoadedAndParsedOnce() {
		repository.save(CMFChileSettings.SECTION_ID, valid.toDocument());

		CMFChileDirectorySettings first = service.getCMFChileDirectorySettings().orElseThrow();
		CMFChileDirectorySettings second = service.getCMFChileDirectorySettings().orElseThrow();

		assertThat(second).isSameAs(first);
		assertThat(repository.finds).isEqualTo(1);
		assertThat(first.clientId()).isEqualTo("oidf-conformance");
	}

	@Test
	public void aSaveReplacesTheCachedSettings() {
		repository.save(CMFChileSettings.SECTION_ID, valid.toDocument());
		service.getCMFChileDirectorySettings();

		ServerSettingsService.SaveResult result = service.saveCMFChileSettings(keepAllWithClientId(valid, "new-client"), "Admin User");
		int findsAfterSave = repository.finds;

		assertThat(result.errors()).isEmpty();
		assertThat(service.getCMFChileDirectorySettings().orElseThrow().clientId()).isEqualTo("new-client");
		assertThat(repository.finds).isEqualTo(findsAfterSave);
	}

	@Test
	public void aSaveBeforeTheFirstReadIsWhatTheFirstReadReturns() {
		repository.save(CMFChileSettings.SECTION_ID, valid.toDocument());

		service.saveCMFChileSettings(keepAllWithClientId(valid, "saved-first"), "Admin User");

		assertThat(service.getCMFChileDirectorySettings().orElseThrow().clientId()).isEqualTo("saved-first");
	}

	@Test
	public void aRefusedSaveLeavesStorageAndCacheAlone() {
		repository.save(CMFChileSettings.SECTION_ID, valid.toDocument());
		CMFChileDirectorySettings cached = service.getCMFChileDirectorySettings().orElseThrow();
		CMFChileSettingsUpdate bad = new CMFChileSettingsUpdate("http://insecure.example.cl", null, "x", null, false, null, false,
			List.of(), List.of());

		ServerSettingsService.SaveResult result = service.saveCMFChileSettings(bad, "Admin User");

		assertThat(result.errors()).extracting(SettingsError::field).containsExactly("directoryTokenEndpoint");
		assertThat(service.getCMFChileDirectorySettings().orElseThrow()).isSameAs(cached);
		assertThat(service.getCMFChileSettings().clientId()).isEqualTo("oidf-conformance");
	}

	@Test
	public void aNewEntryWithoutAKeyIsRefused() {
		CMFChileSettingsUpdate update = new CMFChileSettingsUpdate(null, null, null, null, false, null, false,
			List.of(new CertificateEntryUpdate(null, "primary", valid.positiveCertificates().get(0).certificateChainPem(), null)),
			List.of());

		ServerSettingsService.SaveResult result = service.saveCMFChileSettings(update, "Admin User");

		assertThat(result.errors()).extracting(SettingsError::field).containsExactly("positiveCertificates[0].privateKeyPem");
		assertThat(repository.documents).isEmpty();
	}

	@Test
	public void aSaveKeepsSecretsThatWereNotResentAndRecordsWhoAndWhen() {
		repository.save(CMFChileSettings.SECTION_ID, valid.toDocument());
		Instant before = Instant.now();

		ServerSettingsService.SaveResult result = service.saveCMFChileSettings(keepAllWithClientId(valid, "new-client"), "Admin User");

		CMFChileSettings stored = service.getCMFChileSettings();
		assertThat(result.settings()).isEqualTo(stored);
		assertThat(stored.softwareStatementEndpoint()).isEqualTo("https://directory.example.cl/software-statement");
		assertThat(stored.clientSecret()).isEqualTo("the-client-secret");
		assertThat(stored.clientJwks()).isEqualTo(valid.clientJwks());
		assertThat(stored.positiveCertificates()).isEqualTo(valid.positiveCertificates());
		assertThat(stored.updatedBy()).isEqualTo("Admin User");
		assertThat(stored.updatedAt()).isAfterOrEqualTo(before.minusMillis(1));
	}

	@Test
	public void aCorruptDocumentThrowsAndIsNotCached() {
		repository.save(CMFChileSettings.SECTION_ID, new CMFChileSettings(null, null, null, null, "not json",
			List.of(), List.of(), null, null).toDocument());

		assertThatThrownBy(() -> service.getCMFChileDirectorySettings()).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> service.getCMFChileDirectorySettings()).isInstanceOf(IllegalStateException.class);

		assertThat(repository.finds).isEqualTo(2);
	}
}
