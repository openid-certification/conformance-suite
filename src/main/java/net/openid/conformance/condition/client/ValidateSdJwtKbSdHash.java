package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ValidateSdJwtKbSdHash extends AbstractCondition {

	@Override
	@PreEnvironment(required = "sdjwt", strings = "vp_token")
	public Environment evaluate(Environment env) {
		String sdJwtStr = env.getString("vp_token");

		int lastIndexOf = sdJwtStr.lastIndexOf("~");
		if(lastIndexOf < 0) {
			throw error("SD-JWT has no ~ in it", args("sdjwt", sdJwtStr));
		}

		String sdAlg = env.getString("sdjwt", "_sd_alg");
		if (sdAlg != null) {
			// null means the default, sha-256
			if (!sdAlg.equals("sha-256")) {
				throw error("_sd_alg value not currently supported", args("_sd_alg", sdAlg));
			}
		}

		String toHash = sdJwtStr.substring(0, lastIndexOf+1);

		String calculatedSdHash = null;
		try {
			calculatedSdHash = getCalculatedSdHash(toHash);
		} catch (NoSuchAlgorithmException e) {
			throw error("Unable to find SHA-256 algorithm", e);
		}

		String kbJwtSdHash = env.getString("sdjwt", "binding.claims.sd_hash");
		if (kbJwtSdHash == null) {
			throw error("Key binding jwt does not contain required 'sd_hash'",
				args("kbjwt", env.getElementFromObject("sdjwt", "binding")));
		}

		if (!calculatedSdHash.equals(kbJwtSdHash)) {
			throw error("sd_hash in the kb-jwt does not match the one calculated from the presented SD-JWT",
				args("calculatedSdHash", calculatedSdHash, "kbJwtSdHash", kbJwtSdHash,
					"hashalg", "sha-256", "tohash", toHash));
		}

		logSuccess("sd_hash in the kb-jwt does matches the one calculated from the presented SD-JWT",
			args("calculatedSdHash", calculatedSdHash, "kbJwtSdHash", kbJwtSdHash,
				"hashalg", "sha-256", "tohash", toHash));

		return env;
	}

	public static String getCalculatedSdHash(String toHash) throws NoSuchAlgorithmException {
		byte[] bytes = toHash.getBytes(StandardCharsets.US_ASCII);
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(bytes, 0, bytes.length);
		byte[] digest = md.digest();
		return Base64.encodeBase64URLSafeString(digest);
	}

}
