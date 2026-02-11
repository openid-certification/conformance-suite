package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DiagnosticOption;
import org.multipaz.crypto.AsymmetricKey;
import org.multipaz.mdoc.response.DeviceResponseParser;
import org.multipaz.testapp.TestAppUtils;

import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@SuppressWarnings("deprecation")
public class ParseCredentialAsMdoc extends AbstractCondition {
	@Override
	@PreEnvironment(strings = { "credential", "session_transcript" })
//	@PostEnvironment(required = "mdoc")
	public Environment evaluate(Environment env) {
		// as per ISO 18013-7, vp_token is a base64url-encoded-without-padding DeviceResponse data structure as defined in ISO/IEC 18013-5.
		String mdocBase64 = env.getString("credential");

		byte[] bytes = new Base64URL(mdocBase64).decode();

		String diagnostics = Cbor.INSTANCE.toDiagnostics(bytes,
			Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));

		byte[] sessionTranscript = Base64.getDecoder().decode(env.getString("session_transcript"));

		DeviceResponseParser parser = new DeviceResponseParser(bytes, sessionTranscript);

		// this is only required for MACed mdocs
		JsonObject jwkJson = env.getObject("decryption_jwk");
		JWK jwk = null;
		if (jwkJson != null) {
			try {
				jwk = JWK.parse(jwkJson.toString());
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
			var key = TestAppUtils.convertToEcPrivateKey(jwk);
			parser.setEphemeralReaderKey(AsymmetricKey.Companion.anonymous(key, key.getCurve().getDefaultSigningAlgorithmFullySpecified())); // pass encryption key
		}

		DeviceResponseParser.DeviceResponse response;
		try {
			response = kotlinx.coroutines.BuildersKt.runBlocking(
				kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
				(scope, continuation) -> parser.parse(continuation)
			);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
		List<DeviceResponseParser.Document> docs = response.getDocuments();
		if (docs.size() != 1) {
			throw error("Expected exactly one document in mdoc",
				args("expected", 1,
					"actual", docs.size(),
					"cbor_diagnostic", diagnostics));
		}
		if (!docs.get(0).getDeviceSignedAuthenticated()) {
			throw error("mdoc device-signed data was neither properly MACed nor signed by a DeviceKey in the MSO. This may mean the contents of the Session Transcript are wrong - expand the items above where the conformance suite calculates the session transcript to see what values were used.",
				args("cbor_diagnostic", diagnostics));
		}

		logSuccess("Parsed mdoc & validated device-signed data", args("cbor_diagnostic", diagnostics));

		return env;
	}

}
