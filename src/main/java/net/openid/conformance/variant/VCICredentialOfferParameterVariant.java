package net.openid.conformance.variant;

@VariantParameter(
	name = "vci_credential_offer_variant",
	displayName = "Credential Offer Variant",
	description = "VCI Credential Offer Parameter Variant to be used. 'by_value' is the most commonly used.",
	defaultValue = "by_value"
)
public enum VCICredentialOfferParameterVariant {

	BY_VALUE,

	BY_REFERENCE;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
