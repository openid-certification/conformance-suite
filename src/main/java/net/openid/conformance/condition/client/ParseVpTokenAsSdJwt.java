package net.openid.conformance.condition.client;

import com.authlete.sd.Disclosure;
import com.authlete.sd.SDJWT;
import com.authlete.sd.SDObjectDecoder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParseVpTokenAsSdJwt extends AbstractCondition {

	private static final Gson gson = new Gson();

	@Override
	@PreEnvironment(strings = "vp_token")
	@PostEnvironment(required = "sdjwt")
	public Environment evaluate(Environment env) {
		String sdJwtStr = env.getString("vp_token");

		SDJWT sdJwt;
		try {
			sdJwt = SDJWT.parse(sdJwtStr);
		} catch (IllegalArgumentException e) {
			throw error("Parsing SD-JWT failed", e, args("sdjwt", sdJwtStr));
		}


		List<String> disclosures = sdJwt.getDisclosures().stream().map(Disclosure::getJson).collect(Collectors.toList());
		Map<String, Object> decodedMap;
		try {
			String credJwtStr = sdJwt.getCredentialJwt();
			JWT nimbusCredJwt = JWTUtil.parseJWT(credJwtStr);
			JWTClaimsSet jwtClaimsSet = nimbusCredJwt.getJWTClaimsSet();
			boolean includeNullValues = true;
			decodedMap = new SDObjectDecoder().decode(
				jwtClaimsSet.toJSONObject(includeNullValues),
				sdJwt.getDisclosures()
			);
		} catch (ParseException e) {
			throw error("Applying SD-JWT disclosures failed", e, args("sdjwt", sdJwtStr));
		}

		JsonObject credJwt = null;
		try {
			credJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(sdJwt.getCredentialJwt());
		} catch (ParseException e) {
			throw error("Parsing SD-JWT credential jwt failed", e, args("sdjwt", sdJwtStr));
		}

		JsonObject bindJwt = null;
		try {
			String bindingJwt = sdJwt.getBindingJwt();
			if (bindingJwt == null) {
				throw error("SD-JWT does not contain a key binding", args("sdjwt", sdJwtStr));
			}
			bindJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(bindingJwt);
		} catch (ParseException e) {
			throw error("Parsing SD-JWT key binding jwt failed", e, args("sdjwt", sdJwtStr));
		}

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("value", sdJwtStr);
		jsonObject.add("decoded", JsonParser.parseString(gson.toJson(decodedMap)).getAsJsonObject());
		jsonObject.add("disclosures", JsonParser.parseString(gson.toJson(disclosures)).getAsJsonArray());
		jsonObject.add("binding", bindJwt);
		jsonObject.add("credential", credJwt);

		env.putObject("sdjwt", jsonObject);

		logSuccess("Parsed SDJWT", jsonObject);

		return env;
	}

}
