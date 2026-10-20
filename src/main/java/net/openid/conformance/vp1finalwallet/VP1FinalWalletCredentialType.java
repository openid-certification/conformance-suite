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
	 * The suite's built-in DCQL queries. Each is a resource beside the credential type's minimal
	 * query, named by appending its suffix to that query's stem.
	 */
	public enum BuiltInDcqlQuery {
		/** The minimal query the happy flow sends. */
		DEFAULT(""),
		/** Every mandatory data element of the credential type. */
		ALL_MANDATORY("-all-mandatory"),
		/**
		 * Whether the holder is aged 18 or over, offering the data elements that reveal the least
		 * first. Only the credential types with age data elements have this query; the module that
		 * uses it excludes the others.
		 */
		AGE_OVER_18("-age-over-18"),
		/** The first element of the PID 'nationalities' array. Only the SD-JWT VC PID has this query. */
		NATIONALITIES_FIRST("-nationalities-first"),
		/** Every element of the PID 'nationalities' array. Only the SD-JWT VC PID has this query. */
		NATIONALITIES_ALL("-nationalities-all"),
		/**
		 * One member of the PID 'place_of_birth' object, offering 'locality', 'region' and 'country'
		 * in that order. Only the SD-JWT VC PID has this query.
		 */
		PLACE_OF_BIRTH_MEMBER("-place-of-birth-member");

		private final String resourceSuffix;

		BuiltInDcqlQuery(String resourceSuffix) {
			this.resourceSuffix = resourceSuffix;
		}
	}

	/**
	 * The resource holding the given built-in DCQL query for this credential type in the given
	 * format, or null when the tester supplies the query (a custom credential has no built-in
	 * queries).
	 */
	public String getDcqlResource(VP1FinalWalletCredentialFormat format, BuiltInDcqlQuery query) {
		String stem = dcqlResourceStem(format);
		return stem == null ? null : stem + query.resourceSuffix + ".json";
	}

	@Override
	public String toString() {
		return variantValue;
	}
}
