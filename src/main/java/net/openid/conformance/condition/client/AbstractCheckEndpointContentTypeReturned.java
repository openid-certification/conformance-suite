package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractCheckEndpointContentTypeReturned extends AbstractCondition {

	// this currently copes with the "old" way of using three seperate environment variables, one for each status / headers / body and the new preferred way of the object returned by AbstractCondition.convertResponseForEnvironment
	protected Environment checkContentType(Environment env, String headersEnvKey, String pathPrefix, String expected) {
		String contentType = getContentType(env, headersEnvKey, pathPrefix);

		String mimeType = getMimeTypeFromContentType(contentType);

		if (expected.equals(mimeType)) {
			logSuccess(headersEnvKey + " Content-Type: header is " + expected);
			return env;
		}

		throw error("Invalid content-type header in "+headersEnvKey, args("expected", expected, "actual", contentType));
	}

	public static String getMimeTypeFromContentType(String contentType) {
		String mimeType = null;
		try {
			mimeType = contentType.split(";")[0].trim();
		} catch (Exception e) {
		}
		return mimeType;
	}

	protected String getContentType(Environment env, String headersEnvKey, String pathPrefix) {
		String contentType = env.getString(headersEnvKey, pathPrefix + "content-type");
		if (Strings.isNullOrEmpty(contentType)) {
			throw error("Couldn't find content-type header in "+ headersEnvKey);
		}
		return contentType;
	}
}
