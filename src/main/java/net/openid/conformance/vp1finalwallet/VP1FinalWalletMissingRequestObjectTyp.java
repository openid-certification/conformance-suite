package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CreateMultiSignedRequestObjectWithoutTyp;
import net.openid.conformance.condition.client.SignRequestObjectIncludeTypHeaderWithoutTyp;
import net.openid.conformance.condition.client.SignRequestObjectIncludeX5cHeaderIfAvailableWithoutTyp;
import net.openid.conformance.condition.client.SignRequestObjectIncludeX5cHeaderWithoutTyp;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-missing-request-object-typ",
	displayName = "OID4VP-1.0-FINAL: Request object without typ header",
	summary = """
		Sends a correctly signed request object that has no 'typ' header. Per OID4VP section 5, wallets \
		MUST NOT process request objects where the typ header is not present. This applies equally to a \
		signed request passed over the DC API, which Appendix A.3.2.1 defines as a request object as per \
		section 5. \
		For a multi-signed request the typ is absent in the protected header of every signature.

		Such a request object was not intended for the wallet, so the wallet must not act on it. In the \
		redirect flow it must not send anything, not even an error response, to the response_uri, which is \
		only known from inside the request object; the wallet should display an error, a screenshot of \
		which must be uploaded. Over the DC API the wallet should either return an error response \
		(normally 'invalid_request' or 'invalid_request_object') or reject the request and display an \
		error, a screenshot of which must be uploaded.""",
	profile = "OID4VP-1FINAL"
)
// only a signed request object has a typ header
@VariantNotApplicable(parameter = VP1FinalWalletRequestMethod.class, values = {"request_uri_unsigned", "url_query"})
public class VP1FinalWalletMissingRequestObjectTyp extends AbstractVP1FinalWalletInvalidRequestObjectTyp {

	@Override
	protected Class<? extends Condition> defectiveSigningCondition(VP1FinalWalletClientIdPrefix prefix) {
		return switch (prefix) {
			case X509_SAN_DNS, X509_HASH -> SignRequestObjectIncludeX5cHeaderWithoutTyp.class;
			case PRE_REGISTERED -> SignRequestObjectIncludeX5cHeaderIfAvailableWithoutTyp.class;
			case DECENTRALIZED_IDENTIFIER -> SignRequestObjectIncludeTypHeaderWithoutTyp.class;
			case REDIRECT_URI, WEB_ORIGIN -> throw new RuntimeException(prefix + " client id prefix is not valid for signed requests");
		};
	}

	@Override
	protected Class<? extends Condition> multiSigningConditionWithDefectiveTyp() {
		return CreateMultiSignedRequestObjectWithoutTyp.class;
	}

}
