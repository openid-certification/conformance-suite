package net.openid.conformance.condition.as;

import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWAUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CalculateAtHash extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "signing_algorithm", "access_token" })
	@PostEnvironment(strings = "at_hash")
	public Environment evaluate(Environment env) {

		String algorithm = env.getString("signing_algorithm");

		String accessToken = env.getString("access_token");

		MessageDigest digester;

		try {
			String digestAlgorithm = JWAUtil.getDigestAlgorithmForSigAlg(algorithm);
			digester = MessageDigest.getInstance(digestAlgorithm);
		} catch (NoSuchAlgorithmException e) {
			throw error("Unsupported digest for algorithm", e, args("alg", algorithm));
		} catch (JWAUtil.InvalidAlgorithmException e) {
			throw error("Unsupported algorithm", e, args("alg", algorithm));
		}

		byte[] stateDigest = digester.digest(accessToken.getBytes(StandardCharsets.US_ASCII));

		byte[] halfDigest = new byte[stateDigest.length / 2];
		System.arraycopy(stateDigest, 0, halfDigest, 0, halfDigest.length);

		String hashValue = Base64URL.encode(halfDigest).toString();

		env.putString("at_hash", hashValue);

		logSuccess("Successful at_hash encoding", args("at_hash", hashValue));

		return env;
	}

}
