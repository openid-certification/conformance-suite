package net.openid.conformance.condition.as;

import com.android.identity.appsupport.ui.consent.MdocConsentField;
import com.android.identity.cbor.Cbor;
import com.android.identity.cbor.CborArray;
import com.android.identity.cbor.DataItem;
import com.android.identity.cbor.DiagnosticOption;
import com.android.identity.cbor.Simple;
import com.android.identity.crypto.Algorithm;
import com.android.identity.crypto.Crypto;
import com.android.identity.testapp.TestAppUtils;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CreateMdocVpToken extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "vp_token")
	public Environment evaluate(Environment env) {
		byte[] sessionTranscript = createSessionTranscript(env);

		// This will result in an mdoc without any fields (no name/etc). We should perhaps attempt to parse the PE/DCQL.
		List<MdocConsentField> mdocConsentFields = new ArrayList<>();

		byte[] mdoc = TestAppUtils.INSTANCE.generateEncodedDeviceResponse(mdocConsentFields, sessionTranscript);

		env.putString("vp_token", Base64URL.encode(mdoc).toString());

		String diagnostics = Cbor.INSTANCE.toDiagnostics(mdoc,
			Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));
		log("Created mdl presentation",
			args("mdoc_b64", Base64URL.encode(mdoc).toString(),
				"cbor_diagnostic", diagnostics));

		return env;
	}

	private byte[] createSessionTranscript(Environment env) {
		String clientId = env.getString("client", "client_id");
		String responseUri = env.getString("authorization_request_object", "claims.response_uri");
		String nonce = env.getString("nonce");

		byte[] nonceBytes = new byte[16];
		new SecureRandom().nextBytes(nonceBytes);
		String mdocGeneratedNonce = Base64URL.encode(nonceBytes).toString();
		env.putString("mdoc_generated_nonce", mdocGeneratedNonce);

		Map<String, String> sessionTranscriptInput = Map.of(
			"clientId", clientId,
			"responseUri", responseUri,
			"nonce", nonce,
			"mdocGeneratedNonce", mdocGeneratedNonce);

		byte[] clientIdToHash = Cbor.INSTANCE.encode(
			CborArray.Companion.builder()
				.add(clientId)
				.add(mdocGeneratedNonce)
				.end()
				.build());
		byte[] clientIdHash = Crypto.INSTANCE.digest(Algorithm.SHA256, clientIdToHash);
		byte[] responseUriToHash = Cbor.INSTANCE.encode(
			CborArray.Companion.builder()
				.add(responseUri)
				.add(mdocGeneratedNonce)
				.end()
				.build());
		byte[] responseUriHash = Crypto.INSTANCE.digest(Algorithm.SHA256, responseUriToHash);

		DataItem oid4vpHandover = CborArray.Companion.builder()
			.add(clientIdHash)
			.add(responseUriHash)
			.add(nonce)
			.end()
			.build();

		byte[] sessionTranscript = Cbor.INSTANCE.encode(
			CborArray.Companion.builder()
				.add(Simple.Companion.getNULL())
				.add(Simple.Companion.getNULL())
				.add(oid4vpHandover)
				.end()
				.build()
		);

		String diagnostics = Cbor.INSTANCE.toDiagnostics(sessionTranscript,
			Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));
		log("Created session transcript",
			args("session_transcript_input", sessionTranscriptInput,
				"session_transcript_b64", Base64URL.encode(sessionTranscript).toString(),
				"cbor_diagnostic", diagnostics));
		return sessionTranscript;
	}

}
