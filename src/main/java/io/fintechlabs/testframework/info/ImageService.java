package io.fintechlabs.testframework.info;

import java.util.List;
import java.util.Map;

import com.mongodb.DBObject;

/**
 * @author jheenan
 */
public interface ImageService {
	/**
	 * Fill a placeholder with the given content
	 *
	 * @param assumeAdmin If true, no access controls will be applied. Only set to true if being called from the
	 *                    test module itself, not via the REST API.
	 */
	DBObject fillPlaceholder(String testId, String placeholder, Map<String, Object> update, boolean assumeAdmin);

	/**
	 * Get unfilled placeholder IDs
	 *
	 * @param assumeAdmin If true, no access controls will be applied. Only set to true if being called from the
	 *                    test module itself, not via the REST API.
	 */
	List<String> getRemainingPlaceholders(String testId, boolean assumeAdmin);

	/**
	 * If there aren't any placeholders left on the test, to update the status to FINISHED
	 *
	 * @param assumeAdmin If true, no access controls will be applied. Only set to true if being called from the
	 *                    test module itself, not via the REST API.
	 */
	void lastPlaceholderFilled(String testId, boolean assumeAdmin);

	/**
	 * Get all the images for a test
	 *
	 * @param assumeAdmin If true, no access controls will be applied. Only set to true if being called from the
	 *                    test module itself, not via the REST API.
	 */
	List<DBObject> getAllImagesForTestId(String testId, boolean assumeAdmin);
}
