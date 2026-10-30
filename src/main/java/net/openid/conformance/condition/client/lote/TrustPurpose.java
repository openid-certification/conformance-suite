package net.openid.conformance.condition.client.lote;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.X509CertificateUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The certificate chains the suite validates against a trust source, with everything that defines
 * that source: the configuration field the tester can put a trust anchor PEM in, where the PEM is
 * registered in the environment, and which lists of trusted entities ({@link LoteSlot}) and service
 * types can supply anchors instead. The register, ensure and validate conditions of a chain all
 * read their trust source from here.
 */
public enum TrustPurpose {

	CREDENTIAL("credential signing certificate", "Credential Trust Anchor", "Credential Issuer",
		List.of("credential.trust_anchor_pem"), "credential_trust_anchor_pem", null, false,
		LoteSlot.ISSUANCE_SUFFIX, LoteSlot.PID, LoteSlot.MDL),

	// a status list PEM may sit beside the PID or mDL providers list, which stays the credential's trust
	// source: the PEM then replaces only the list's revocation services
	STATUS_LIST("status list signing certificate", "Status List Trust Anchor", "Credential Issuer",
		List.of("credential.status_list_trust_anchor_pem"), "status_list_trust_anchor_pem", null, true,
		LoteSlot.REVOCATION_SUFFIX, LoteSlot.PID, LoteSlot.MDL),

	// the legacy vci.* keys are read as a fallback for a configuration that reaches here without
	// ConfigMigration having moved them to client_attestation.*
	CLIENT_ATTESTATION("client attestation signing certificate", "Client Attestation Trust Anchor", "Client Attestation",
		List.of("client_attestation.trust_anchor", "vci.client_attestation_trust_anchor"), "client_attestation_trust_anchor_pem", null, false,
		LoteSlot.ISSUANCE_SUFFIX, LoteSlot.WALLET),

	KEY_ATTESTATION("key attestation signing certificate", "Key Attestation Trust Anchor", "Key Attestation",
		List.of("client_attestation.key_attestation_trust_anchor_pem", "vci.key_attestation_trust_anchor_pem"), "key_attestation_trust_anchor_pem", null, false,
		LoteSlot.ISSUANCE_SUFFIX, LoteSlot.WALLET),

	// a RICAL (ISO/IEC 18013-5 second edition Annex F) is the request object's trust source when set
	REQUEST_OBJECT("request object signing certificate", "Request Object Trust Anchor", "Client",
		List.of("client.request_object_trust_anchor_pem"), "client_request_object_trust_anchor_pem",
		new OtherSource(ImmutableList.of("client.rical", "client.rical_url"), "'RICAL' or 'RICAL URL'", "Client"), false,
		LoteSlot.ISSUANCE_SUFFIX, LoteSlot.WRPAC),

	REGISTRATION_CERTIFICATE("registration certificate signing certificate", "Registration Certificate Trust Anchor", "Credential Issuer",
		List.of("credential.registration_certificate_trust_anchor_pem"), "registration_certificate_trust_anchor_pem", null, false,
		LoteSlot.ISSUANCE_SUFFIX, LoteSlot.WRPRC);

	/** A trust source that is neither a PEM nor a list of trusted entities: the configuration keys that set it, and how the form labels them. */
	@Immutable
	public record OtherSource(ImmutableList<String> configKeys, String fieldLabels, String sectionLabel) implements Serializable {

		/** Whether any of the keys is set; the form keeps a cleared field as "". */
		boolean isConfigured(Environment env) {
			for (String key : configKeys) {
				String value = env.getString("config", key);
				if (value != null && !value.isBlank()) {
					return true;
				}
			}
			return false;
		}

		String fieldDescription() {
			return fieldLabels + " field in the '" + sectionLabel + "' section of the test configuration";
		}
	}

	/** The certificate the chain leads from, for messages. */
	public final String certificate;

	/** The configuration form's label and section of the trust anchor PEM field. */
	public final String pemFieldLabel;
	public final String pemSectionLabel;

	/** The configuration keys of the PEM field, current first, then legacy ones read as a fallback. */
	private final ImmutableList<String> pemConfigKeys;

