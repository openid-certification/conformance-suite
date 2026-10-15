package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.X509CertificateUtil;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Collects the trust anchors a certificate chain may terminate at: the PEM the tester configured
 * for the purpose, if any, plus the anchors extracted from the lists of trusted entities
 * ({@link LoteSlot}s) that cover that purpose, filtered by service type (issuance services for the
 * certificates that sign the thing itself, revocation services for the certificates that sign
 * status information). Each anchor remembers where it came from, so a chain check can say which
 * listed entity the chain led to and a failure can list what was tried.
 */
public final class LoteTrustAnchors {

	private LoteTrustAnchors() {
	}

	/** The anchors and, in the same order, a human-readable source for each. */
	public record TrustAnchorSet(List<X509Certificate> anchors, List<String> sources) {

		public boolean isEmpty() {
			return anchors.isEmpty();
		}

		/** The sources as a JSON array for log entries. */
		public JsonArray sourcesJson() {
			JsonArray array = new JsonArray();
			sources.forEach(array::add);
			return array;
		}

		/** The source of {@code anchor}, or null when it is not in the set. */
		public String sourceOf(X509Certificate anchor) {
			int i = anchors.indexOf(anchor);
			return i < 0 ? null : sources.get(i);
		}
	}

	/**
	 * @param env the environment
	 * @param configuredPem the trust anchor the tester configured for this purpose, PEM, or null
	 * @param configuredLabel the configuration form's label for that field, for the source text
	 * @param serviceTypeSuffix {@link LoteSlot#ISSUANCE_SUFFIX} or {@link LoteSlot#REVOCATION_SUFFIX}
	 * @param slots the lists whose anchors cover this purpose
	 * @throws X509CertificateUtil.X5cCertificateChainException when the configured PEM is not a certificate
	 */
	public static TrustAnchorSet resolve(Environment env, String configuredPem, String configuredLabel,
			String serviceTypeSuffix, LoteSlot... slots) throws X509CertificateUtil.X5cCertificateChainException {
		List<X509Certificate> anchors = new ArrayList<>();
		List<String> sources = new ArrayList<>();
		if (configuredPem != null && !configuredPem.isBlank()) {
			anchors.add(X509CertificateUtil.parseTrustAnchorPem(configuredPem));
			sources.add("'" + configuredLabel + "' from the test configuration");
		}
		for (LoteSlot slot : slots) {
			JsonElement list = env.getElementFromObject(slot.envKey, "anchors.list");
			if (list == null || !list.isJsonArray()) {
				continue;
			}
			for (JsonElement anchorEl : list.getAsJsonArray()) {
				JsonObject anchor = anchorEl.getAsJsonObject();
				String serviceType = OIDFJSON.getString(anchor.get("service_type"));
				if (serviceType == null || !serviceType.endsWith(serviceTypeSuffix)) {
					continue;
				}
				X509Certificate cert;
				try {
					cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
						new ByteArrayInputStream(Base64.getDecoder().decode(OIDFJSON.getString(anchor.get("cert")))));
				} catch (Exception e) {
					// ExtractLoteTrustAnchors only stores certificates it parsed
					throw new IllegalStateException("A stored trust anchor of " + slot.name() + " does not parse", e);
				}
				if (anchors.contains(cert)) {
					continue;
				}
				anchors.add(cert);
				JsonElement teName = anchor.get("te_name");
				sources.add(slot.fieldLabel + ": " + (teName == null || teName.isJsonNull() ? "(unnamed entity)" : OIDFJSON.getString(teName))
					+ ", " + serviceType.substring(serviceType.lastIndexOf('/', serviceType.lastIndexOf('/') - 1) + 1)
					+ ", " + cert.getSubjectX500Principal().getName());
			}
		}
		return new TrustAnchorSet(anchors, sources);
	}

	/** Whether any of {@code slots} holds at least one extracted anchor. */
	public static boolean anySlotHasAnchors(Environment env, LoteSlot... slots) {
		for (LoteSlot slot : slots) {
			JsonElement list = env.getElementFromObject(slot.envKey, "anchors.list");
			if (list != null && list.isJsonArray() && !list.getAsJsonArray().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/** Whether the test configuration names a list URL for any of {@code slots}. */
	public static boolean anySlotConfigured(Environment env, LoteSlot... slots) {
		for (LoteSlot slot : slots) {
			String url = env.getString("config", slot.configKey);
			if (url != null && !url.isBlank()) {
				return true;
			}
		}
		return false;
	}
}
