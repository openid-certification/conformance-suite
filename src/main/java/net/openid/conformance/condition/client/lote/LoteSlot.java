package net.openid.conformance.condition.client.lote;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * The lists of trusted entities (ETSI TS 119 602 LoTEs) the suite can take trust anchors from,
 * one slot per role the EUDI Wallet ecosystem publishes a list for. Each slot owns one
 * environment object ({@link #envKey}) holding everything about that list — URL, fetched value,
 * parsed header and payload, extracted anchors — so that the conditions, which all work on the
 * object mapped to {@code "lote"}, stay slot-agnostic; the sequence maps the key before calling
 * them and unmaps it afterwards.
 *
 * <p>The EU profiles (TS 119 602 Annexes D to G) fix the list type and the service type
 * identifiers per list. The mDL providers list has no profile in the specification; the European
 * Commission defines its list type and service types in the eIDAS Dashboard's mDL Providers List
 * Specification (https://eidas.ec.europa.eu/efda/wallet/lists-of-trusted-entities/mdl-providers).
 */
public enum LoteSlot {

	PID("lote_pid", "credential.pid_lote_url", "PID providers LoTE URL", "Credential Issuer",
		"http://uri.etsi.org/19602/LoTEType/EUPIDProvidersList",
		Set.of("http://uri.etsi.org/19602/SvcType/PID/Issuance", "http://uri.etsi.org/19602/SvcType/PID/Revocation")),

	MDL("lote_mdl", "credential.mdl_lote_url", "mDL providers LoTE URL", "Credential Issuer",
		"http://trust.ec.europa.eu/lists/mDL/mDLProvidersListType",
		Set.of("http://trust.ec.europa.eu/lists/mDL/SvcType/Issuance", "http://trust.ec.europa.eu/lists/mDL/SvcType/Revocation")),

	WALLET("lote_wallet", "client_attestation.wallet_lote_url", "Wallet providers LoTE URL", "Client Attestation",
		"http://uri.etsi.org/19602/LoTEType/EUWalletProvidersList",
		Set.of("http://uri.etsi.org/19602/SvcType/WalletSolution/Issuance", "http://uri.etsi.org/19602/SvcType/WalletSolution/Revocation")),

	WRPAC("lote_wrpac", "client.wrpac_lote_url", "WRPAC providers LoTE URL", "Client",
		"http://uri.etsi.org/19602/LoTEType/EUWRPACProvidersList",
		Set.of("http://uri.etsi.org/19602/SvcType/WRPAC/Issuance", "http://uri.etsi.org/19602/SvcType/WRPAC/Revocation")),

	WRPRC("lote_wrprc", "client.wrprc_lote_url", "WRPRC providers LoTE URL", "Client",
		"http://uri.etsi.org/19602/LoTEType/EUWRPRCProvidersList",
		Set.of("http://uri.etsi.org/19602/SvcType/WRPRC/Issuance", "http://uri.etsi.org/19602/SvcType/WRPRC/Revocation"));

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

	/** The service type identifiers the profile allows. */
	public final ImmutableSet<String> allowedServiceTypes;

	LoteSlot(String envKey, String configKey, String fieldLabel, String sectionLabel,
			String expectedLoteType, Set<String> allowedServiceTypes) {
		this.allowedServiceTypes = ImmutableSet.copyOf(allowedServiceTypes);
		this.envKey = envKey;
		this.configKey = configKey;
		this.fieldLabel = fieldLabel;
		this.sectionLabel = sectionLabel;
		this.expectedLoteType = expectedLoteType;
	}

	/** How the field is referred to in messages to the tester. */
	public String fieldDescription() {
		return "'" + fieldLabel + "' field in the '" + sectionLabel + "' section of the test configuration";
	}

	/** Whether {@code serviceType} is one this slot takes anchors from. */
	public boolean acceptsServiceType(String serviceType) {
		return allowedServiceTypes.contains(serviceType);
	}
}
