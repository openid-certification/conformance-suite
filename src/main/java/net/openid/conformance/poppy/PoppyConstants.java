package net.openid.conformance.poppy;

public final class PoppyConstants {

	public static final String PROTOCOL_VERSION = "0.4";
	public static final String SESSION_JWT_TYP = "poppy-session+jwt";
	public static final String DELEGATION_JWT_TYP = "poppy-delegation+jwt";
	public static final String HEADER_SESSION = "Poppy-Session";
	public static final String HEADER_VERSION = "Poppy-Version";
	public static final int DELEGATION_LIFETIME_SECONDS = 3600;
	public static final int DELEGATION_MAX_LIFETIME_SECONDS = 86400;

	private PoppyConstants() {
	}
}
