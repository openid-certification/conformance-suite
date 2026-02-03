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

public abstract class AbstractParseCredentialAsSdJwt extends AbstractCondition {

	protected abstract boolean expectKbJwt();

	private static final Gson gson = new Gson();

	@Override
	@PreEnvironment(strings = "credential")
	@PostEnvironment(required = "sdjwt")
	public Environment evaluate(Environment env) {
		String sdJwtStr = env.getString("credential");

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
			if (expectKbJwt()) {
				if (bindingJwt == null) {
					throw error("SD-JWT does not contain a key binding JWT", args("sdjwt", sdJwtStr));
				}
				bindJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(bindingJwt);
			} else {
				if (bindingJwt != null) {
					throw error("SD-JWT appears to have a key binding JWT, but because it was received from an issuer it should not. The SD-JWT should end in a '~'.", args("sdjwt", sdJwtStr));
				}
			}
		} catch (ParseException e) {
			throw error("Parsing SD-JWT key binding jwt failed", e, args("sdjwt", sdJwtStr));
		}
		JsonObject decodedJsonObject = JsonParser.parseString(gson.toJson(decodedMap)).getAsJsonObject();

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("value", sdJwtStr);
		jsonObject.add("decoded", decodedJsonObject);
		jsonObject.add("disclosures", JsonParser.parseString(gson.toJson(disclosures)).getAsJsonArray());
		jsonObject.add("binding", bindJwt);
		jsonObject.add("credential", credJwt);

		env.putObject("sdjwt", jsonObject);

		logSuccess("Parsed SDJWT", jsonObject);

		return env;
	}
}
