package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddBuiltInRegistrationCertificateToClientConfiguration;
import net.openid.conformance.condition.client.AddUntrustedAccessCertificateToClientConfiguration;
import net.openid.conformance.condition.client.WarnIfBuiltInAccessCertificateIsEphemeral;
import net.openid.conformance.condition.common.ExpectUntrustedRequestObjectCertificateErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.VariantApplicableOnly;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-untrusted-access-certificate",
	displayName = "OID4VP-1.0-FINAL: Request object signed with an untrusted access certificate",
	summary = """
		Makes a request signed with a certificate that is a copy of the configured signing certificate (or, with no \
		'jwks' field in the 'Client' section of the test configuration, of the suite's built-in wallet-relying-party \
		access certificate): the same subject including the organizationIdentifier and the same subject alternative \
		name and extensions, so the client_id still matches it, but with a new key and issued by a certificate \
		authority minted at runtime that no published trust anchor or list of trusted entities contains. A \
		multi-signed request is signed with untrusted copies of both configured certificates. The wallet must \
		validate the trust chain of the request signing certificate, so it should display an error, a screenshot \
		of which must be uploaded.""",
	profile = "OID4VP-1FINAL"
)
@VariantApplicableOnly(parameter = VP1FinalWalletClientIdPrefix.class, values = {"x509_san_dns", "x509_hash"})
@VariantNotApplicable(parameter = VP1FinalWalletRequestMethod.class, values = {"request_uri_unsigned", "url_query"})
public class VP1FinalWalletUntrustedAccessCertificate extends AbstractVP1FinalWalletTest {

	@Override
	protected void addBuiltInCertificatesToClientConfiguration() {
		// the registration certificate is bound to the access certificate by organizationIdentifier, which the
		// untrusted certificate shares, so the trust chain stays the request's only defect
		callAndContinueOnFailure(WarnIfBuiltInAccessCertificateIsEphemeral.class, ConditionResult.WARNING);
		callAndStopOnFailure(AddUntrustedAccessCertificateToClientConfiguration.class, clientIdPrefixRequirement());
		callAndStopOnFailure(AddBuiltInRegistrationCertificateToClientConfiguration.class);
	}

	@Override
	protected void checkOwnRequestSigningCertificatesAgainstTrustLists() {
		// the request signing certificate is deliberately covered by no trust list
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectUntrustedRequestObjectCertificateErrorPage.class, clientIdPrefixRequirement());
		env.putString("error_callback_placeholder", env.getString("untrusted_request_object_certificate_error"));
	}

	@Override
	protected void continueAfterRequestUriCalled() {
		eventLog.log(getName(), "Wallet has retrieved request_uri - the request object signing certificate does not chain to a trusted certificate authority, so the wallet should display an error, a screenshot of which must be uploaded for the test to transition to 'FINISHED'.");
		createPlaceholder();
		waitForPlaceholders();
	}

	@Override
	protected Object handleDirectPost(String requestId) {
		throw new TestFailureException(getId(), "Direct post (response_uri) endpoint has been called but the wallet should have stopped because the request object signing certificate does not chain to a trusted certificate authority.");
	}

	@Override
	protected void processBrowserApiResponse() {
		handleBrowserApiResponseAsNegativeTest(
			"Browser API returned a successful response but the wallet should have stopped because the request object signing certificate does not chain to a trusted certificate authority.");
	}

	private String clientIdPrefixRequirement() {
		return clientIdPrefix == VP1FinalWalletClientIdPrefix.X509_HASH ? "OID4VP-1FINAL-5.9.3-6" : "OID4VP-1FINAL-5.9.3-5";
	}
}
