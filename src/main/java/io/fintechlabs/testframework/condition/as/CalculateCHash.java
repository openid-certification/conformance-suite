package io.fintechlabs.testframework.condition.as;

import com.nimbusds.jose.util.Base64URL;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalculateCHash extends AbstractCondition {

	public CalculateCHash(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String[] requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(strings = {"signing_algorithm", "authorization_code"})
	@PostEnvironment(strings = "c_hash")
	public Environment evaluate(Environment env) {

		String algorithm = env.getString("signing_algorithm");
		String code = env.getString("authorization_code");

		MessageDigest digester;

		try {
			Matcher matcher = Pattern.compile("^(HS|RS|ES|PS)(256|384|512)$").matcher(algorithm);
			if (!matcher.matches()) {
				throw error("Invalid algorithm", args("algorithm", algorithm));
			}

			String digestAlgorithm = "SHA-" + matcher.group(2);
			digester = MessageDigest.getInstance(digestAlgorithm);

		} catch (NoSuchAlgorithmException e) {
			throw error("Unsupported digest for algorithm", e, args("alg", algorithm));
		}

		byte[] stateDigest = digester.digest(code.getBytes(StandardCharsets.US_ASCII));

		byte[] halfDigest = new byte[stateDigest.length / 2];
		System.arraycopy(stateDigest, 0, halfDigest, 0, halfDigest.length);

		String hashValue = Base64URL.encode(halfDigest).toString();

		env.putString("c_hash", hashValue);

		logSuccess("Successful c_hash encoding", args("c_hash", hashValue));

		return env;
	}

}
