package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class EnsureContentTypeIsAnyOf extends AbstractCheckEndpointContentTypeReturned {

	/** Lower case, as media type names are case-insensitive (RFC 9110 Section 8.3.1). */
	private final Set<String> expectedContentTypes;

	public EnsureContentTypeIsAnyOf(String firstContentType, String... additionalContentTypes) {
		var set = new TreeSet<String>();
		set.add(firstContentType.toLowerCase(Locale.ROOT));
		if (additionalContentTypes != null) {
			for (String contentType : additionalContentTypes) {
				set.add(contentType.toLowerCase(Locale.ROOT));
			}
		}
		expectedContentTypes = Set.copyOf(set);
	}

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		String headersEnvKey = "endpoint_response";
		String contentType = getContentType(env, headersEnvKey, "headers.");

		String mimeType = getMimeTypeFromContentType(contentType);

		if (expectedContentTypes.contains(mimeType.toLowerCase(Locale.ROOT))) {
			logSuccess(headersEnvKey + " Content-Type: header is one of " + expectedContentTypes, args("actual", contentType));
			return env;
		}

		throw error("Invalid content-type header in "+headersEnvKey, args("allowed_content_types", expectedContentTypes, "actual", contentType));
	}

}
