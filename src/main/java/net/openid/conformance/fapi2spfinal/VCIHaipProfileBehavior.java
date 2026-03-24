package net.openid.conformance.fapi2spfinal;

/**
 * Profile behavior for VCI HAIP (High Assurance Interoperability Profile) tests.
 *
 * HAIP adds additional constraints on top of VCI:
 * - DPoP with ES256 required
 * - Nonce endpoint required
 * - Credential validity checks (exp, status list)
 * - x5c header required for SD-JWT credentials
 */
public class VCIHaipProfileBehavior extends VCIProfileBehavior {
}
