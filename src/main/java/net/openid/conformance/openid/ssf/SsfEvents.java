package net.openid.conformance.openid.ssf;

import java.util.HashSet;
import java.util.Set;

public class SsfEvents {

	public static Set<String> getStandardCapeEvents() {
		return new HashSet<>(Set.of(
			// CAEP events
			"https://schemas.openid.net/secevent/caep/event-type/session-established", //
			"https://schemas.openid.net/secevent/caep/event-type/session-presented", //
			"https://schemas.openid.net/secevent/caep/event-type/session-revoked", //
			"https://schemas.openid.net/secevent/caep/event-type/credential-change", //
			"https://schemas.openid.net/secevent/caep/event-type/device-compliance-change", //
			"https://schemas.openid.net/secevent/caep/event-type/assurance-level-change", //
			"https://schemas.openid.net/secevent/caep/event-type/token-claims-change", //
			"https://schemas.openid.net/secevent/ssf/event-type/verification", //
			// See: https://openid.github.io/sharedsignals/openid-caep-1_0.html#name-risk-level-change
			"https://schemas.openid.net/secevent/caep/event-type/risk-level-change" //
		));
	}

	public static Set<String> getStandardRiscEvents() {
		return new HashSet<>(Set.of(
			// RISC events
			"https://schemas.openid.net/secevent/risc/event-type/account-credential-change-required", //
			"https://schemas.openid.net/secevent/risc/event-type/account-disabled", //
			"https://schemas.openid.net/secevent/risc/event-type/account-enabled", //
			"https://schemas.openid.net/secevent/risc/event-type/account-purged", //
			"https://schemas.openid.net/secevent/risc/event-type/credential-compromise", //
			"https://schemas.openid.net/secevent/risc/event-type/identifier-changed", //
			"https://schemas.openid.net/secevent/risc/event-type/identifier-recycled", //
			"https://schemas.openid.net/secevent/risc/event-type/opt-in", //
			"https://schemas.openid.net/secevent/risc/event-type/opt-out-cancelled", //
			"https://schemas.openid.net/secevent/risc/event-type/opt-out-effective", //
			"https://schemas.openid.net/secevent/risc/event-type/opt-out-initiated", //
			"https://schemas.openid.net/secevent/risc/event-type/recovery-activated", //
			"https://schemas.openid.net/secevent/risc/event-type/recovery-information-changed" //
		));
	}


}
