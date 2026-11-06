package net.openid.conformance.poppy;

import java.util.Set;

public final class PoppyConstants {

	public static final String PROTOCOL_VERSION = "0.4";
	public static final String SESSION_JWT_TYP = "poppy-session+jwt";
	public static final String DELEGATION_JWT_TYP = "poppy-delegation+jwt";
	public static final String HEADER_SESSION = "Poppy-Session";
	public static final String HEADER_VERSION = "Poppy-Version";
	public static final int DELEGATION_LIFETIME_SECONDS = 3600;
	public static final int DELEGATION_MAX_LIFETIME_SECONDS = 86400;

	/** The scopes the emulated Company publishes in its authorization server metadata and enforces. */
	public static final Set<String> COMPANY_SCOPES = Set.of("orders:read");

	private PoppyConstants() {
	}
}
