package net.openid.conformance.fapi2spfinal;

/**
 * Profile behavior for VCI HAIP (High Assurance Interoperability Profile) client tests.
 *
 * <p>HAIP wallet constraints (DPoP-only sender constraining, client_attestation auth,
 * unsigned auth requests, plain OAuth without id_token, no JARM) are enforced via the
 * variant configuration in {@link net.openid.conformance.vci10wallet.VCIWalletTestPlanHaip},
 * so this behavior currently inherits {@link VCIClientProfileBehavior} unchanged.
 * Override methods here if future HAIP-specific server-emulator behavior is needed.
 */
public class VCIHaipClientProfileBehavior extends VCIClientProfileBehavior {
}
