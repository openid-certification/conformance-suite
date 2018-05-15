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
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

import com.google.common.base.Strings;

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

		
		String alg = null;
		String hash = null; 
		
		if (algElement.isJsonPrimitive()) {
			alg = algElement.getAsString();
		}
		
		if (hashElement.isJsonPrimitive()) {
			hash = hashElement.getAsString();
		}

		if (Strings.isNullOrEmpty(alg)) {
			throw error("Alg is null or empty. Invalid");
		}
		
		if (Strings.isNullOrEmpty(hash)) {
			throw error(HashName + " element is null or empty. Invalid");
		}
		
		String baseString = getBaseStringBasedOnType(env);

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

		byte[] stateDigest = digester.digest(baseString.getBytes(StandardCharsets.US_ASCII));

		byte[] halfDigest = new byte[stateDigest.length / 2];
		System.arraycopy(stateDigest, 0, halfDigest, 0, halfDigest.length);

		String expectedHash = Base64URL.encode(halfDigest).toString();
		if (!hash.equals(expectedHash)) {
			throw error("Invalid " + HashName + " in token", args("expected", expectedHash, "actual", hash));
		}

		logSuccess("State hash validated successfully", args(HashName, hash));

		return env;
	}
	
	private String getBaseStringBasedOnType(Environment env) {

		String baseString = null;
		
		switch (HashName) {
			case "s_hash":
				baseString = env.getString("state");
				if (baseString == null) {
					throw error("Couldn't find state");
				}
				break;
			case "at_hash":
				JsonObject accessToken = env.get("access_token"); 
				if (accessToken == null) {
					throw error("Could not get access_token object..."); 
				}
				baseString = accessToken.get("value").getAsString();
				break;
			case "c_hash":
				baseString = env.getString("callback_params", "code");
				if (baseString == null) {
					throw error("Could not find callback_params.code");
				}
				break;
			default:
				throw error("Invalid HashName(" + HashName + ")");
		}

		return baseString;
	}

}
