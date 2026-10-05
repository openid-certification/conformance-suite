package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CreateMultiSignedRequestObjectWithWrongTyp;
import net.openid.conformance.condition.client.SignRequestObjectIncludeTypHeaderWithWrongTyp;
import net.openid.conformance.condition.client.SignRequestObjectIncludeX5cHeaderIfAvailableWithWrongTyp;
import net.openid.conformance.condition.client.SignRequestObjectIncludeX5cHeaderWithWrongTyp;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-invalid-request-object-typ",
	displayName = "OID4VP-1.0-FINAL: Request object typ header not valid",
	summary = """
		Sends a correctly signed request object whose 'typ' header is 'jwt' instead of \
		'oauth-authz-req+jwt'. Per OID4VP section 5, wallets MUST NOT process request objects where the \
		typ header does not have the value 'oauth-authz-req+jwt'. This applies equally to a signed request \
		passed over the DC API, which Appendix A.3.2.1 defines as a request object as per section 5. \
		For a multi-signed request the typ is wrong in the protected header of every signature.

		Such a request object was not intended for the wallet, so the wallet must not act on it. In the \
		redirect flow it must not send anything, not even an error response, to the response_uri, which is \
		only known from inside the request object. Over the DC API it must not return a fulfilled response \
		of any kind; it is expected to reject the request. The wallet should display an error, a screenshot \
		of which must be uploaded.""",
	profile = "OID4VP-1FINAL"
)
// only a signed request object has a typ header
@VariantNotApplicable(parameter = VP1FinalWalletRequestMethod.class, values = {"request_uri_unsigned", "url_query"})
public class VP1FinalWalletInvalidRequestObjectTyp extends AbstractVP1FinalWalletInvalidRequestObjectTyp {

	@Override
	protected Class<? extends Condition> defectiveSigningCondition(VP1FinalWalletClientIdPrefix prefix) {
		return switch (prefix) {
			case X509_SAN_DNS, X509_HASH -> SignRequestObjectIncludeX5cHeaderWithWrongTyp.class;
			case PRE_REGISTERED -> SignRequestObjectIncludeX5cHeaderIfAvailableWithWrongTyp.class;
			case DECENTRALIZED_IDENTIFIER -> SignRequestObjectIncludeTypHeaderWithWrongTyp.class;
			case REDIRECT_URI, WEB_ORIGIN -> throw new RuntimeException(prefix + " client id prefix is not valid for signed requests");
		};
	}

	@Override
	protected Class<? extends Condition> multiSigningConditionWithDefectiveTyp() {
		return CreateMultiSignedRequestObjectWithWrongTyp.class;
	}

}
