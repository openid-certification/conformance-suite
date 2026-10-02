package net.openid.conformance.settings;

import org.bson.Document;

import java.util.Optional;

/**
 * Storage for server settings, one document per section.
 */
public interface ServerSettingsRepository {

	Optional<Document> find(String sectionId);

	/** Replaces the section's document, creating it if absent. */
	void save(String sectionId, Document document);
}
