package net.openid.conformance.variant;

@VariantParameter(
	name = "authzen_support",
	displayName = "Authzen Specification Support",
	description = "Specifies whether the PDP supports only the 'Core' parts of the spec (no optional 'Properties' in the request) or 'Properties' in requests",
	defaultValue = "core"
)
public enum AuthzenSupport {

	CORE,
	PROPERTIES;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
