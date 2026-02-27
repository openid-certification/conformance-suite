package net.openid.conformance.vci10issuer;


import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AbstractCheckEndpointContentTypeReturned;
import net.openid.conformance.condition.client.EnsureContentTypeApplicationJwt;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VCIClientAuthType;
import net.openid.conformance.variant.VCIProfile;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.vci10issuer.condition.VCIDecodeSignedCredentialIssuerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIRequestSignedCredentialIssuerMetadata;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-metadata-test-signed",
	displayName = "OID4VCI 1.0: Signed Issuer metadata test",
	summary = """
		This test case validates the signed metadata exposed by the credential issuer,
		as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) specification.

		This test will send a credential issuer metadata request with content-type: application/jwt.
		The test execution is skipped if the credential issuer metadata response is not signed.
		""",
	profile = "OID4VCI-1_0"
)
@VariantParameters({VCIClientAuthType.class, VCIProfile.class})
public class VCIIssuerMetadataSignedTest extends VCIIssuerMetadataTest {

	@Override
	protected void fetchCredentialIssuerMetadata() {
		callAndStopOnFailure(VCIRequestSignedCredentialIssuerMetadata.class);
		super.fetchCredentialIssuerMetadata();
	}

	@Override
	protected void checkIssuerMetadataResponse() {

		String mimeType = AbstractCheckEndpointContentTypeReturned.getMimeTypeFromContentType(
			env.getString("endpoint_response", "headers.content-type"));
		if ("application/json".equalsIgnoreCase(mimeType)) {
			fireTestSkipped("Skipping test as credential issuer metadata response is not signed (content-type returned is application/json).");
			return;
		}

		callAndContinueOnFailure(EnsureContentTypeApplicationJwt.class, Condition.ConditionResult.FAILURE, "RFC8414-3.2");
		callAndStopOnFailure(VCIDecodeSignedCredentialIssuerMetadata.class, "OID4VCI-1FINAL-12.2.2");
	}
}
