package net.openid.conformance.variant;

@VariantParameter(
	name = "ciba_mode",
	displayName = "CIBA Mode",
	description = "This configuration allows the client to get authentication result in three 3 ways: poll, ping & push (This mode is not currently part of the certification program)."
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
