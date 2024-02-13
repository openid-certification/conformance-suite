package net.openid.conformance.condition.client;

import com.android.identity.mdoc.response.DeviceResponseParser;
import com.android.identity.util.CborUtil;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ParseVpTokenAsMdoc extends AbstractCondition {
	@Override
	@PreEnvironment(strings = "vp_token")
	//@PostEnvironment(required = "sdjwt")
	public Environment evaluate(Environment env) {
		// as per ISO 18013-7, vp_token is a base64url-encoded-without-padding DeviceResponse data structure as defined in ISO/IEC 18013-5.
		String mdocBase64 = env.getString("vp_token");

		byte[] bytes = new Base64URL(mdocBase64).decode();
		byte[] sessionTranscript = { 0 }; // FIXME need to calculate this properly otherwise deviceSignedAuthenticated will be false

		DeviceResponseParser parser = new DeviceResponseParser();
		parser.setDeviceResponse(bytes);
		parser.setSessionTranscript(sessionTranscript);

		{
			@SuppressWarnings("unused")
			DeviceResponseParser.DeviceResponse response = parser.parse();
		}


		String diagnostics = CborUtil.toDiagnostics(bytes,
			CborUtil.DIAGNOSTICS_FLAG_PRETTY_PRINT | CborUtil.DIAGNOSTICS_FLAG_EMBEDDED_CBOR);
//System.out.println(diagnostics);
		logSuccess("Parsed mdoc", args("cbor_diagnostic", diagnostics));


//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		new CborEncoder(baos).encode(
//			new CborBuilder()
//				.addArray()
//				.add(SimpleValue.NULL)   // DeviceEngagementBytes isn't used.
//				.add(SimpleValue.NULL)   // EReaderKeyBytes isn't used.
//				.addArray()              // Proprietary handover structure follows.
//				.add("TestHandover")
//				.add(new ByteString(new byte[] {1, 2, 3, 4}))
//				.add(new ByteString(new byte[] {10, 11, 12, 13, 14}))
//				.add(new UnicodeString("something"))
//				.end()
//				.end()
//				.build());
//		byte[] sessionTranscript = baos.toByteArray();
//		session.setSessionTranscript(sessionTranscript);

//		SDJWT sdJwt;
//		try {
//			sdJwt = SDJWT.parse(mdocBase64);
//		} catch (IllegalArgumentException e) {
//			throw error("Parsing SD-JWT failed", e, args("sdjwt", mdocBase64));
//		}
//
//
//		List<String> disclosures = sdJwt.getDisclosures().stream().map(Disclosure::getJson).collect(Collectors.toList());
//		Map<String, Object> decodedMap;
//		try {
//			String credJwtStr = sdJwt.getCredentialJwt();
//			JWT nimbusCredJwt = JWTUtil.parseJWT(credJwtStr);
//			JWTClaimsSet jwtClaimsSet = nimbusCredJwt.getJWTClaimsSet();
//			boolean includeNullValues = true;
//			decodedMap = new SDObjectDecoder().decode(
//				jwtClaimsSet.toJSONObject(includeNullValues),
//				sdJwt.getDisclosures()
//			);
//		} catch (ParseException e) {
//			throw error("Applying SD-JWT disclosures failed", e, args("sdjwt", mdocBase64));
//		}
//
//		JsonObject credJwt = null;
//		try {
//			credJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(sdJwt.getCredentialJwt());
//		} catch (ParseException e) {
//			throw error("Parsing SD-JWT credential jwt failed", e, args("sdjwt", mdocBase64));
//		}
//
//		JsonObject bindJwt = null;
//		try {
//			String bindingJwt = sdJwt.getBindingJwt();
//			if (bindingJwt == null) {
//				throw error("SD-JWT does not containg a holder binding", args("sdjwt", mdocBase64));
//			}
//			bindJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(bindingJwt);
//		} catch (ParseException e) {
//			throw error("Parsing SD-JWT binding jwt failed", e, args("sdjwt", mdocBase64));
//		}
//
//		JsonObject jsonObject = new JsonObject();
//		jsonObject.addProperty("value", mdocBase64);
//		jsonObject.add("decoded", JsonParser.parseString(gson.toJson(decodedMap)).getAsJsonObject());
//		jsonObject.add("disclosures", JsonParser.parseString(gson.toJson(disclosures)).getAsJsonArray());
//		jsonObject.add("binding", bindJwt);
//		jsonObject.add("credential", credJwt);
//
//		env.putObject("sdjwt", jsonObject);
//
//		logSuccess("Parsed SDJWT", jsonObject);

		return env;
	}

}
