package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.X509CertificateUtil;
import org.bouncycastle.asn1.x500.style.BCStyle;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Takes the trust anchors out of the list of trusted entities: every X.509 certificate in a
 * ServiceDigitalIdentity whose service type the slot accepts (ETSI TS 119 602 clause 6.6.3), with
 * the trusted entity's name and the service type, so the chain checks can say which listed
 * entity a certificate chains to. A service whose ServiceStatus says it is no longer in force
 * (clause 6.6.4) is left out: a withdrawn provider's certificate is not a trust anchor. Certificates
 * whose subject organizationName does not strictly match the TEName are noted in the log but used:
 * clause 6.6.3.0 only says they should match, and the Commission's development lists miss it on
 * most entries (TEName "EUDI Wallet Reference Implementation - CZ" against organizationName "EUDI
 * Wallet Reference Implementation"), so a finding would be noise. Re-verifies the JWS first, and its signer against the configured
 * LoTE signing certificate when one is set: a corrupted, re-signed or substituted list must not
 * silently drive a trust decision, whatever the earlier checks' severity was. An
 * empty list (no TrustedEntitiesList, clause 6.4.0) yields no anchors; that is the shape of the
 * event's negative-test lists, so it is not an error here but a failed chain check later.
 *
 * <p>Writes {@code anchors}: an array of {@code {cert, subject, service_type, te_name}} with the
 * certificate as base64 DER.
 */
public class ExtractLoteTrustAnchors extends AbstractLoteCondition {

	/**
	 * The last path segment of the service status URIs that mean a service is in force. The EU
	 * profiles of Annexes D to G do not use ServiceStatus at all (its absence means every listed
	 * service is approved, clause 6.4.3 note 1); the Pub-EAA profile (Annex H) uses
	 * {@code .../SvcStatus/notified} and {@code .../SvcStatus/withdrawn}, and ETSI TS 119 612 trusted
	 * lists {@code .../Svcstatus/granted} and {@code .../Svcstatus/withdrawn}. Anything else is taken
	 * as not in force.
	 */
	private static final Set<String> IN_FORCE_STATUS_SEGMENTS = Set.of("granted", "notified");

	/** Whether a service with {@code status} (null when absent) supplies trust anchors. */
	static boolean isInForce(String status) {
		if (status == null) {
			return true;
		}
		String segment = status.substring(status.lastIndexOf('/') + 1);
		return IN_FORCE_STATUS_SEGMENTS.contains(segment.toLowerCase(java.util.Locale.ROOT));
	}

	@Override
	@PreEnvironment(required = LOTE)
	@PostEnvironment(required = LOTE)
	public Environment evaluate(Environment env) {
		LoteSlot slot = getSlot(env);
		JWSObject jws = parseJws(env);
		List<X509Certificate> chain = getSignerCertificateChain(jws);
		X509Certificate signer = chain.get(0);
		verifyJwtSignatureWithX5cLeafCert(getLoteValue(env), chain);
		String configuredPem = env.getString("config", LoteSlot.SIGNING_CERTIFICATE_CONFIG_KEY);
		if (configuredPem != null && !configuredPem.isBlank()) {
			try {
				ValidateLoteSignerAgainstConfiguredCertificate.checkSigner(chain,
					X509CertificateUtil.parseTrustAnchorPem(configuredPem));
			} catch (X509CertificateUtil.X5cCertificateChainException e) {
				throw error("The list of trusted entities is not sealed by the configured '" + LoteSlot.SIGNING_CERTIFICATE_FIELD_LABEL
					+ "'; its trust anchors cannot be used: " + e.getMessage(),
					args("signer_certificate_subject", signer.getSubjectX500Principal().getName()));
			}
		}

		JsonObject lote = getLotePayload(env).getAsJsonObject("LoTE");
		JsonArray anchors = new JsonArray();
		Set<String> seen = new LinkedHashSet<>();
		int skippedServices = 0;
		List<String> notInForce = new ArrayList<>();
		List<String> organizationMismatches = new ArrayList<>();
		JsonElement entities = lote.get("TrustedEntitiesList");
		if (entities != null && entities.isJsonArray()) {
			for (JsonElement entityEl : entities.getAsJsonArray()) {
				if (!entityEl.isJsonObject()) {
					continue;
				}
				JsonObject entity = entityEl.getAsJsonObject();
				String teName = entity.get("TrustedEntityInformation") != null && entity.get("TrustedEntityInformation").isJsonObject()
					? firstValue(entity.getAsJsonObject("TrustedEntityInformation").get("TEName")) : null;
				JsonElement services = entity.get("TrustedEntityServices");
				if (services == null || !services.isJsonArray()) {
					continue;
				}
				for (JsonElement serviceEl : services.getAsJsonArray()) {
					JsonElement infoEl = serviceEl.isJsonObject() ? serviceEl.getAsJsonObject().get("ServiceInformation") : null;
					if (infoEl == null || !infoEl.isJsonObject()) {
						continue;
					}
					JsonObject service = infoEl.getAsJsonObject();
					JsonElement typeEl = service.get("ServiceTypeIdentifier");
					String serviceType = typeEl != null && typeEl.isJsonPrimitive() ? OIDFJSON.getString(typeEl) : null;
					if (!slot.acceptsServiceType(serviceType)) {
						skippedServices++;
						continue;
					}
					JsonElement statusEl = service.get("ServiceStatus");
					String status = statusEl != null && statusEl.isJsonPrimitive() ? OIDFJSON.getString(statusEl) : null;
					if (!isInForce(status)) {
						notInForce.add((teName == null ? "(unnamed entity)" : teName) + ": " + status);
						continue;
					}
					JsonElement identity = service.get("ServiceDigitalIdentity");
					JsonElement certs = identity != null && identity.isJsonObject() ? identity.getAsJsonObject().get("X509Certificates") : null;
					if (certs == null || !certs.isJsonArray()) {
						continue;
					}
					for (JsonElement certEl : certs.getAsJsonArray()) {
						JsonElement val = certEl.isJsonObject() ? certEl.getAsJsonObject().get("val") : null;
						if (val == null || !val.isJsonPrimitive()) {
							continue;
						}
						String b64 = OIDFJSON.getString(val);
						X509Certificate cert = X509CertificateUtil.parseBase64Der(b64);
						if (cert == null) {
							// reported by ValidateLoteStructure; an unparseable entry cannot be an anchor
							continue;
						}
						String key = serviceType + "|" + b64;
						if (!seen.add(key)) {
							continue;
						}
						String organization = X509CertificateUtil.subjectRdnValue(cert, BCStyle.O);
						if (teName != null && !teName.equals(organization)) {
							organizationMismatches.add("TEName '" + teName + "', organizationName "
								+ (organization == null ? "absent" : "'" + organization + "'"));
						}
						JsonObject anchor = new JsonObject();
						anchor.addProperty("cert", b64);
						anchor.addProperty("subject", cert.getSubjectX500Principal().getName());
						anchor.addProperty("service_type", serviceType);
						anchor.addProperty("te_name", teName);
						anchors.add(anchor);
					}
				}
			}
		}

		env.putObject(LOTE, "anchors", wrap(anchors));

		JsonArray summary = new JsonArray();
		for (JsonElement anchor : anchors) {
			summary.add(OIDFJSON.getString(anchor.getAsJsonObject().get("subject"))
				+ " (" + OIDFJSON.getString(anchor.getAsJsonObject().get("service_type")) + ")");
		}
		if (!organizationMismatches.isEmpty()) {
			log("Listed certificates whose subject organizationName does not strictly match the trusted entity's TEName;"
					+ " ETSI TS 119 602 clause 6.6.3.0 says they should, and they are used as trust anchors regardless",
				args("mismatches", organizationMismatches));
		}
		if (anchors.isEmpty()) {
			logSuccess("The list of trusted entities lists no trust anchors for this slot; nothing will chain to it",
				args("slot", slot.name(), "skipped_services_of_other_types", skippedServices,
					"skipped_services_not_in_force", notInForce));
		} else {
			logSuccess("Extracted the trust anchors from the list of trusted entities",
				args("slot", slot.name(), "anchor_count", anchors.size(), "anchors", summary,
					"skipped_services_of_other_types", skippedServices,
					"skipped_services_not_in_force", notInForce));
		}
		return env;
	}

	/** Environment objects hold objects, so the anchor array travels inside one. */
	private static JsonObject wrap(JsonArray anchors) {
		JsonObject holder = new JsonObject();
		holder.add("list", anchors);
		return holder;
	}
}
