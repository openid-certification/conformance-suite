package net.openid.conformance.condition.as;

/**
 * The environment key under which the VP1 Final verifier tests record the request the verifier
 * made to the revocation list endpoint they serve. The list handlers run without the test lock,
 * so the request is only recorded there; the conditions that check it run when the test finishes.
 */
public final class VP1FinalRevocationListRequest {

	/** Holds the request parts (method, headers, query_string_params) of the fetch. */
	public static final String ENV_KEY = "revocation_list_request";

	/** The media type the test served, so the Accept header can be checked against it. */
	public static final String SERVED_CONTENT_TYPE_ENV_KEY = "revocation_list_served_content_type";

	private VP1FinalRevocationListRequest() {
	}
}