	/** The top-level environment string the configured PEM is registered under. */
	private final String registeredKey;

	/** A further trust source the configuration can set for this chain, or null. */
	private final OtherSource otherSource;

	/** Whether a configured PEM may sit beside a configured list instead of being the one trust source. */
	public final boolean pemMayAccompanyLists;

	/** {@link LoteSlot#ISSUANCE_SUFFIX} or {@link LoteSlot#REVOCATION_SUFFIX}. */
	private final String serviceTypeSuffix;

	/** The lists of trusted entities whose anchors cover this chain. */
	private final ImmutableList<LoteSlot> slots;

	TrustPurpose(String certificate, String pemFieldLabel, String pemSectionLabel, List<String> pemConfigKeys,
			String registeredKey, OtherSource otherSource, boolean pemMayAccompanyLists, String serviceTypeSuffix, LoteSlot... slots) {
		this.certificate = certificate;
		this.pemFieldLabel = pemFieldLabel;
		this.pemSectionLabel = pemSectionLabel;
		this.pemConfigKeys = ImmutableList.copyOf(pemConfigKeys);
		this.registeredKey = registeredKey;
		this.otherSource = otherSource;
		this.pemMayAccompanyLists = pemMayAccompanyLists;
		this.serviceTypeSuffix = serviceTypeSuffix;
		this.slots = ImmutableList.copyOf(slots);
	}

	/** How the PEM field is referred to in messages to the tester. */
	public String pemFieldDescription() {
		return "'" + pemFieldLabel + "' field in the '" + pemSectionLabel + "' section of the test configuration";
	}

	/** The PEM the test configuration sets for this chain, or null; the form keeps a cleared field as "". */
	public String configuredPem(Environment env) {
		for (String key : pemConfigKeys) {
			String pem = env.getString("config", key);
			if (pem != null) {
				return pem.isBlank() ? null : pem;
			}
		}
		return null;
	}

	/** The PEM a register condition stored for the chain checks, or null. */
	public String registeredPem(Environment env) {
		return env.getString(registeredKey);
	}

	public void register(Environment env, String pem) {
		env.putString(registeredKey, pem);
	}

	/**
	 * The trust sources the test configuration sets for this chain, as field descriptions: the other
	 * source first, then the PEM, then each list; see {@link LoteTrustAnchors#configuredSources}.
	 */
	public List<String> configuredSources(Environment env) {
		List<String> sources = new ArrayList<>();
		if (otherSource != null && otherSource.isConfigured(env)) {
			sources.add(otherSource.fieldDescription());
		}
		sources.addAll(LoteTrustAnchors.configuredSources(env, configuredPem(env), pemFieldDescription(), slots));
		return sources;
	}

	/**
	 * The anchors to validate the chain against: the registered PEM when set, otherwise the anchors of
	 * the lists of trusted entities.
	 *
	 * @throws X509CertificateUtil.X5cCertificateChainException when the registered PEM is not a certificate
	 */
	public LoteTrustAnchors.TrustAnchorSet resolve(Environment env) throws X509CertificateUtil.X5cCertificateChainException {
		return LoteTrustAnchors.resolve(env, registeredPem(env), pemFieldLabel, serviceTypeSuffix, slots);
	}

	/** The anchors of the lists of trusted entities covering this chain, whatever PEM is registered. */
	public LoteTrustAnchors.TrustAnchorSet listAnchors(Environment env) {
		return LoteTrustAnchors.fromLists(env, serviceTypeSuffix, slots);
	}

	/** The fields that can supply this chain's trust source, for a message saying none is set. */
	public String fieldsDescription() {
		StringBuilder sb = new StringBuilder();
		if (otherSource != null) {
			sb.append(otherSource.fieldLabels()).append(" in the '").append(otherSource.sectionLabel()).append("' section, ");
		}
		sb.append("'").append(pemFieldLabel).append("' in the '").append(pemSectionLabel).append("' section");
		for (LoteSlot slot : slots) {
			sb.append(", '").append(slot.fieldLabel).append("' in the '").append(slot.sectionLabel).append("' section");
		}
		return sb.toString();
	}
}
