package net.openid.conformance.condition.as;

import com.nimbusds.jose.util.Base64;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborArray;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.DiagnosticOption;
import org.multipaz.cbor.Simple;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.Crypto;

import java.util.Map;
import java.util.Set;

public abstract class AbstractIso18013Part7AnnexBMdocSessionTranscript extends AbstractCondition {
	public void createSessionTranscript(Environment env, String clientId, String responseUri, String nonce, String mdocGeneratedNonce) {
		// the contents of the handover / session transcript is as defined in ISO 18013 part 7 section B.4.4

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

		String transcript_b64 = Base64.encode(sessionTranscript).toString();

		env.putString("session_transcript", transcript_b64);

		log("Created session transcript",
			args("session_transcript_input", sessionTranscriptInput,
				"session_transcript_b64", transcript_b64,
				"cbor_diagnostic", diagnostics));
	}
}
