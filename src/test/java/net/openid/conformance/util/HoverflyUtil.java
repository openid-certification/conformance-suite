package net.openid.conformance.util;

import io.specto.hoverfly.junit.core.model.RequestFieldMatcher;

import java.util.List;
import java.util.Map;

/**
 * Helper class to facilitate the creation of hoverfly matchers.
 */
public class HoverflyUtil {

	private HoverflyUtil() {
		// prevent instantiation
	}

	/**
	 * Create a matcher for a single form post body value.
	 * @param fieldName
	 * @param value
	 * @return
	 */
	public static RequestFieldMatcher<Map<String, List<RequestFieldMatcher<?>>>> formBodyFieldMatcher(String fieldName, String value) {
		return RequestFieldMatcher.newFormMatcher(Map.of(fieldName, List.of(RequestFieldMatcher.newExactMatcher(value))));
	}
}
