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

	EUDI_PID("eudi_pid", "/json/dcql/vp1final-wallet-eudi-pid.json"),
	MDL("mdl", "/json/dcql/vp1final-wallet-mdl.json"),
	CUSTOM("custom", null);

	private final String variantValue;
	private final String dcqlResource;

	private VP1FinalWalletCredentialType(String variantValue, String dcqlResource) {
		this.variantValue = variantValue;
		this.dcqlResource = dcqlResource;
	}

	/** The suite's built-in DCQL query for this credential type, or null when the tester supplies one. */
	public String getDcqlResource() {
		return dcqlResource;
	}

	@Override
	public String toString() {
		return variantValue;
	}
}
