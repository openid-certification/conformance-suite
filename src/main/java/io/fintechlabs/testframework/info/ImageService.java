package io.fintechlabs.testframework.info;

import com.mongodb.DBObject;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

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
	DBObject fillPlaceholder(String testId, String placeholder, Update update, boolean assumeAdmin);

	/**
	 * Get unfilled placeholders
	 *
	 * @param assumeAdmin If true, no access controls will be applied. Only set to true if being called from the
	 *                    test module itself, not via the REST API.
	 */
	List<DBObject> getRemainingPlaceholders(String testId, boolean assumeAdmin);

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
