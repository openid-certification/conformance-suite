package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.mdoc.rical.Rical;
import org.multipaz.mdoc.rical.RicalCertificateInfo;
import org.multipaz.mdoc.rical.SignedRical;

/**
 * Parses the registered signed RICAL (untagged COSE_Sign1, ISO/IEC 18013-5 second edition draft
 * Annex F) and stores a summary in the environment. Signature verification is done separately by
 * {@link ValidateRicalSignature} so that a broken signature surfaces as its own finding rather
 * than blocking all other RICAL checks.
 */
public class ParseRical extends AbstractRicalCondition {

	@Override
	@PreEnvironment(required = "rical")
	public Environment evaluate(Environment env) {

		byte[] ricalBytes = getRicalBytes(env);

		LenientRical lenient;
		try {
			lenient = parseSignedRicalLenient(ricalBytes);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw error("Interrupted while parsing the RICAL", e);
		}
		SignedRical signedRical = lenient.signedRical;

		Rical rical = signedRical.getRical();
		// cap the per-certificate detail so a large RICAL doesn't produce an enormous log entry
		int logLimit = 20;
		JsonArray certificates = new JsonArray();
		for (RicalCertificateInfo certInfo : rical.getCertificateInfos().stream().limit(logLimit).toList()) {
			JsonObject entry = new JsonObject();
			entry.addProperty("subject", certInfo.getCertificate().getSubject().getName());
			entry.addProperty("is_trust_anchor", certInfo.isTrustAnchor());
			if (certInfo.getName() != null) {
				entry.addProperty("name", certInfo.getName());
			}
			if (certInfo.getType() != null) {
				entry.addProperty("type", certInfo.getType());
			}
			if (certInfo.getIssuingCountry() != null) {
				entry.addProperty("issuing_country", certInfo.getIssuingCountry());
			}
			if (certInfo.getTrustConstraints() != null && !certInfo.getTrustConstraints().isEmpty()) {
				entry.addProperty("trust_constraints", certInfo.getTrustConstraints().size());
			}
			certificates.add(entry);
		}
		if (rical.getCertificateInfos().size() > logLimit) {
			certificates.add("... " + (rical.getCertificateInfos().size() - logLimit) + " further entries not shown");
		}

		logSuccess("Parsed RICAL",
			args("provider", rical.getProvider(),
				// true when the strict parser rejected the list and mis-encoded serialNumber
				// fields had to be normalized from the embedded certificates; the defect
				// itself is reported by ValidateRicalStructure
				"serial_numbers_normalized", lenient.serialNumbersNormalized,
				"version", rical.getVersion(),
				"type", rical.getType(),
				"date", rical.getDate().toString(),
				"next_update", rical.getNextUpdate() == null ? null : rical.getNextUpdate().toString(),
				"not_after", rical.getNotAfter() == null ? null : rical.getNotAfter().toString(),
				"certificate_count", rical.getCertificateInfos().size(),
				"certificates", certificates));

		return env;
	}
}
