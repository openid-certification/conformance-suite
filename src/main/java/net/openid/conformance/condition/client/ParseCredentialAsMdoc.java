package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
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
	@PostEnvironment(required = { "mdoc" }, strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		// as per ISO 18013-7, vp_token is a base64url-encoded-without-padding DeviceResponse data structure as defined in ISO/IEC 18013-5.
		String mdocBase64 = env.getString("credential");

		byte[] bytes = new Base64URL(mdocBase64).decode();

		String diagnostics = Cbor.INSTANCE.toDiagnostics(bytes,
			Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));

		// Extract IssuerSigned from DeviceResponse for downstream conditions (e.g. revocation check)
		try {
			DataItem deviceResponseItem = Cbor.INSTANCE.decode(bytes);
			DataItem documents = deviceResponseItem.getOrNull("documents");
			if (documents == null) {
				throw error("DeviceResponse does not contain a 'documents' array");
			}
			DataItem firstDoc = documents.getAsArray().get(0);
			DataItem issuerSigned = firstDoc.getOrNull("issuerSigned");
			if (issuerSigned == null) {
				throw error("First document in DeviceResponse does not contain 'issuerSigned'");
			}
			byte[] issuerSignedBytes = Cbor.INSTANCE.encode(issuerSigned);
			env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(issuerSignedBytes));
		} catch (ConditionError e) {
			throw e;
		} catch (Exception e) {
			throw error("Failed to extract IssuerSigned from DeviceResponse", e);
		}

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
		if (!docs.get(0).getIssuerSignedAuthenticated()) {
			throw error("mdoc issuer-signed data (issuerAuth COSE_Sign1) signature verification failed. The MSO signature could not be verified against the public key in the issuer certificate.",
				args("cbor_diagnostic", diagnostics));
		}
		if (!docs.get(0).getDeviceSignedAuthenticated()) {
			throw error("mdoc device-signed data was neither properly MACed nor signed by a DeviceKey in the MSO. This may mean the contents of the Session Transcript are wrong - expand the items above where the conformance suite calculates the session transcript to see what values were used.",
				args("cbor_diagnostic", diagnostics));
		}

		DeviceResponseParser.Document doc = docs.get(0);
		JsonObject disclosedElements = new JsonObject();
		for (String namespace : doc.getIssuerNamespaces()) {
			JsonArray elementNames = new JsonArray();
			for (String elementName : doc.getIssuerEntryNames(namespace)) {
				elementNames.add(elementName);
			}
			disclosedElements.add(namespace, elementNames);
		}
		JsonObject mdoc = new JsonObject();
		mdoc.addProperty("docType", doc.getDocType());
		mdoc.add("disclosed_elements", disclosedElements);
		env.putObject("mdoc", mdoc);

		logSuccess("Parsed mdoc & validated issuer-signed and device-signed data",
			args("cbor_diagnostic", diagnostics,
				"docType", doc.getDocType(),
				"disclosed_elements", disclosedElements));

		return env;
	}

}
