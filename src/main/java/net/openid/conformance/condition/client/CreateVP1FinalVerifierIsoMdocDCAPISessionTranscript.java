package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.ArrayBuilder;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborArray;
import org.multipaz.cbor.CborBuilder;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.DiagnosticOption;
import org.multipaz.cbor.Simple;
import org.bouncycastle.util.encoders.Hex;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.Crypto;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Map;
import java.util.Set;

public class CreateVP1FinalVerifierIsoMdocDCAPISessionTranscript extends AbstractCondition {
	@Override
	@PreEnvironment(strings = { "origin", "nonce" })
	@PostEnvironment(strings = "session_transcript")
	public Environment evaluate(Environment env) {
		JsonObject jwkJson = env.getObject("decryption_jwk");
		byte[] jwkThumbprint = null;
		String jwkPublicJson = null;
		if (jwkJson != null) {
			try {
				JWK jwk = JWK.parse(jwkJson.toString());
				jwkPublicJson = jwk.toPublicJWK().toJSONString();
				// computeThumbprint return base64url, but the spec requires us to use the raw bytes of the hash output
				jwkThumbprint = jwk.computeThumbprint().decode();
			} catch (ParseException | JOSEException e) {
				throw new RuntimeException(e);
			}
		}

		String origin = env.getString("origin");
		String nonce =  env.getString("nonce");

		// https://openid.net/specs/openid-4-verifiable-presentations-1_0-29.html#appendix-B.2.6.2
		Map<String, String> sessionTranscriptInput = Map.of(
			"jwkThumbprint_b64", jwkThumbprint != null ? Base64.encode(jwkThumbprint).toString() : "<null>",
			"origin", origin,
			"nonce", nonce);

		ArrayBuilder<CborBuilder> builder = CborArray.Companion.builder()
			.add(origin)
			.add(nonce);
		if (jwkThumbprint != null) {
			builder.add(jwkThumbprint);
		} else {
			builder.add(Simple.Companion.getNULL());
		}
		byte[] handoverInfo = Cbor.INSTANCE.encode(
			builder.end().build());
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

		// every input and intermediate of the OID4VP B.2.6.2 OpenID4VPDCAPIHandover calculation,
		// with all bytes in hex, as one ordered multi-line string (a map's entries render in
		// arbitrary order in the log UI) so a mismatching counterparty can compare step by step
		String calculationDetail = String.join("\n",
			"origin (utf8 bytes): " + Hex.toHexString(origin.getBytes(StandardCharsets.UTF_8)),
			"nonce (utf8 bytes): " + Hex.toHexString(nonce.getBytes(StandardCharsets.UTF_8)),
			"response encryption JWK (public part): " + (jwkPublicJson != null ? jwkPublicJson : "<none - the handover uses null in place of the thumbprint>"),
			"jwkThumbprint = RFC 7638 SHA-256 thumbprint of that JWK: " + (jwkThumbprint != null ? Hex.toHexString(jwkThumbprint) : "null"),
			"OpenID4VPDCAPIHandoverInfo = CBOR([origin (tstr), nonce (tstr), jwkThumbprint (bstr, or null)]): " + Hex.toHexString(handoverInfo),
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
