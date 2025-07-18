package net.openid.conformance.vciid2issuer.condition;

import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.variant.VCIServerMetadata;

public class VCIFetchCredentialIssuerMetadataSequence extends AbstractConditionSequence {

	private final VCIServerMetadata metadata;

	public VCIFetchCredentialIssuerMetadataSequence(VCIServerMetadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public void evaluate() {

		switch (metadata) {
			case DISCOVERY:
				callAndStopOnFailure(VCIGetDynamicCredentialIssuerMetadata.class, "OID4VCI-ID2-11.2.2");
				break;
			case STATIC:
				callAndStopOnFailure(VCIGetStaticCredentialIssuerMetadata.class, "OID4VCI-ID2-11.2.2");
				break;
		}
	}

}
