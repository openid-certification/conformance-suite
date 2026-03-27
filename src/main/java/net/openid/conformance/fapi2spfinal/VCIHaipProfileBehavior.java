package net.openid.conformance.fapi2spfinal;

/**
 * Profile behavior for VCI HAIP (High Assurance Interoperability Profile) tests.
 *
 * <p>Currently identical to {@link VCIProfileBehavior} — exists as a distinct class so that
 * {@code @VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "vci_haip")} can
 * select it. HAIP-specific constraints (DPoP ES256, nonce endpoint, credential validity,
 * x5c headers) are currently handled via {@code fapi2Profile == VCI_HAIP} checks in
 * {@link net.openid.conformance.vci10issuer.AbstractVCIIssuerTestModule}. Override methods
 * here if future HAIP behavior belongs in the profile behavior layer.
 */
public class VCIHaipProfileBehavior extends VCIProfileBehavior {
}
