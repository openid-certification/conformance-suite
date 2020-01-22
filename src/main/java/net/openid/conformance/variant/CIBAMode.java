package net.openid.conformance.variant;

@VariantParameter(
	name = "ciba_mode",
	displayName = "CIBA Mode",
	description = "The CIBA notification mode you want to test. If you server supports more than one, run create separate test plans for each one."
)
public enum CIBAMode {

	POLL,
	PING,
	PUSH;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
