package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWAUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class AbstractValidateHash extends AbstractCondition {

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
			alg = OIDFJSON.getString(algElement);
		}

		if (hashElement.isJsonPrimitive()) {
			hash = OIDFJSON.getString(hashElement);
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
			String digestAlgorithm = JWAUtil.getDigestAlgorithmForSigAlg(alg);
			digester = MessageDigest.getInstance(digestAlgorithm);
		} catch (NoSuchAlgorithmException e) {
			throw error("Unsupported digest for algorithm", e, args("alg", alg));
		} catch (JWAUtil.InvalidAlgorithmException e) {
			throw error("Invalid algorithm", args("alg", alg));
		}

		byte[] stateDigest = digester.digest(baseString.getBytes(StandardCharsets.US_ASCII));

		byte[] halfDigest = new byte[stateDigest.length / 2];
		System.arraycopy(stateDigest, 0, halfDigest, 0, halfDigest.length);

		String expectedHash = Base64URL.encode(halfDigest).toString();
		if (!hash.equals(expectedHash)) {
			throw error("Invalid " + hashName + " in token", args("expected_hash", expectedHash, "id_token_hash", hash, "unhashed_value", baseString));
		}

		logSuccess(hashName + " validated successfully", args("expected_hash", expectedHash, "id_token_hash", hash, "unhashed_value", baseString));

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
				baseString = OIDFJSON.getString(accessToken.get("value"));
				break;
			case "c_hash":
				baseString = env.getString("authorization_endpoint_response", "code");
				if (baseString == null) {
					throw error("Could not find authorization_endpoint_response.code");
				}
				break;
			default:
				throw error("Invalid HashName(" + hashName + ")");
		}

		return baseString;
	}

}
