package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.mdoc.vical.SignedVical;
import org.multipaz.mdoc.vical.Vical;
import org.multipaz.mdoc.vical.VicalCertificateInfo;

/**
 * Parses the registered signed VICAL (untagged COSE_Sign1, ISO/IEC 18013-5 Annex C) and stores a
 * summary in the environment. Signature verification is done separately by
 * {@link ValidateVicalSignature} so that a broken signature surfaces as its own finding rather
 * than blocking all other VICAL checks.
 */
public class ParseVical extends AbstractVicalCondition {

	@Override
	@PreEnvironment(required = "vical")
	public Environment evaluate(Environment env) {

		byte[] vicalBytes = getVicalBytes(env);

		SignedVical signedVical;
		try {
			signedVical = parseSignedVical(vicalBytes, true);
		} catch (Exception e) {
			throw error("Failed to parse VICAL as a COSE_Sign1-signed VICAL structure", e);
		}

		Vical vical = signedVical.getVical();
		// cap the per-certificate detail so a national VICAL with hundreds of IACAs
		// doesn't produce an enormous single log entry
		int logLimit = 20;
		JsonArray certificates = new JsonArray();
		for (VicalCertificateInfo certInfo : vical.getCertificateInfos().stream().limit(logLimit).toList()) {
			JsonObject entry = new JsonObject();
			entry.addProperty("subject", certInfo.getCertificate().getSubject().getName());
			JsonArray docTypes = new JsonArray();
			certInfo.getDocTypes().forEach(docTypes::add);
			entry.add("doc_types", docTypes);
			if (certInfo.getIssuingAuthority() != null) {
				entry.addProperty("issuing_authority", certInfo.getIssuingAuthority());
			}
			if (certInfo.getIssuingCountry() != null) {
				entry.addProperty("issuing_country", certInfo.getIssuingCountry());
			}
			certificates.add(entry);
		}
		if (vical.getCertificateInfos().size() > logLimit) {
			certificates.add("... " + (vical.getCertificateInfos().size() - logLimit) + " further entries not shown");
		}

		logSuccess("Parsed VICAL",
			args("provider", vical.getVicalProvider(),
				"version", vical.getVersion(),
				"date", vical.getDate().toString(),
				"next_update", vical.getNextUpdate() == null ? null : vical.getNextUpdate().toString(),
				"certificate_count", vical.getCertificateInfos().size(),
				"certificates", certificates));

		return env;
	}
}
