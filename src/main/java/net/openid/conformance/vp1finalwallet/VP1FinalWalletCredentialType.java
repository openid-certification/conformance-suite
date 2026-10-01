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

	EUDI_PID("eudi_pid", "/json/dcql/vp1final-wallet-eudi-pid", "/json/dcql/vp1final-wallet-eudi-pid-mdoc"),
	MDL("mdl", null, "/json/dcql/vp1final-wallet-mdl"),
	PHOTO_ID("photoid", null, "/json/dcql/vp1final-wallet-photoid"),
	CUSTOM("custom", null, null);

	private final String variantValue;
	private final String sdJwtDcqlResourceStem;
	private final String mdocDcqlResourceStem;

	private VP1FinalWalletCredentialType(String variantValue, String sdJwtDcqlResourceStem,
			String mdocDcqlResourceStem) {
		this.variantValue = variantValue;
		this.sdJwtDcqlResourceStem = sdJwtDcqlResourceStem;
		this.mdocDcqlResourceStem = mdocDcqlResourceStem;
	}

	private String dcqlResourceStem(VP1FinalWalletCredentialFormat format) {
		return format == VP1FinalWalletCredentialFormat.ISO_MDL ? mdocDcqlResourceStem : sdJwtDcqlResourceStem;
	}

	/**
	 * The suite's built-in DCQL query for this credential type in the given format, or null when
	 * the tester supplies one.
	 */
	public String getDcqlResource(VP1FinalWalletCredentialFormat format) {
		String stem = dcqlResourceStem(format);
		return stem == null ? null : stem + ".json";
	}

	/**
	 * The suite's built-in DCQL query requesting every mandatory data element of this credential
	 * type in the given format, or null when the tester supplies the query (there is no known
	 * mandatory set for a custom credential).
	 */
	public String getAllMandatoryClaimsDcqlResource(VP1FinalWalletCredentialFormat format) {
		String stem = dcqlResourceStem(format);
		return stem == null ? null : stem + "-all-mandatory.json";
	}

	/**
	 * The suite's built-in DCQL query asking whether the holder is aged 18 or over, in the given
	 * format, or null when the tester supplies the query. Only the credential types with age data
	 * elements have such a query; the module that uses it excludes the others.
	 */
	public String getAgeOver18DcqlResource(VP1FinalWalletCredentialFormat format) {
		String stem = dcqlResourceStem(format);
		return stem == null ? null : stem + "-age-over-18.json";
	}

	@Override
	public String toString() {
		return variantValue;
	}
}
