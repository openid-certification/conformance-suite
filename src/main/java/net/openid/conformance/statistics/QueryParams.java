package net.openid.conformance.statistics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reading the filter parameters the statistics page sends. Both endpoints it drives -
 * {@code GET /api/statistics/overview} and the drill-down listing {@code GET /api/plan} -
 * are given the same parameters by the same page, so they read them the same way here
 * rather than each in their own slightly different manner.
 *
 * <p>A value that is blank counts as absent, so a client can send an empty parameter for
 * "no filter" instead of having to leave it out. Anything that cannot be used at all is an
 * {@link IllegalArgumentException} whose message names the parameter and is meant to be
 * shown to whoever made the request.
 */
public final class QueryParams {

	/** The prefix a plan level variant filter is sent under, e.g. {@code variant.fapi_profile}. */
	public static final String VARIANT_PREFIX = "variant.";

	/**
	 * A variant parameter name is a variant parameter of some test plan, and is used as part
	 * of a Mongo field path by the plan listing, so it is held to what such a name looks like.
	 */
	private static final Pattern VARIANT_NAME = Pattern.compile("[A-Za-z0-9_-]+");

	private QueryParams() {}

	/**
	 * @param params the request parameters, as {@code HttpServletRequest.getParameterMap()}
	 *               returns them
	 * @param name   the parameter to read
	 * @return its first non blank value, trimmed, or null if there is none
	 */
	public static String first(Map<String, String[]> params, String name) {
		return value(params.get(name));
	}

	/**
	 * @param params the request parameters
	 * @return every {@code variant.<name>=<value>} parameter, in the order they were sent
	 * @throws IllegalArgumentException if one of them is not named after a variant parameter
	 */
	public static Map<String, String> variant(Map<String, String[]> params) {
		Map<String, String> variant = new LinkedHashMap<>();
		for (Map.Entry<String, String[]> parameter : params.entrySet()) {
			if (!parameter.getKey().startsWith(VARIANT_PREFIX)) {
				continue;
			}
			String name = parameter.getKey().substring(VARIANT_PREFIX.length()).trim();
			if (name.isEmpty()) {
				throw new IllegalArgumentException("'" + VARIANT_PREFIX
					+ "' must be followed by a variant parameter name, e.g. variant.client_auth_type=mtls");
			}
			if (!VARIANT_NAME.matcher(name).matches()) {
				throw new IllegalArgumentException("'" + VARIANT_PREFIX + name
					+ "' is not a variant parameter name; only letters, digits, '_' and '-' can be used");
			}
			String value = value(parameter.getValue());
			if (value != null) {
				variant.put(name, value);
			}
		}
		return variant;
	}

	/** @return the first value, trimmed, or null if there is none or it is blank */
	private static String value(String... values) {
		if (values == null || values.length == 0 || values[0] == null) {
			return null;
		}
		String value = values[0].trim();
		return value.isEmpty() ? null : value;
	}
}
