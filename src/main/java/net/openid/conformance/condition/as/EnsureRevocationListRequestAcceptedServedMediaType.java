package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

import java.util.List;

/**
 * Checks that an Accept header the verifier sent when fetching the revocation list actually
 * accepts the media type the test served. draft-ietf-oauth-status-list section 8.1 uses HTTP
 * content negotiation and defines the status list media types for it; a verifier that asks for
 * a type it is not served has either misconfigured the format or misread the reference, and
 * would reject a list a conformant Status Provider returned.
 *
 * <p>An absent Accept header is not flagged, on the basis of draft-21 of the specification.
 * Draft-15 section 8.1 says the Relying Party SHOULD send one unless the content type is known
 * in the ecosystem or it supports both formats; draft-21 dropped that sentence and only defines
 * the media types for content negotiation, so there is nothing stricter to test.
 */
public class EnsureRevocationListRequestAcceptedServedMediaType extends AbstractCondition {

	@Override
	@PreEnvironment(required = { ServedRevocationList.ENV_KEY, "incoming_request" })
	public Environment evaluate(Environment env) {

		String accept = env.getString("incoming_request", "headers.accept");
		String served = env.getString(ServedRevocationList.ENV_KEY, "content_type");

		if (accept == null || accept.isBlank()) {
			logSuccess("The verifier sent no Accept header when it fetched the revocation list, which draft-21 of the Token Status List specification does not require",
				args("served_content_type", served));
			return env;
		}

		List<MediaType> ranges;
		try {
			ranges = MediaType.parseMediaTypes(accept);
		} catch (InvalidMediaTypeException e) {
			throw error("The Accept header the verifier sent when it fetched the revocation list is not a valid list of media ranges", e,
				args("accept", accept, "served_content_type", served));
		}

		// a range with a q weight of 0 refuses the types it covers
		MediaType servedType = MediaType.parseMediaType(served);
		boolean accepted = ranges.stream()
			.anyMatch(range -> range.getQualityValue() > 0 && range.includes(servedType));

		if (!accepted) {
			throw error("The Accept header the verifier sent when it fetched the revocation list does not accept the media type the list is served as",
				args("accept", accept, "served_content_type", served));
		}

		logSuccess("The Accept header the verifier sent accepts the media type the revocation list is served as",
			args("accept", accept, "served_content_type", served));
		return env;
	}
}
