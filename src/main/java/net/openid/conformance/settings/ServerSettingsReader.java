package net.openid.conformance.settings;

import java.util.Optional;

/**
 * Read-only access to the server settings, as given to test modules.
 */
// one accessor per settings section; not meant as a lambda target
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ServerSettingsReader {

	/**
	 * Parsed once and cached; a save by an admin applies to calls made after it.
	 *
	 * @return empty if an admin has never saved the Chile CMF section
	 * @throws IllegalStateException if the stored section is corrupt
	 */
	Optional<CMFChileDirectorySettings> getCMFChileDirectorySettings();
}
