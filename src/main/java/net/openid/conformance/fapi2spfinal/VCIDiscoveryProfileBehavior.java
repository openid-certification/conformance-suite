package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.vci10issuer.condition.VCIFetchOAuthorizationServerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIGetDynamicCredentialIssuerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIParseCredentialIssuerMetadata;
import net.openid.conformance.vci10issuer.condition.VCISelectOAuthorizationServer;
import net.openid.conformance.vci10issuer.condition.VCISetDiscoveryUrlFromAuthorizationServer;

/**
 * Discovery profile behavior for VCI (Verifiable Credentials Issuance) tests.
 *
 * Fetches credential issuer metadata to discover the authorization server,
 * then sets discoveryUrl so that standard discovery checks can validate the AS metadata.
 */
public class VCIDiscoveryProfileBehavior extends FAPI2DiscoveryProfileBehavior {

	@Override
	public ConditionSequence fetchServerConfiguration(boolean isOpenId) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(VCIGetDynamicCredentialIssuerMetadata.class, "OID4VCI-1FINAL-12.2.2");
				callAndStopOnFailure(VCIParseCredentialIssuerMetadata.class, "OID4VCI-1FINAL-12.2.2");
				callAndStopOnFailure(VCIFetchOAuthorizationServerMetadata.class, ConditionResult.FAILURE,
					"OID4VCI-1FINAL-12.2.3", "RFC8414-3.1");
				callAndStopOnFailure(VCISelectOAuthorizationServer.class, ConditionResult.FAILURE,
					"OID4VCI-1FINAL-12.2.3");
			}
		};
	}

	@Override
	public ConditionSequence afterServerConfigurationFetched() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(VCISetDiscoveryUrlFromAuthorizationServer.class);
			}
		};
	}
}
