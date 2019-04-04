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

	public ValidateHash(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	public Environment validateHash(Environment env, String hashName, String envName ) {

		JsonObject hashJson = env.getObject(envName);
		if (hashJson == null) {
			throw error("Couldn't find " + hashName);
		}

		JsonElement algElement= hashJson.get("alg");
		if (algElement == null) {
			throw error("Could not find alg field.");
		}

		JsonElement hashElement = hashJson.get(hashName);
		if (hashElement == null) {
			throw error("Could not find " + hashName + " field.");
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
			throw error(hashName + " element is null or empty. Invalid");
		}

		String baseString = getBaseStringBasedOnType(env, hashName);

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
			throw error("Invalid " + hashName + " in token", args("expected_hash", expectedHash, "id_token_hash", hash, "unhashed_value", baseString));
		}

		logSuccess("State hash validated successfully", args(hashName, hash));

		return env;
	}

	protected String getBaseStringBasedOnType(Environment env, String hashName) {

		String baseString = null;

		switch (hashName) {
			case "s_hash":
				baseString = env.getString("state");
				if (baseString == null) {
					throw error("Couldn't find state");
				}
				break;
			case "at_hash":
				JsonObject accessToken = env.getObject("access_token");
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
				throw error("Invalid HashName(" + hashName + ")");
		}

		return baseString;
	}

}
