package net.openid.conformance.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loads, saves and caches the server settings.
 *
 * <p>The parsed Chile CMF section is cached in memory: the first read after startup loads and
 * parses it, and every successful save replaces it. This assumes a single suite instance per
 * database, as running test modules already do; a change made directly in MongoDB is seen only
 * after a restart.
 */
@Service
public class ServerSettingsService implements ServerSettingsReader {

	private static final Logger logger = LoggerFactory.getLogger(ServerSettingsService.class);

	private final ServerSettingsRepository repository;

	/** null until first loaded; Optional.empty() when nothing has been saved. */
	private final AtomicReference<Optional<CMFChileDirectorySettings>> directorySettings = new AtomicReference<>();

	public ServerSettingsService(ServerSettingsRepository repository) {
		this.repository = repository;
	}

	/**
	 * @param settings the stored section as it is after the call: the new one if saved, the old one if refused
	 * @param errors empty if the section was saved
	 */
	public record SaveResult(CMFChileSettings settings, List<SettingsError> errors) {
	}

	/**
	 * @return the stored section, secrets included; {@link CMFChileSettings#empty()} if never saved
	 */
	public CMFChileSettings getCMFChileSettings() {
		return repository.find(CMFChileSettings.SECTION_ID)
			.map(CMFChileSettings::fromDocument)
			.orElseGet(CMFChileSettings::empty);
	}

	/**
	 * Merges the update onto the stored section, validates it and, if valid, saves it and replaces the
	 * cached parsed settings. Synchronized so concurrent saves cannot interleave their merges.
	 */
	public synchronized SaveResult saveCMFChileSettings(CMFChileSettingsUpdate update, String updatedBy) {
		CMFChileSettings stored = getCMFChileSettings();
		CMFChileSettings merged = SettingsSecretMerge.merge(stored, update);
		List<SettingsError> errors = CMFChileSettingsValidator.validate(merged);
		if (!errors.isEmpty()) {
			return new SaveResult(stored, errors);
		}

		// MongoDB dates hold milliseconds; truncating keeps the returned settings equal to what a reload reads
		CMFChileSettings saved = merged.withAudit(Instant.now().truncatedTo(ChronoUnit.MILLIS), updatedBy);
		CMFChileDirectorySettings parsed = CMFChileSettingsParser.parse(saved);
		repository.save(CMFChileSettings.SECTION_ID, saved.toDocument());
		directorySettings.set(Optional.of(parsed));
		logger.info("Server settings section '{}' saved by {}; changed fields: {}",
			CMFChileSettings.SECTION_ID, updatedBy, saved.changedFields(stored));
		return new SaveResult(saved, List.of());
	}

	@Override
	public Optional<CMFChileDirectorySettings> getCMFChileDirectorySettings() {
		Optional<CMFChileDirectorySettings> cached = directorySettings.get();
		if (cached != null) {
			return cached;
		}
		Optional<CMFChileDirectorySettings> loaded = repository.find(CMFChileSettings.SECTION_ID)
			.map(CMFChileSettings::fromDocument)
			.map(CMFChileSettingsParser::parse);
		// a save that completed while this was loading has already cached fresher settings; keep them
		directorySettings.compareAndSet(null, loaded);
		return directorySettings.get();
	}
}
