package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that an Accept header the verifier sent when fetching the revocation list actually
 * accepts the media type the test served. draft-ietf-oauth-status-list section 8.1 uses HTTP
 * content negotiation and defines the status list media types for it; a verifier that asks for
 * a type it is not served has either misconfigured the format or misread the reference, and
 * would reject a list a conformant Status Provider returned. An absent Accept header is not
 * flagged: the specification requires none.
 */
public class EnsureRevocationListRequestAcceptedServedMediaType extends AbstractCondition {

	@Override
	@PreEnvironment(strings = VP1FinalRevocationListRequest.SERVED_CONTENT_TYPE_ENV_KEY,
		required = VP1FinalRevocationListRequest.ENV_KEY)
	public Environment evaluate(Environment env) {

		String accept = env.getString(VP1FinalRevocationListRequest.ENV_KEY, "headers.accept");
		String served = env.getString(VP1FinalRevocationListRequest.SERVED_CONTENT_TYPE_ENV_KEY);

		if (accept == null || accept.isBlank()) {
			logSuccess("The verifier sent no Accept header when it fetched the revocation list, which the Token Status List specification does not require",
				args("served_content_type", served));
			return env;
		}

		boolean accepted = false;
		for (String range : accept.split(",")) {
			// drop any parameters, e.g. the q weight
			String mediaType = range.split(";")[0].trim();
			if (mediaType.equals(served) || "*/*".equals(mediaType)
				|| mediaType.equals(served.split("/")[0] + "/*")) {
				accepted = true;
				break;
			}
		}

		if (!accepted) {
			throw error("The Accept header the verifier sent when it fetched the revocation list does not accept the media type the list is served as",
				args("accept", accept, "served_content_type", served));
		}

		logSuccess("The Accept header the verifier sent accepts the media type the revocation list is served as",
			args("accept", accept, "served_content_type", served));
		return env;
	}
}
