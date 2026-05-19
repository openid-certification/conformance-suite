package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.InvalidateFirstMultiSignedRequestObjectSignature;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-multisigned-one-invalid-signature",
	displayName = "OID4VP-1.0-FINAL: Multi-signed request with one invalid signature",
	summary = "Makes a request where one of the two signatures in the multi-signed request object is invalid, but the other remains valid. Per the specification, wallets MUST verify at least one valid signature, so the wallet should accept the request and respond normally.",
	profile = "OID4VP-1FINAL"
)
@VariantNotApplicable(parameter = VP1FinalWalletRequestMethod.class, values = {"request_uri_unsigned", "url_query", "request_uri_signed"})
public class VP1FinalWalletMultiSignedOneInvalidSignature extends AbstractVP1FinalWalletTest {

	@Override
	protected void signRequestObject() {
		super.signRequestObject();
		callAndStopOnFailure(InvalidateFirstMultiSignedRequestObjectSignature.class);
	}

}
