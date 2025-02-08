package net.openid.conformance.condition.client;

import com.android.identity.cbor.Cbor;
import com.android.identity.cbor.DiagnosticOption;
import com.android.identity.mdoc.response.DeviceResponseParser;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractMdocSessionTranscript;
import net.openid.conformance.testmodule.Environment;

import java.util.List;
import java.util.Set;

public class ParseVpTokenAsMdoc extends AbstractMdocSessionTranscript {
	@Override
	@PreEnvironment(strings = "vp_token")
//	@PostEnvironment(required = "mdoc")
	public Environment evaluate(Environment env) {
		// as per ISO 18013-7, vp_token is a base64url-encoded-without-padding DeviceResponse data structure as defined in ISO/IEC 18013-5.
		String mdocBase64 = env.getString("vp_token");

		byte[] bytes = new Base64URL(mdocBase64).decode();

		String diagnostics = Cbor.INSTANCE.toDiagnostics(bytes,
			Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));

		String clientId = env.getString("config", "client.client_id");
		String responseUri = env.getString("response_uri");
		String nonce =  env.getString("nonce");
		String mdocGeneratedNonce = env.getString("mdoc_generated_nonce");
		byte[] sessionTranscript = createSessionTranscript(clientId, responseUri, nonce, mdocGeneratedNonce);

		DeviceResponseParser parser = new DeviceResponseParser(bytes, sessionTranscript);
		DeviceResponseParser.DeviceResponse response = parser.parse();
		List<DeviceResponseParser.Document> docs = response.getDocuments();
		if (docs.size() != 1) {
			throw error("Expected exactly one document in mdoc",
				args("expected", 1,
					"actual", docs.size(),
					"cbor_diagnostic", diagnostics));
		}
		if (!docs.get(0).getDeviceSignedAuthenticated()) {
			throw error("mdoc device-signed data was neither properly MACed nor signed by a DeviceKey in the MSO.",
				args("cbor_diagnostic", diagnostics));
		}

		logSuccess("Parsed mdoc & validate device-signed data", args("cbor_diagnostic", diagnostics));

		return env;
	}

}
