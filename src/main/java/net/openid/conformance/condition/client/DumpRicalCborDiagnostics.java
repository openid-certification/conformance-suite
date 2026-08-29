package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DiagnosticOption;
import org.multipaz.cose.CoseSign1;

import java.util.Set;

/**
 * Logs the registered RICAL in CBOR diagnostic notation (RFC 8949 section 8) so its actual
 * encoding can be inspected without external tooling: the COSE_Sign1 envelope with byte string
 * lengths only, and the RICAL payload in full. Purely informational - expected to be called
 * with an INFO severity.
 */
public class DumpRicalCborDiagnostics extends AbstractRicalCondition {

	// a RICAL with many certificates produces a large dump; cap the log entry size
	private static final int MAX_DIAGNOSTIC_CHARS = 200_000;

	@Override
	@PreEnvironment(required = "rical")
	public Environment evaluate(Environment env) {

		byte[] ricalBytes = getRicalBytes(env);

		String envelope;
		CoseSign1 coseSign1;
		try {
			envelope = Cbor.INSTANCE.toDiagnostics(ricalBytes,
				Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.BSTR_PRINT_LENGTH));
			coseSign1 = getRicalCoseSign1(ricalBytes);
		} catch (Exception e) {
			throw error("Failed to render the RICAL in CBOR diagnostic notation", e);
		}

		String payload;
		try {
			payload = Cbor.INSTANCE.toDiagnostics(coseSign1.getPayload(),
				Set.of(DiagnosticOption.PRETTY_PRINT));
		} catch (Exception e) {
			throw error("Failed to render the RICAL COSE_Sign1 payload in CBOR diagnostic notation", e);
		}
		if (payload.length() > MAX_DIAGNOSTIC_CHARS) {
			payload = payload.substring(0, MAX_DIAGNOSTIC_CHARS)
				+ "\n... truncated, " + (payload.length() - MAX_DIAGNOSTIC_CHARS) + " further characters not shown";
		}

		logSuccess("RICAL in CBOR diagnostic notation",
			args("cose_sign1_envelope", envelope,
				"rical_payload", payload));

		return env;
	}
}
