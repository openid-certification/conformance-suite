package io.fintechlabs.testframework.condition.client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public abstract class ValidateHash extends AbstractCondition {
	
	protected String HashName;
	protected String EnvName = null;
	
	public ValidateHash(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	public Environment evaluate(Environment env) {
		
		JsonObject hashJson = env.get(EnvName);
		if (hashJson == null) {
			throw error("Couldn't find " + HashName);
		}

		JsonElement algElement= hashJson.get("alg");
		if (algElement == null) {
			throw error("Could not find alg field."); 
		}

		JsonElement hashElement = hashJson.get(HashName);
		if (hashElement == null) {
			throw error("Could not find " + HashName + " field.");
		}

		String alg = algElement.getAsString();
		String hash = hashElement.getAsString();

		
		String state = env.getString("state");
		if (state == null) {
			throw error("Couldn't find state");
		}
		
		MessageDigest digester;

		try {
			Matcher matcher = Pattern.compile("^(HS|RS|ES|PS)(256|384|512)$").matcher(alg);
			if (!matcher.matches()) {
				throw error("Invalid algorithm", args("alg", alg));
			}

			String digestAlgorithm = "SHA-" + matcher.group(2);
			digester = MessageDigest.getInstance(digestAlgorithm);
		} catch (NoSuchAlgorithmException e) {
			throw error("Unsupported digest for algorithm", e, args("alg", alg));
		}

		byte[] stateDigest = digester.digest(state.getBytes(StandardCharsets.US_ASCII));

		byte[] halfDigest = new byte[stateDigest.length / 2];
		System.arraycopy(stateDigest, 0, halfDigest, 0, halfDigest.length);

		String expectedHash = Base64URL.encode(halfDigest).toString();
		if (!hash.equals(expectedHash)) {
			throw error("Invalid " + HashName + " in token", args("expected", expectedHash, "actual", hash));
		}

		logSuccess("State hash validated successfully", args(HashName, hash));

		return env;
	}

}
