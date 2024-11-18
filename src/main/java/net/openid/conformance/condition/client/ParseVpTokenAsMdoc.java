package net.openid.conformance.condition.client;

import com.android.identity.cbor.Cbor;
import com.android.identity.cbor.DiagnosticOption;
import com.android.identity.mdoc.response.DeviceResponseParser;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;

public class ParseVpTokenAsMdoc extends AbstractCondition {
	@Override
	@PreEnvironment(strings = "vp_token")
//	@PostEnvironment(required = "mdoc")
	public Environment evaluate(Environment env) {
		// as per ISO 18013-7, vp_token is a base64url-encoded-without-padding DeviceResponse data structure as defined in ISO/IEC 18013-5.
		String mdocBase64 = env.getString("vp_token");

		byte[] bytes = new Base64URL(mdocBase64).decode();
		byte[] sessionTranscript = { 0 }; // FIXME need to calculate this properly otherwise deviceSignedAuthenticated will be false

		DeviceResponseParser parser = new DeviceResponseParser(bytes, sessionTranscript);

		{
			@SuppressWarnings("unused")
			DeviceResponseParser.DeviceResponse response = parser.parse();
		}


		String diagnostics = Cbor.INSTANCE.toDiagnostics(bytes,
			Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));
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
