package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.variant.VariantParameter;

/**
 * Which credential the wallet is asked to present. For the well known credential types the suite
 * uses one of its own DCQL queries, so the tester does not have to write one; 'custom' uses the
 * DCQL query from the test configuration instead.
 *
 * 'custom' is the default so that test plans created before this parameter existed - which always
 * supply their own DCQL query - continue to work.
 */
@VariantParameter(
	name = "credential_type",
	displayName = "Credential Type",
	description = "The credential the wallet will be asked to present. Select 'custom' to supply your own DCQL query in the test configuration.",
	defaultValue = "custom",
	sortOrder = 80
)
public enum VP1FinalWalletCredentialType {

	EUDI_PID("eudi_pid", "/json/dcql/vp1final-wallet-eudi-pid.json",
		"/json/dcql/vp1final-wallet-eudi-pid-all-mandatory.json"),
	MDL("mdl", "/json/dcql/vp1final-wallet-mdl.json",
		"/json/dcql/vp1final-wallet-mdl-all-mandatory.json"),
	PHOTO_ID("photoid", "/json/dcql/vp1final-wallet-photoid.json",
		"/json/dcql/vp1final-wallet-photoid-all-mandatory.json"),
	CUSTOM("custom", null, null);

	private final String variantValue;
	private final String dcqlResource;
	private final String allMandatoryClaimsDcqlResource;

	private VP1FinalWalletCredentialType(String variantValue, String dcqlResource,
			String allMandatoryClaimsDcqlResource) {
		this.variantValue = variantValue;
		this.dcqlResource = dcqlResource;
		this.allMandatoryClaimsDcqlResource = allMandatoryClaimsDcqlResource;
	}

	/** The suite's built-in DCQL query for this credential type, or null when the tester supplies one. */
	public String getDcqlResource() {
		return dcqlResource;
	}

	/**
	 * The suite's built-in DCQL query requesting every mandatory data element of this credential
	 * type, or null when the tester supplies the query (there is no known mandatory set for a
	 * custom credential).
	 */
	public String getAllMandatoryClaimsDcqlResource() {
		return allMandatoryClaimsDcqlResource;
	}

	@Override
	public String toString() {
		return variantValue;
	}
}
