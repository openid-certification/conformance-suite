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
	 */
	DBObject fillPlaceholder(String testId, String placeholder, Update update);

	/**
	 * Get unfilled placeholders
	 */
	List<DBObject> getRemainingPlaceholders(String testId);

	/**
	 * If there aren't any placeholders left on the test, to update the status to FINISHED
	 */
	void lastPlaceholderFilled(String testId);

	/**
	 * Get all the images for a test
	 */
	List<DBObject> getAllImagesForTestId(String testId);
}
