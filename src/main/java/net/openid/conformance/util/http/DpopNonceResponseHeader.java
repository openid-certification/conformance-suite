package net.openid.conformance.util.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.util.RFC6749AppendixASyntaxUtils;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * The {@code DPoP-Nonce} response header supplied by an authorization or resource server, checked
 * against RFC 9449.
 *
 * <p>{@link #from(JsonObject)} does not throw: the caller gets the nonce, a description of the way the
 * header breaks the RFC, or neither when the server did not send the header at all, so that the calling
 * condition is the one that raises the error and gets it attributed to itself.
 */
public record DpopNonceResponseHeader(String nonce, String violation) {

	/** Response headers are stored in the environment with lowercased names. */
	public static final String HEADER_NAME = "dpop-nonce";

	private static final DpopNonceResponseHeader ABSENT = new DpopNonceResponseHeader(null, null);

	/**
	 * @param responseHeaders the response headers as stored in the environment - lowercased names, with a
	 *                        header the server sent more than once stored as a JSON array
	 */
	public static DpopNonceResponseHeader from(JsonObject responseHeaders) {
		if (responseHeaders == null) {
			return ABSENT;
		}

		JsonElement header = responseHeaders.get(HEADER_NAME);
		if (header == null) {
			return ABSENT;
		}

		if (header.isJsonArray()) {
			return violation("The response contains " + header.getAsJsonArray().size()
				+ " DPoP-Nonce headers, but RFC9449 section 8 says there MUST NOT be more than one"
				+ " DPoP-Nonce header.");
		}

		if (!OIDFJSON.isString(header)) {
			return violation("The DPoP-Nonce response header could not be read as a string.");
		}

		String value = OIDFJSON.getString(header);

		if (value.isEmpty()) {
			return violation("The DPoP-Nonce response header is empty, but RFC9449 section 8.1 defines"
				+ " the nonce as '1*NQCHAR', which requires at least one character.");
		}

		if (!RFC6749AppendixASyntaxUtils.isNQCharSequence(value)) {
			return violation("The DPoP-Nonce response header contains characters that are not allowed."
				+ " RFC9449 section 8.1 defines the nonce as '1*NQCHAR', so only the characters"
				+ " %x21 / %x23-5B / %x5D-7E may be used.");
		}

		return new DpopNonceResponseHeader(value, null);
	}

	private static DpopNonceResponseHeader violation(String description) {
		return new DpopNonceResponseHeader(null, description);
	}
}
