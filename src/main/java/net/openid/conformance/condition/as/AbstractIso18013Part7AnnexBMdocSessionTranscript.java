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
		byte[] clientIdHash;
		byte[] responseUriHash;
		byte[] responseUriToHash = Cbor.INSTANCE.encode(
			CborArray.Companion.builder()
				.add(responseUri)
				.add(mdocGeneratedNonce)
				.end()
				.build());
		try {
			clientIdHash = kotlinx.coroutines.BuildersKt.runBlocking(
				kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
				(scope, continuation) -> Crypto.INSTANCE.digest(Algorithm.SHA256, clientIdToHash, continuation)
			);
			responseUriHash = kotlinx.coroutines.BuildersKt.runBlocking(
				kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
				(scope, continuation) -> Crypto.INSTANCE.digest(Algorithm.SHA256, responseUriToHash, continuation)
			);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}

		DataItem oid4vpHandover = CborArray.Companion.builder()
			.add(clientIdHash)
			.add(responseUriHash)
			.add(nonce)
			.end()
			.build();
		byte[] oid4vpHandoverBytes = Cbor.INSTANCE.encode(oid4vpHandover);

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

		// every input and intermediate of the ISO/IEC 18013-7 B.4.4 OID4VPHandover calculation,
		// with all bytes in hex, as one ordered multi-line string (a map's entries render in
		// arbitrary order in the log UI) so a mismatching counterparty can compare step by step
		String calculationDetail = String.join("\n",
			"client_id (utf8 bytes): " + hex(clientId.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
			"mdocGeneratedNonce = base64url decoded JWE apu (utf8 bytes): " + hex(mdocGeneratedNonce.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
			"ClientIdToHash = CBOR([client_id, mdocGeneratedNonce], both text strings): " + hex(clientIdToHash),
			"clientIdHash = SHA-256(ClientIdToHash): " + hex(clientIdHash),
			"response_uri (utf8 bytes): " + hex(responseUri.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
			"ResponseUriToHash = CBOR([response_uri, mdocGeneratedNonce], both text strings): " + hex(responseUriToHash),
			"responseUriHash = SHA-256(ResponseUriToHash): " + hex(responseUriHash),
			"nonce from the authorization request (utf8 bytes): " + hex(nonce.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
			"OID4VPHandover = CBOR([clientIdHash (bstr), responseUriHash (bstr), nonce (tstr)]): " + hex(oid4vpHandoverBytes),
			"SessionTranscript = CBOR([null, null, OID4VPHandover]): " + hex(sessionTranscript));

		log("Created session transcript",
			args("session_transcript_input", sessionTranscriptInput,
				"session_transcript_b64", transcript_b64,
				"cbor_diagnostic", diagnostics,
				"calculation_detail", calculationDetail));
	}

	private static String hex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
