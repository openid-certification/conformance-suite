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

public class CalculateSHash extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "signing_algorithm", required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	@PostEnvironment(strings = "s_hash")
	public Environment evaluate(Environment env) {

		String algorithm = env.getString("signing_algorithm");
		String state = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE);


		MessageDigest digester;

		try {
			String digestAlgorithm = JWAUtil.getDigestAlgorithmForSigAlg(algorithm);
			digester = MessageDigest.getInstance(digestAlgorithm);
		} catch (NoSuchAlgorithmException e) {
			throw error("Unsupported digest for algorithm", e, args("alg", algorithm));
		} catch (JWAUtil.InvalidAlgorithmException e) {
			throw error("Unsupported algorithm", e, args("alg", algorithm));
		}

		byte[] stateDigest = digester.digest(state.getBytes(StandardCharsets.US_ASCII));

		byte[] halfDigest = new byte[stateDigest.length / 2];
		System.arraycopy(stateDigest, 0, halfDigest, 0, halfDigest.length);

		String hashValue = Base64URL.encode(halfDigest).toString();

		env.putString("s_hash", hashValue);

		logSuccess("Successful s_hash encoding", args("s_hash", hashValue));

		return env;
	}

}
