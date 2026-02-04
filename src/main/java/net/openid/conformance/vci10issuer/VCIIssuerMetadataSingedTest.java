package net.openid.conformance.vci10issuer;


import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureContentTypeApplicationJwt;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VCIClientAuthType;
import net.openid.conformance.variant.VCIProfile;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.vci10issuer.condition.VCIDecodeSignedCredentialIssuerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIParseCredentialIssuerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIRequestSignedCredentialIssuerMetadata;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-metadata-test-signed",
	displayName = "OID4VCI 1.0: Signed Issuer metadata test",
	summary = """
		This test case validates the signed metadata exposed by the credential issuer,
		as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) specification.

		This test will send a credential issuer metadata request with content-type: application/jwt.
		""",
	profile = "OID4VCI-1_0",
	configurationFields = {
		"vci.credential_issuer_url"
	}
)
@VariantParameters({VCIClientAuthType.class, VCIProfile.class})
public class VCIIssuerMetadataSingedTest extends VCIIssuerMetadataTest{

	@Override
	protected ConditionSequence createFetchCredentialIssuerMetadataSequence() {
		return super.createFetchCredentialIssuerMetadataSequence()
			.butFirst(condition(VCIRequestSignedCredentialIssuerMetadata.class))
			.replace(VCIParseCredentialIssuerMetadata.class, condition(VCIDecodeSignedCredentialIssuerMetadata.class));
	}

	@Override
	protected void checkIssuerMetadataResponse() {
		callAndContinueOnFailure(EnsureContentTypeApplicationJwt.class, Condition.ConditionResult.FAILURE, "RFC8414-3.2");
	}
}
