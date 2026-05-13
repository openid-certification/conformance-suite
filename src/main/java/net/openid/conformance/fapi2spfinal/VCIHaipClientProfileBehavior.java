package net.openid.conformance.fapi2spfinal;

import java.util.HashMap;
import java.util.Map;

/**
 * Profile behavior for VCI HAIP (High Assurance Interoperability Profile) client tests.
 *
 * <p>HAIP wallet constraints (DPoP-only sender constraining, client_attestation auth,
 * unsigned auth requests, plain OAuth without id_token, no JARM) are enforced via the
 * variant configuration in {@link net.openid.conformance.vci10wallet.VCIWalletTestPlanHaip}.
 * The only issuer-side server-emulator difference today is injecting the HAIP
 * status_list claim into issued SD-JWT credentials, which is done via
 * {@link #additionalSdJwtClaims()}.
 */
public class VCIHaipClientProfileBehavior extends VCIClientProfileBehavior {

	/**
	 * Inject the HAIP-required SD-JWT status list reference so paired wallet tests
	 * receive the same credential shape regardless of which side serves the issuer.
	 * Mirrors {@code AbstractVCIWalletTest.additionalSdJwtClaimsForHaip}.
	 */
	@Override
	protected Map<String, Object> additionalSdJwtClaims() {
		String issuer = module.getEnv().getString("server", "issuer");
		String statusListUri = (issuer == null ? "" : issuer) + "statuslists/1";

		Map<Object, Object> statusListEntry = new HashMap<>();
		statusListEntry.put("idx", 0);
		statusListEntry.put("uri", statusListUri);

		Map<Object, Object> status = new HashMap<>();
		status.put("status_list", statusListEntry);

		Map<String, Object> additionalClaims = new HashMap<>();
		additionalClaims.put("status", status);
		return additionalClaims;
	}
}
