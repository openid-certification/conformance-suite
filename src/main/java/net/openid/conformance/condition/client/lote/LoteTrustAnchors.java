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
 * listed entity the chain led to and a failure can list what was tried. A configured list that
 * contributes no anchor of the wanted type (an empty list, or one listing only other services)
 * still counts as a trust source: it trusts nobody, so a chain must not then pass on the
 * trust-anchor-free legacy walk.
 */
public final class LoteTrustAnchors {

	private LoteTrustAnchors() {
	}

	/**
	 * The anchors and, in the same order, a human-readable source for each, plus the configuration
	 * fields of the lists of trusted entities that were consulted, whether or not they contributed.
	 */
	public record TrustAnchorSet(List<X509Certificate> anchors, List<String> sources, List<String> configuredLists) {

		/** A set from explicit anchors, with no list of trusted entities consulted. */
		public TrustAnchorSet(List<X509Certificate> anchors, List<String> sources) {
			this(anchors, sources, List.of());
		}

		public boolean isEmpty() {
			return anchors.isEmpty();
		}

		/**
		 * Whether a list of trusted entities is configured for this purpose but yields no anchor: it
		 * trusts nothing, so no chain may be accepted.
		 */
		public boolean trustsNothing() {
			return anchors.isEmpty() && !configuredLists.isEmpty();
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
		List<String> configuredLists = new ArrayList<>();
		for (LoteSlot slot : slots) {
			String url = env.getString("config", slot.configKey);
			if (env.containsObject(slot.envKey) || (url != null && !url.isBlank())) {
				configuredLists.add(slot.fieldDescription());
			}
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
					+ ", " + shortServiceType(serviceType)
					+ ", " + cert.getSubjectX500Principal().getName());
			}
		}
		return new TrustAnchorSet(anchors, sources, configuredLists);
	}

	/**
	 * The last two path segments of a service type identifier other than "SvcType", which the ETSI
	 * ({@code .../SvcType/PID/Issuance}) and the Commission's mDL ({@code .../mDL/SvcType/Issuance})
	 * identifiers put in different places: "PID/Issuance", "mDL/Issuance".
	 */
	static String shortServiceType(String serviceType) {
		List<String> segments = new ArrayList<>(List.of(serviceType.split("/")));
		segments.removeIf(segment -> segment.isEmpty() || segment.equals("SvcType"));
		return String.join("/", segments.subList(Math.max(0, segments.size() - 2), segments.size()));
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
