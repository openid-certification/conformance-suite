package net.openid.conformance.info;

import org.bson.Document;

import java.util.List;
import java.util.Map;

public interface ImageService {
	/**
	 * Fill a placeholder with the given content
	 *
	 * @param assumeAdmin If true, no access controls will be applied. Only set to true if being called from the
	 *                    test module itself, not via the REST API.
	 */
	Document fillPlaceholder(String testId, String placeholder, Map<String, Object> update, boolean assumeAdmin);

	/**
	 * Get unfilled placeholder IDs
	 *
	 * @param assumeAdmin If true, no access controls will be applied. Only set to true if being called from the
	 *                    test module itself, not via the REST API.
	 */
	List<String> getRemainingPlaceholders(String testId, boolean assumeAdmin);

	/**
	 * Get filled placeholder IDs
	 *
	 * This will only work before a test completes, as completing a test marks all placeholders as satisfied.
	 *
	 * @param assumeAdmin If true, no access controls will be applied. Only set to true if being called from the
	 *                    test module itself, not via the REST API.
	 */
	List<Document> getFilledPlaceholders(String testId, boolean assumeAdmin);

	/**
	 * Get all the images for a test
	 *
	 * @param assumeAdmin If true, no access controls will be applied. Only set to true if being called from the
	 *                    test module itself, not via the REST API.
	 */
	List<Document> getAllImagesForTestId(String testId, boolean assumeAdmin);
}
