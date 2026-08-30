package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.cbor.ArrayBuilder;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborArray;
import org.multipaz.cbor.CborBuilder;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.DiagnosticOption;
import org.multipaz.cbor.Simple;

import java.text.ParseException;
import java.util.Map;
import java.util.Set;

public abstract class AbstractCreateVP1FinalIsoMdocRedirectSessionTranscript extends AbstractCondition {

	protected void calculateSessionTranscript(Environment env, JsonObject jwkJson, String clientId, String nonce, String responseUri) {
		JWK jwk = null;
		if (jwkJson != null) {
			try {
				jwk = JWK.parse(jwkJson.toString());
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
		calculateSessionTranscript(env, jwk, clientId, nonce, responseUri);
	}

	protected void calculateSessionTranscript(Environment env, JWK jwk, String clientId, String nonce, String responseUri) {
		byte[] jwkThumbprint = null;
		if (jwk != null) {
			try {
				// computeThumbprint return base64url, but the spec requires us to use the raw bytes of the hash output
				jwkThumbprint = jwk.computeThumbprint().decode();
			} catch (JOSEException e) {
				throw new RuntimeException(e);
			}
		}

		// https://openid.net/specs/openid-4-verifiable-presentations-1_0-29.html#appendix-B.2.6.2
		Map<String, String> sessionTranscriptInput = Map.of(
			"jwkThumbprint_b64", jwkThumbprint != null ? Base64.encode(jwkThumbprint).toString() : "<null>",
			"client_id", clientId,
			"nonce", nonce,
			"response_uri", responseUri);

		ArrayBuilder<CborBuilder> builder = CborArray.Companion.builder()
			.add(clientId)
			.add(nonce);
		if (jwkThumbprint != null) {
			builder.add(jwkThumbprint);
		} else {
			builder.add(Simple.Companion.getNULL());
		}
		builder.add(responseUri);
		byte[] handoverInfo = Cbor.INSTANCE.encode(
			builder.end().build());
		byte[] handoverInfoHash = MdocUtil.sha256(handoverInfo);

		String handoverInfoDiagnostics = Cbor.INSTANCE.toDiagnostics(handoverInfo,
			Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));
		log("Created OpenID4VPHandover",
			args("handover_info_b64", Base64.encode(handoverInfo).toString(),
				"handover_info_hash_b64", Base64.encode(handoverInfoHash).toString(),
				"cbor_diagnostic", handoverInfoDiagnostics));

		DataItem handover = CborArray.Companion.builder()
			.add("OpenID4VPHandover")
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

		// every input and intermediate of the OID4VP B.2.6.1 OpenID4VPHandover calculation, with
		// all bytes in hex, as one ordered multi-line string (a map's entries render in arbitrary
		// order in the log UI) so a mismatching counterparty can compare step by step
		String calculationDetail = String.join("\n",
			"client_id (utf8 bytes): " + MdocUtil.utf8Hex(clientId),
			"nonce (utf8 bytes): " + MdocUtil.utf8Hex(nonce),
			"response_uri (utf8 bytes): " + MdocUtil.utf8Hex(responseUri),
			"response encryption JWK (public part): " + MdocUtil.describeEncryptionJwk(jwk),
			"jwkThumbprint = RFC 7638 SHA-256 thumbprint of that JWK: " + (jwkThumbprint != null ? MdocUtil.hex(jwkThumbprint) : "null"),
			"OpenID4VPHandoverInfo = CBOR([client_id (tstr), nonce (tstr), jwkThumbprint (bstr, or null), response_uri (tstr)]): " + MdocUtil.hex(handoverInfo),
			"handoverInfoHash = SHA-256(OpenID4VPHandoverInfo): " + MdocUtil.hex(handoverInfoHash),
			"OpenID4VPHandover = CBOR([\"OpenID4VPHandover\" (tstr), handoverInfoHash (bstr)]): " + MdocUtil.hex(handoverBytes),
			"SessionTranscript = CBOR([null, null, OpenID4VPHandover]): " + MdocUtil.hex(sessionTranscript));

		log("Created session transcript",
			args("session_transcript_input", sessionTranscriptInput,
				"session_transcript_b64", transcript_b64,
				"cbor_diagnostic", diagnostics,
				"calculation_detail", calculationDetail));
	}
}
