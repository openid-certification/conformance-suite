package net.openid.conformance.condition.client;

import com.android.identity.cbor.Cbor;
import com.android.identity.cbor.CborArray;
import com.android.identity.cbor.DataItem;
import com.android.identity.cbor.DiagnosticOption;
import com.android.identity.cbor.Simple;
import com.android.identity.crypto.Algorithm;
import com.android.identity.crypto.Crypto;
import com.nimbusds.jose.util.Base64;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractMdocSessionTranscript;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Set;

public class CreateVerifierIsoMdocDCAPISessionTranscript extends AbstractMdocSessionTranscript {
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
		byte[] handoverInfoHash = Crypto.INSTANCE.digest(Algorithm.SHA256, handoverInfo);

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

		log("Created session transcript",
			args("session_transcript_input", sessionTranscriptInput,
				"session_transcript_b64", transcript_b64,
				"cbor_diagnostic", diagnostics));

		return env;
	}

}
