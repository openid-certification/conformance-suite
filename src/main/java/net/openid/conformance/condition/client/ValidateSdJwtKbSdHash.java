package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class ValidateSdJwtKbSdHash extends AbstractCondition {

	public static final String DEFAULT_SD_ALG = "sha-256";

	// 'Hash Name String' values from the IANA Named Information Hash Algorithm Registry, as used
	// by _sd_alg (RFC 9901 section 4.1.1), mapped to the JCA algorithm name. The names are
	// case-sensitive. The registry's truncated sha-256 variants and k12 are absent because no JCA
	// provider the suite loads implements them; the blake2 algorithms come from BouncyCastle.
	// Everything that hashes with a credential's _sd_alg goes through this map, because the SD-JWT
	// library passes the claim value to JCA as is and so takes any name JCA does (SHA-256, md5).
	private static final Map<String, String> JCA_ALGORITHM_BY_SD_ALG = Map.of(
		"sha-256", "SHA-256",
		"sha-384", "SHA-384",
		"sha-512", "SHA-512",
		"sha3-224", "SHA3-224",
		"sha3-256", "SHA3-256",
		"sha3-384", "SHA3-384",
		"sha3-512", "SHA3-512",
		"blake2s-256", "BLAKE2S-256",
		"blake2b-256", "BLAKE2B-256",
		"blake2b-512", "BLAKE2B-512");

	@Override
	@PreEnvironment(required = "sdjwt", strings = "credential")
	public Environment evaluate(Environment env) {
		String sdJwtStr = env.getString("credential");

		int lastIndexOf = sdJwtStr.lastIndexOf("~");
		if(lastIndexOf < 0) {
			throw error("SD-JWT has no ~ in it", args("sdjwt", sdJwtStr));
		}

		// RFC 9901 section 4.3.1: the sd_hash uses the same hash algorithm as the disclosure digests
		String sdAlg = getSdAlg(env);

		String toHash = sdJwtStr.substring(0, lastIndexOf+1);

		String calculatedSdHash = null;
		try {
			calculatedSdHash = calculateDigest(toHash, sdAlg);
		} catch (NoSuchAlgorithmException e) {
			throw error("The credential's _sd_alg claim is not a hash algorithm the conformance suite supports, so sd_hash cannot be checked",
				e, args("_sd_alg", sdAlg, "supported", supportedSdAlgs()));
		}

		String kbJwtSdHash = env.getString("sdjwt", "binding.claims.sd_hash");
		if (kbJwtSdHash == null) {
			throw error("Key binding jwt does not contain required 'sd_hash'",
				args("kbjwt", env.getElementFromObject("sdjwt", "binding")));
		}

		if (!calculatedSdHash.equals(kbJwtSdHash)) {
			throw error("sd_hash in the kb-jwt does not match the one calculated from the presented SD-JWT",
				args("calculatedSdHash", calculatedSdHash, "kbJwtSdHash", kbJwtSdHash,
					"hashalg", sdAlg, "tohash", toHash));
		}

		logSuccess("sd_hash in the kb-jwt does matches the one calculated from the presented SD-JWT",
			args("calculatedSdHash", calculatedSdHash, "kbJwtSdHash", kbJwtSdHash,
				"hashalg", sdAlg, "tohash", toHash));

		return env;
	}

	/**
	 * The hash algorithm the issuer used for the disclosure digests of the parsed SD-JWT, taken from
	 * the _sd_alg claim of the issuer-signed JWT, or sha-256 when the claim is absent (RFC 9901
	 * section 4.1.1). The same algorithm applies to the key binding JWT's sd_hash.
	 */
	public static String getSdAlg(Environment env) {
		JsonElement sdAlg = env.getElementFromObject("sdjwt", "credential.claims._sd_alg");
		if (sdAlg != null && OIDFJSON.isString(sdAlg)) {
			return OIDFJSON.getString(sdAlg);
		}
		return DEFAULT_SD_ALG;
	}

	/** Whether sdAlg is an _sd_alg value the suite can hash with. */
	public static boolean isSupportedSdAlg(String sdAlg) {
		return JCA_ALGORITHM_BY_SD_ALG.containsKey(sdAlg);
	}

	/** The _sd_alg values the suite can hash with, for error messages. */
	public static SortedSet<String> supportedSdAlgs() {
		return new TreeSet<>(JCA_ALGORITHM_BY_SD_ALG.keySet());
	}

	/** The sd_hash of an SD-JWT using sha-256, for credentials the suite itself issues. */
	public static String getCalculatedSdHash(String toHash) throws NoSuchAlgorithmException {
		return calculateDigest(toHash, DEFAULT_SD_ALG);
	}

	/**
	 * The base64url-encoded digest of the US-ASCII bytes of a string, which is how RFC 9901 defines
	 * both a disclosure's digest (section 4.2.3, over the base64url-encoded disclosure) and sd_hash
	 * (section 4.3.1, over the SD-JWT up to and including its last '~').
	 *
	 * @param sdAlg the _sd_alg value of the credential
	 * @throws NoSuchAlgorithmException if the suite cannot map sdAlg to a hash algorithm
	 */
	public static String calculateDigest(String toHash, String sdAlg) throws NoSuchAlgorithmException {
		String jcaAlgorithm = JCA_ALGORITHM_BY_SD_ALG.get(sdAlg);
		if (jcaAlgorithm == null) {
			throw new NoSuchAlgorithmException("Unsupported _sd_alg: " + sdAlg);
		}
		byte[] bytes = toHash.getBytes(StandardCharsets.US_ASCII);
		MessageDigest md = MessageDigest.getInstance(jcaAlgorithm);
		md.update(bytes, 0, bytes.length);
		byte[] digest = md.digest();
		return Base64.encodeBase64URLSafeString(digest);
	}

}
