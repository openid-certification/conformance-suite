package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Takes the trust anchors out of the list of trusted entities: every X.509 certificate in a
 * ServiceDigitalIdentity whose service type the slot accepts (ETSI TS 119 602 clause 6.6.3), with
 * the trusted entity's name and the service type, so the chain checks can say which listed
 * entity a certificate chains to. Re-verifies the JWS first: a corrupted or re-signed list must
 * not silently drive a trust decision, whatever the earlier signature check's severity was. An
 * empty list (no TrustedEntitiesList, clause 6.4.0) yields no anchors; that is the shape of the
 * event's negative-test lists, so it is not an error here but a failed chain check later.
 *
 * <p>Writes {@code anchors}: an array of {@code {cert, subject, service_type, te_name}} with the
 * certificate as base64 DER.
 */
public class ExtractLoteTrustAnchors extends AbstractLoteCondition {

	@Override
	@PreEnvironment(required = LOTE)
	@PostEnvironment(required = LOTE)
	public Environment evaluate(Environment env) {
		LoteSlot slot = getSlot(env);
		JWSObject jws = parseJws(env);
		X509Certificate signer = getSignerCertificateChain(jws).get(0);
		try {
			JWSVerifier verifier = new DefaultJWSVerifierFactory().createJWSVerifier(jws.getHeader(), signer.getPublicKey());
			if (!jws.verify(verifier)) {
				throw error("The signature of the list of trusted entities does not verify; its trust anchors cannot be used");
			}
		} catch (Exception e) {
			throw error("The signature of the list of trusted entities could not be verified; its trust anchors cannot be used", e);
		}

		JsonObject lote = getLotePayload(env).getAsJsonObject("LoTE");
		JsonArray anchors = new JsonArray();
		Set<String> seen = new LinkedHashSet<>();
		int skippedServices = 0;
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
						String b64 = OIDFJSON.getString(val).replaceAll("\\s", "");
						X509Certificate cert;
						try {
							cert = (X509Certificate) CertificateFactory.getInstance("X.509")
								.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(b64)));
						} catch (Exception e) {
							// reported by ValidateLoteStructure; an unparseable entry cannot be an anchor
							continue;
						}
						String key = serviceType + "|" + b64;
						if (!seen.add(key)) {
							continue;
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
		if (anchors.isEmpty()) {
			logSuccess("The list of trusted entities lists no trust anchors for this slot; nothing will chain to it",
				args("slot", slot.name(), "skipped_services_of_other_types", skippedServices));
		} else {
			logSuccess("Extracted the trust anchors from the list of trusted entities",
				args("slot", slot.name(), "anchor_count", anchors.size(), "anchors", summary,
					"skipped_services_of_other_types", skippedServices));
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
