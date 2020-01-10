package net.openid.conformance.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;

import java.text.ParseException;

public class JWKUtil {
	public static JWKSet parseJWKSet(String jwksString) throws ParseException {
		JWKSet jwkSet = JWKSet.parse(jwksString);
		return jwkSet;
	}

	public static JsonObject getPublicJwksAsJsonObject(JWKSet jwks) {
		JsonObject publicJwks = new JsonParser().parse(jwks.toJSONObject(true).toJSONString()).getAsJsonObject();
		return publicJwks;
	}

	public static JsonObject getPrivateJwksAsJsonObject(JWKSet jwks) {
		JsonObject privateJwks = new JsonParser().parse(jwks.toJSONObject(false).toJSONString()).getAsJsonObject();
		return privateJwks;
	}
}
