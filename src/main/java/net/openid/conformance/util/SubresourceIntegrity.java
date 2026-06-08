package net.openid.conformance.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * W3C Subresource Integrity (SRI) verification helper, referenced normatively
 * by IETF SD-JWT VC §7. Implements the algorithm in W3C SRI §3.3.4
 * ("Do bytes match metadataList?"): selects the strongest algorithm present in
 * the metadata, then returns true if any entry at that tier hashes to the
 * expected value.
 *
 * Conformance-suite parsing deviates from strict W3C SRI §3.3.2 step 6
 * (which silently skips tokens whose algorithm is not understood). Because
 * this helper is used to test issuer conformance, an unrecognized hash
 * algorithm in an issuer's integrity-metadata string is treated as a hard
 * failure — it is almost always a typo or use of an unblessed algorithm,
 * not legitimate forward compatibility. The hash value uses standard
 * base64 per the W3C SRI grammar (RFC 4648 §4 alphabet); base64url
 * characters ({@code -}, {@code _}) are rejected. Padding is optional,
 * mirroring common decoder tolerance.
 */
public final class SubresourceIntegrity {

	private SubresourceIntegrity() {}

	public enum Algorithm {
		SHA256("sha256", "SHA-256", 1),
		SHA384("sha384", "SHA-384", 2),
		SHA512("sha512", "SHA-512", 3);

		private final String token;
		private final String jceName;
		private final int strength;

		Algorithm(String token, String jceName, int strength) {
			this.token = token;
			this.jceName = jceName;
			this.strength = strength;
		}

		public String getToken() {
			return token;
		}

		public static Algorithm fromToken(String token) {
			for (Algorithm a : values()) {
				if (a.token.equals(token)) {
					return a;
				}
			}
			return null;
		}
	}

	public static final class Entry {
		private final Algorithm algorithm;
		private final byte[] expectedHash;

		public Entry(Algorithm algorithm, byte[] expectedHash) {
			this.algorithm = algorithm;
			this.expectedHash = expectedHash.clone();
		}

		public Algorithm algorithm() {
			return algorithm;
		}

		public byte[] expectedHash() {
			return expectedHash.clone();
		}
	}

	/**
	 * Parses an SRI integrity-metadata string. Deviates from W3C SRI §3.3.2
	 * step 6 — unrecognized hash algorithms cause this method to throw
	 * rather than silently skip, on the basis that this helper is used in a
	 * conformance-test context and an issuer using an algorithm we don't
	 * understand is a bug worth surfacing loudly.
	 * @throws IllegalArgumentException if a token is grammatically malformed,
	 *         uses an unsupported algorithm, has an empty hash value, or the
	 *         hash value contains characters outside the standard base64
	 *         alphabet.
	 */
	public static List<Entry> parse(String integrityMetadata) {
		if (integrityMetadata == null) {
			throw new IllegalArgumentException("integrity metadata is null");
		}
		String trimmed = integrityMetadata.trim();
		if (trimmed.isEmpty()) {
			return List.of();
		}
		List<Entry> entries = new ArrayList<>();
		for (String token : trimmed.split("\\s+")) {
			int q = token.indexOf('?');
			String hashExpression = q >= 0 ? token.substring(0, q) : token;
			int dash = hashExpression.indexOf('-');
			if (dash < 0) {
				throw new IllegalArgumentException("integrity entry missing algorithm separator '-': " + token);
			}
			String algToken = hashExpression.substring(0, dash);
			String base64Hash = hashExpression.substring(dash + 1);
			Algorithm algorithm = Algorithm.fromToken(algToken);
			if (algorithm == null) {
				throw new IllegalArgumentException("integrity entry uses unsupported algorithm '" + algToken
					+ "', expected one of sha256, sha384, sha512");
			}
			if (base64Hash.isEmpty()) {
				throw new IllegalArgumentException("integrity entry has empty hash value: " + token);
			}
			byte[] hashBytes;
			try {
				hashBytes = decodeStandardBase64(base64Hash);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("integrity entry has invalid base64 hash value '"
					+ base64Hash + "': " + e.getMessage());
			}
			entries.add(new Entry(algorithm, hashBytes));
		}
		return List.copyOf(entries);
	}

	/**
	 * Returns the subset of entries with the strongest algorithm present, per
	 * W3C SRI §3.3.3.
	 */
	public static List<Entry> strongest(List<Entry> entries) {
		if (entries.isEmpty()) {
			return List.of();
		}
		Algorithm best = entries.get(0).algorithm;
		for (Entry e : entries) {
			if (e.algorithm.strength > best.strength) {
				best = e.algorithm;
			}
		}
		List<Entry> result = new ArrayList<>();
		for (Entry e : entries) {
			if (e.algorithm == best) {
				result.add(e);
			}
		}
		return List.copyOf(result);
	}

	/**
	 * Verifies the given bytes against the integrity metadata per W3C SRI
	 * §3.3.4. Returns true if the metadata is empty (§3.3.4 step 2) or if any
	 * entry at the strongest algorithm tier matches.
	 * @throws IllegalArgumentException if the metadata string is malformed.
	 */
	public static boolean verify(byte[] bytes, String integrityMetadata) {
		List<Entry> entries = parse(integrityMetadata);
		if (entries.isEmpty()) {
			return true;
		}
		for (Entry entry : strongest(entries)) {
			byte[] actualHash = computeHash(bytes, entry.algorithm);
			if (MessageDigest.isEqual(actualHash, entry.expectedHash)) {
				return true;
			}
		}
		return false;
	}

	public static byte[] computeHash(byte[] bytes, Algorithm algorithm) {
		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm.jceName);
			return digest.digest(bytes);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("JCE provider missing " + algorithm.jceName, e);
		}
	}

	private static byte[] decodeStandardBase64(String value) {
		// Strict W3C SRI alphabet: RFC 4648 §4 (A-Z, a-z, 0-9, +, /). Padding
		// is optional in real-world producers, so we strip a trailing run of '='
		// and re-pad to a multiple of 4 before decoding. '=' may only appear as
		// that trailing run: any '=' elsewhere (padding in the middle, or
		// trailing garbage such as "QQ==junk") is rejected rather than silently
		// truncated. Base64url characters ('-', '_') are not part of the standard
		// alphabet and will cause Base64.getDecoder().decode() to throw
		// IllegalArgumentException.
		int end = value.length();
		while (end > 0 && value.charAt(end - 1) == '=') {
			end--;
		}
		String body = value.substring(0, end);
		if (body.indexOf('=') >= 0) {
			throw new IllegalArgumentException("'=' may only appear as trailing padding");
		}
		int padding = (4 - (body.length() % 4)) % 4;
		String padded = body + "=".repeat(padding);
		return Base64.getDecoder().decode(padded);
	}
}
