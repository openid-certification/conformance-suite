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

	/**
	 * Also see jwtStringToJsonObjectForEnvironment
	 * @param jwt
	 * @return
	 * @throws ParseException
	 */
	public static JsonObject jwtClaimsSetAsJsonObject(JWT jwt) throws ParseException {
		//added this variable to make it obvious
		boolean includeNullValues = true;
		JsonObject claims = new JsonParser().parse(jwt.getJWTClaimsSet().toJSONObject(includeNullValues).toJSONString()).getAsJsonObject();
		return claims;
	}

	/**
	 * Also see jwtStringToJsonObjectForEnvironment
	 * Note: Nimbusds will always remove null values from JWT headers
	 * @param jwt
	 * @return
	 */
	public static JsonObject jwtHeaderAsJsonObject(JWT jwt) {
		JsonObject header = new JsonParser().parse(jwt.getHeader().toJSONObject().toJSONString()).getAsJsonObject();
		return header;
	}

	/**
	 * Parses the JWT and returns a JsonObject with value, header and claims entries
	 * @param jwtAsString
	 * @return
	 * @throws ParseException
	 */
	public static JsonObject jwtStringToJsonObjectForEnvironment(String jwtAsString) throws ParseException {
		JWT token = JWTUtil.parseJWT(jwtAsString);

		JsonObject header = JWTUtil.jwtHeaderAsJsonObject(token);
		JsonObject claims = JWTUtil.jwtClaimsSetAsJsonObject(token);

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("value", jwtAsString); // save the original string to allow for crypto operations
		jsonObject.add("header", header);
		jsonObject.add("claims", claims);
		return jsonObject;
	}

}
