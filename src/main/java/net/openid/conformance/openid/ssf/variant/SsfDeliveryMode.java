package net.openid.conformance.openid.ssf.variant;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "ssf_delivery_mode",
	displayName = "SSF Delivery Mode",
	description = "Whether the SSF server supports PUSH or POLL based delivery mode."
)
public enum SsfDeliveryMode {

	/**
	 * The Push delivery mode (RFC 8935), see: https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html#section-6.1.1
	 */
	PUSH("urn:ietf:rfc:8935"),

	/**
	 * The Poll delivery mode (RFC 8936), see: https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html#section-6.1.2
	 */
	POLL("urn:ietf:rfc:8936");

	@Override
	public String toString() {
		return name().toLowerCase();
	}

	private final String alias;

	SsfDeliveryMode(String alias) {
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}
}
