package net.openid.conformance.fapi2spfinal;

/**
 * Profile behavior for VCI HAIP (High Assurance Interoperability Profile) client tests.
 *
 * <p>HAIP wallet constraints (DPoP-only sender constraining, client_attestation auth,
 * unsigned auth requests, plain OAuth without id_token, no JARM) are enforced via the
 * variant configuration in {@link net.openid.conformance.vci10wallet.VCIWalletTestPlanHaip}.
 * The issuer-side server-emulator difference is that issued mdoc credentials reference the
 * emulated issuer's Token Status List from the MSO, as the SD-JWT credentials of every profile do.
 */
public class VCIHaipClientProfileBehavior extends VCIClientProfileBehavior {

	/**
	 * Reference the same Token Status List from the MSO's status element of issued mdoc
	 * credentials (ISO/IEC 18013-5 12.3.6.2), so mdoc and SD-JWT credentials carry equivalent
	 * revocation information.
	 */
	@Override
	public String additionalMdocStatusListUri() {
		return buildStatusListUri(module.getEnv());
	}
}
