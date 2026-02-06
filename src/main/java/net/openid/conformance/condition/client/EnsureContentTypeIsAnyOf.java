package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;
import java.util.TreeSet;

public class EnsureContentTypeIsAnyOf extends AbstractCheckEndpointContentTypeReturned {

	private final Set<String> expectedContentTypes;

	public EnsureContentTypeIsAnyOf(String firstContentType, String... additionalContentTypes) {
		var set = new TreeSet<String>();
		set.add(firstContentType);
		if (additionalContentTypes != null) {
			set.addAll(Set.of(additionalContentTypes));
		}
		expectedContentTypes = Set.copyOf(set);
	}

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		String headersEnvKey = "endpoint_response";
		String contentType = getContentType(env, headersEnvKey, "headers.");

		String mimeType = getMimeTypeFromContentType(contentType);

		if (expectedContentTypes.contains(mimeType)) {
			logSuccess(headersEnvKey + " Content-Type: header is one of " + expectedContentTypes, args("actual", contentType));
			return env;
		}

		throw error("Invalid content-type header in "+headersEnvKey, args("allowed_content_types", expectedContentTypes, "actual", contentType));
	}

}
