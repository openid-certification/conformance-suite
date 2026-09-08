package net.openid.conformance.openid.ssf.conditions.events;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfConstants;
import net.openid.conformance.testmodule.Environment;

import java.util.Locale;

/**
 * RFC 8935 2.1: "The Content-Type header field of this request MUST be
 * application/secevent+jwt as defined in Sections 2.2 and 2.3 of [RFC8417]".
 * Media type parameters (e.g. a charset) are tolerated; the media type itself must match.
 */
public class OIDSSFEnsurePushRequestContentTypeIsSecEventJwt extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		String contentType = env.getString("ssf", "push_request.headers.content-type");
		if (contentType == null) {
			throw error("Push delivery request has no Content-Type header. RFC 8935 requires 'application/secevent+jwt'.",
				args("headers", env.getElementFromObject("ssf", "push_request.headers")));
		}

		String mediaType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
		if (!SsfConstants.SECURITY_EVENT_TOKEN_CONTENT_TYPE.equals(mediaType)) {
			throw error("Push delivery request Content-Type is not 'application/secevent+jwt'",
				args("content_type", contentType));
		}

		logSuccess("Push delivery request Content-Type is 'application/secevent+jwt'", args("content_type", contentType));

		return env;
	}
}
