package net.openid.conformance.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import java.text.ParseException;

public class JWTUtil {

	/**
	 * wrapper for Nimbus JWTParser
	 * just in case we want to override something one day
	 * @param jwtAsString
	 * @return
	 * @throws ParseException
	 */
	public static JWT parseJWT(String jwtAsString) throws ParseException {
		JWT jwt = JWTParser.parse(jwtAsString);
		return jwt;
	}

	public static JsonObject jwtClaimsSetAsJsonObject(JWT jwt, boolean includeNullValues) throws ParseException {
		JsonObject claims = new JsonParser().parse(jwt.getJWTClaimsSet().toJSONObject(includeNullValues).toJSONString()).getAsJsonObject();
		return claims;
	}

	public static JsonObject jwtHeaderAsJsonObject(JWT jwt) {
		JsonObject header = new JsonParser().parse(jwt.getHeader().toJSONObject().toJSONString()).getAsJsonObject();
		return header;
	}

}
