package net.openid.conformance.variant;

@VariantParameter(
	name = "authzen_support",
	displayName = "AuthZEN Specification Support",
	description = "Specifies whether the PDP supports only the 'Core' parts of the spec, where any optional 'Properties' sent in a request do not affect the decision, or also bases decisions on 'Properties' in requests",
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
