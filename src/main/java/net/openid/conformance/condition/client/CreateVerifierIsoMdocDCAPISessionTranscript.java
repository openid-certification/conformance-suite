package net.openid.conformance.condition.client;

import com.nimbusds.jose.util.Base64;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborArray;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.DiagnosticOption;
import org.multipaz.cbor.Simple;
import org.bouncycastle.util.encoders.Hex;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.Crypto;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

public class CreateVerifierIsoMdocDCAPISessionTranscript extends AbstractCondition {
	@Override
	@PreEnvironment(strings = { "client_id", "origin", "nonce" })
	@PostEnvironment(strings = "session_transcript")
	public Environment evaluate(Environment env) {
		String clientId = env.getString("client_id");
		String origin = env.getString("origin");
		String nonce =  env.getString("nonce");
		// the contents of the handover / session transcript is as defined at https://openid.net/specs/openid-4-verifiable-presentations-1_0-24.html#name-handover-and-sessiontranscr

		Map<String, String> sessionTranscriptInput = Map.of(
			"clientId", clientId,
			"origin", origin,
			"nonce", nonce);

		byte[] handoverInfo = Cbor.INSTANCE.encode(
			CborArray.Companion.builder()
				.add(origin)
				.add(clientId)
				.add(nonce)
				.end()
				.build());
		byte[] handoverInfoHash;
		try {
			handoverInfoHash = kotlinx.coroutines.BuildersKt.runBlocking(
				kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
				(scope, continuation) -> Crypto.INSTANCE.digest(Algorithm.SHA256, handoverInfo, continuation)
			);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}

		String handoverInfoDiagnostics = Cbor.INSTANCE.toDiagnostics(handoverInfo,
			Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));
		log("Created OpenID4VPDCAPIHandoverInfo",
			args("handover_info_b64", Base64.encode(handoverInfo).toString(),
				"handover_info_hash_b64", Base64.encode(handoverInfoHash).toString(),
				"cbor_diagnostic", handoverInfoDiagnostics));

		DataItem handover = CborArray.Companion.builder()
			.add("OpenID4VPDCAPIHandover")
			.add(handoverInfoHash)
			.end()
			.build();
		byte[] handoverBytes = Cbor.INSTANCE.encode(handover);

		byte[] sessionTranscript = Cbor.INSTANCE.encode(
			CborArray.Companion.builder()
				.add(Simple.Companion.getNULL())
				.add(Simple.Companion.getNULL())
				.add(handover)
				.end()
				.build()
		);

		String diagnostics = Cbor.INSTANCE.toDiagnostics(sessionTranscript,
			Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));

		String transcript_b64 = Base64.encode(sessionTranscript).toString();

		env.putString("session_transcript", transcript_b64);

		// every input and intermediate of the handover calculation, with all bytes in hex, as
		// one ordered multi-line string (a map's entries render in arbitrary order in the log
		// UI) so a mismatching counterparty can compare step by step
		String calculationDetail = String.join("\n",
			"origin (utf8 bytes): " + Hex.toHexString(origin.getBytes(StandardCharsets.UTF_8)),
			"client_id (utf8 bytes): " + Hex.toHexString(clientId.getBytes(StandardCharsets.UTF_8)),
			"nonce (utf8 bytes): " + Hex.toHexString(nonce.getBytes(StandardCharsets.UTF_8)),
			"OpenID4VPDCAPIHandoverInfo = CBOR([origin (tstr), client_id (tstr), nonce (tstr)]): " + Hex.toHexString(handoverInfo),
			"handoverInfoHash = SHA-256(OpenID4VPDCAPIHandoverInfo): " + Hex.toHexString(handoverInfoHash),
			"OpenID4VPDCAPIHandover = CBOR([\"OpenID4VPDCAPIHandover\" (tstr), handoverInfoHash (bstr)]): " + Hex.toHexString(handoverBytes),
			"SessionTranscript = CBOR([null, null, OpenID4VPDCAPIHandover]): " + Hex.toHexString(sessionTranscript));

		log("Created session transcript",
			args("session_transcript_input", sessionTranscriptInput,
				"session_transcript_b64", transcript_b64,
				"cbor_diagnostic", diagnostics,
				"calculation_detail", calculationDetail));

		return env;
	}

}
