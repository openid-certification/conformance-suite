package net.openid.conformance.variant;

@VariantParameter(
	name = "ssf_delivery_mode",
	displayName = "Delivery Mode",
	description = "Whether the server supports PUSH or PULL based delivery mode."
)
public enum SsfDeliveryMode {

	PUSH,
	PULL;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
