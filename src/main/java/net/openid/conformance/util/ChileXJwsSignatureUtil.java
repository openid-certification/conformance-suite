package net.openid.conformance.util;

import com.nimbusds.jose.util.Base64URL;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helpers shared by the Chile Open Finance x-jws-signature conditions (CMF-XJWS-4).
 */
public final class ChileXJwsSignatureUtil {

	public static final String X_JWS_SIGNATURE = "x-jws-signature";

	private static final Comparator<String> BY_CODE_POINT =
		(a, b) -> Arrays.compare(a.codePoints().toArray(), b.codePoints().toArray());

	private ChileXJwsSignatureUtil() {
	}

	/** S256(x): the string "S256:" followed by BASE64URL(SHA-256(x)), without padding. */
	public static String s256(byte[] bytes) {
		try {
			return "S256:" + Base64URL.encode(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}

	/**
	 * Decodes a raw query as application/x-www-form-urlencoded (name, value) pairs, keeping blank
	 * values; a name without '=' has the empty value.
	 *
	 * @throws IllegalArgumentException when a percent escape is malformed
	 */
	public static List<Map.Entry<String, String>> decodeFormPairs(String rawQuery) {
		List<Map.Entry<String, String>> pairs = new ArrayList<>();
		if (rawQuery == null) {
			return pairs;
		}
		for (String part : rawQuery.split("&")) {
			if (part.isEmpty()) {
				continue;
			}
			int eq = part.indexOf('=');
			String name = eq < 0 ? part : part.substring(0, eq);
			String value = eq < 0 ? "" : part.substring(eq + 1);
			pairs.add(Map.entry(URLDecoder.decode(name, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8)));
		}
		return pairs;
	}

	/**
	 * The canonical query: the decoded pairs sorted by name then value in Unicode code-point order,
	 * joined as name=value with '&amp;' without re-encoding; null when there are no pairs.
	 */
	public static String canonicalQuery(List<Map.Entry<String, String>> pairs) {
		if (pairs.isEmpty()) {
			return null;
		}
		return pairs.stream()
			.sorted(Map.Entry.<String, String>comparingByKey(BY_CODE_POINT).thenComparing(Map.Entry.comparingByValue(BY_CODE_POINT)))
			.map(p -> p.getKey() + "=" + p.getValue())
			.collect(Collectors.joining("&"));
	}
}
