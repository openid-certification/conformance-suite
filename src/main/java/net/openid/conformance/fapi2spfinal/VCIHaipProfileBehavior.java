package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.client.CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsAttestation;

/**
 * Profile behavior for VCI HAIP (High Assurance Interoperability Profile) tests.
 *
 * <p>Extends {@link VCIProfileBehavior} with HAIP-specific constraints.
 * HAIP requires attestation-based client authentication only.
 * Additional HAIP-specific constraints (DPoP ES256, nonce endpoint, credential validity,
 * x5c headers) are currently handled via {@code fapi2Profile == VCI_HAIP} checks in
 * {@link net.openid.conformance.vci10issuer.AbstractVCIIssuerTestModule}. Override methods
 * here if future HAIP behavior belongs in the profile behavior layer.
 */
public class VCIHaipProfileBehavior extends VCIProfileBehavior {

	@Override
	public Class<? extends AbstractCondition> getDiscoveryTokenEndpointAuthMethodsCheck() {
		return CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsAttestation.class;
	}
}
