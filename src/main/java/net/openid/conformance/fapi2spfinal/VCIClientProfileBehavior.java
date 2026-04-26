package net.openid.conformance.fapi2spfinal;

/**
 * Profile behavior for VCI (Verifiable Credentials Issuance) client tests.
 *
 * <p>The conformance suite acts as the AS that the wallet under test interacts with.
 * For the FAPI2SP client tests pulled into the VCI wallet plan, the AS-emulator
 * defaults from {@link FAPI2ClientProfileBehavior} are appropriate — the wallet's
 * VCI-specific behavior (credential issuer metadata discovery, credential endpoint,
 * nonce endpoint, deferred / notification endpoints) is exercised by the existing
 * {@code VCIWalletTest*} modules, not by the FAPI2SP client tests.
 *
 * <p>Wallet-side VCI specifics (credential offer flow, credential issuer metadata
 * exposure, credential endpoint emulation) live on {@code AbstractVCIWalletTest}
 * today and will be merged into this base class when that absorption happens.
 */
public class VCIClientProfileBehavior extends FAPI2ClientProfileBehavior {
}
