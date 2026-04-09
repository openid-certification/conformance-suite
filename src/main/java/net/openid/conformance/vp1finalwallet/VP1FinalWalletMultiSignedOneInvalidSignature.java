package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.CreateMultiSignedRequestObject;
import net.openid.conformance.condition.client.InvalidateFirstMultiSignedRequestObjectSignature;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;
import org.jetbrains.annotations.NotNull;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-multisigned-one-invalid-signature",
	displayName = "OID4VP-1.0-FINAL: Multi-signed request with one invalid signature",
	summary = "Makes a request where one of the two signatures in the multi-signed request object is invalid, but the other remains valid. Per the specification, wallets MUST verify at least one valid signature, so the wallet should accept the request and respond normally.",
	profile = "OID4VP-1FINAL"
)
@VariantNotApplicable(parameter = VP1FinalWalletRequestMethod.class, values = {"request_uri_unsigned", "url_query", "request_uri_signed"})
public class VP1FinalWalletMultiSignedOneInvalidSignature extends AbstractVP1FinalWalletTest {

	@NotNull
	@Override
	protected ConditionSequence createAuthorizationRedirectStepsMultiSignedRequestUri() {
		ConditionSequence seq = super.createAuthorizationRedirectStepsMultiSignedRequestUri();

		seq = seq.insertAfter(CreateMultiSignedRequestObject.class,
			condition(InvalidateFirstMultiSignedRequestObjectSignature.class));

		return seq;
	}

}
