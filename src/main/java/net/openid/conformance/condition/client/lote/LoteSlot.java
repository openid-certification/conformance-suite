package net.openid.conformance.condition.client.lote;

import com.google.common.collect.ImmutableSet;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.LoteList;

/**
 * The lists of trusted entities (ETSI TS 119 602 LoTEs) the suite can take trust anchors from,
 * one slot per list whose anchors a chain check consumes (the suite publishes a WRPRC providers
 * list too, but validates no registration certificate against one). Each slot owns one
 * environment object ({@link #envKey}) holding everything about that list — URL, fetched value,
 * parsed header and payload, extracted anchors — so that the conditions, which all work on the
 * object mapped to {@code "lote"}, stay slot-agnostic; the sequence maps the key before calling
 * them and unmaps it afterwards.
 *
 * <p>The EU profiles (TS 119 602 Annexes D to G, and the Commission's mDL providers list
 * specification) fix the list type, the status determination approach, the scheme rules URI and
 * the service type identifiers per list. Each slot takes them from the {@link LoteList} the suite
 * publishes for the same role, so the lists the suite serves and the lists it accepts cannot
 * disagree; the suite publishes every service type a profile defines.
 */
public enum LoteSlot {

	PID("lote_pid", "credential.pid_lote_url", "PID providers LoTE URL", "Credential Issuer", LoteList.PID_PROVIDERS),

	MDL("lote_mdl", "credential.mdl_lote_url", "mDL providers LoTE URL", "Credential Issuer", LoteList.MDL_PROVIDERS),

	WALLET("lote_wallet", "client_attestation.wallet_lote_url", "Wallet providers LoTE URL", "Client Attestation",
		LoteList.WALLET_PROVIDERS),

	WRPAC("lote_wrpac", "client.wrpac_lote_url", "WRPAC providers LoTE URL", "Client", LoteList.WRPAC_PROVIDERS);

	/** The slots' environment keys as constants, for use in annotations; each equals the slot's envKey. */
	public static final String PID_ENV_KEY = "lote_pid";
	public static final String MDL_ENV_KEY = "lote_mdl";
	public static final String WALLET_ENV_KEY = "lote_wallet";
	public static final String WRPAC_ENV_KEY = "lote_wrpac";

	/** The service type identifier suffix of the services whose certificates sign what the list is about. */
	public static final String ISSUANCE_SUFFIX = "/Issuance";

	/** The service type identifier suffix of the services whose certificates sign status information. */
	public static final String REVOCATION_SUFFIX = "/Revocation";

	/** The configuration field holding the scheme operator's sealing certificate, shared by every slot. */
	public static final String SIGNING_CERTIFICATE_CONFIG_KEY = "lote.signing_certificate_pem";
	public static final String SIGNING_CERTIFICATE_FIELD_LABEL = "LoTE signing certificate";
	public static final String SIGNING_CERTIFICATE_SECTION_LABEL = "Lists of Trusted Entities (ETSI TS 119 602)";

	/** The environment key the conditions see as {@code "lote"} while this slot is mapped. */
	public final String envKey;

	/** The test configuration field holding the list's URL. */
	public final String configKey;

	/** The field's label on the configuration form, for error messages. */
	public final String fieldLabel;

	/** The field's section on the configuration form, for error messages. */
	public final String sectionLabel;

	/** The LoTEType the profile fixes for this list. */
	public final String expectedLoteType;

	/** The StatusDeterminationApproach the profile fixes for this list. */
	public final String expectedStatusDeterminationApproach;

	/** The SchemeTypeCommunityRules URI the profile fixes for this list. */
	public final String expectedSchemeTypeCommunityRules;

	/** The service type identifiers the profile allows. */
	public final ImmutableSet<String> allowedServiceTypes;

	LoteSlot(String envKey, String configKey, String fieldLabel, String sectionLabel, LoteList profile) {
		this.envKey = envKey;
		this.configKey = configKey;
		this.fieldLabel = fieldLabel;
		this.sectionLabel = sectionLabel;
		this.expectedLoteType = profile.loteType;
		this.expectedStatusDeterminationApproach = profile.statusDeterminationApproach;
		this.expectedSchemeTypeCommunityRules = profile.schemeTypeCommunityRules;
		this.allowedServiceTypes = profile.services.stream()
			.map(LoteList.Service::serviceType)
			.collect(ImmutableSet.toImmutableSet());
	}

	/** How the field is referred to in messages to the tester. */
	public String fieldDescription() {
		return "'" + fieldLabel + "' field in the '" + sectionLabel + "' section of the test configuration";
	}

	/** The list URL the test configuration sets for this slot, trimmed, or null; the form keeps a cleared field as "". */
	public String configuredUrl(Environment env) {
		String url = env.getString("config", configKey);
		return url == null || url.isBlank() ? null : url.trim();
	}

	/** Whether the test configuration names a list URL for this slot. */
	public boolean isConfigured(Environment env) {
		return configuredUrl(env) != null;
	}

	/** The scheme operator's sealing certificate the test configuration sets, PEM, or null when the field is blank. */
	public static String configuredSigningCertificatePem(Environment env) {
		String pem = env.getString("config", SIGNING_CERTIFICATE_CONFIG_KEY);
		return pem == null || pem.isBlank() ? null : pem;
	}

	/** Whether {@code serviceType} is one this slot takes anchors from. */
	public boolean acceptsServiceType(String serviceType) {
		return allowedServiceTypes.contains(serviceType);
	}
}
