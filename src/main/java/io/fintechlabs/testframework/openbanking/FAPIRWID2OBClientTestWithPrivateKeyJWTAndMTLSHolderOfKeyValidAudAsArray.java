package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.as.AddAudValueAsArrayToIdToken;
import io.fintechlabs.testframework.condition.as.SignIdTokenBypassingNimbusChecks;
import io.fintechlabs.testframework.fapi.FAPIRWID2ClientTest;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-ob-client-test-with-private-key-jwt-and-mtls-holder-of-key-valid-aud-as-array",
	displayName = "FAPI-RW-ID2-OB: client test - valid aud in id_token as data type array (with private_key_jwt and MTLS)",
	summary = "This test should be successful. The value of aud within the id_token will be represented as array with one value",
	profile = "FAPI-RW-ID2-OB",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	},
	notApplicableForVariants = {
		FAPIRWID2ClientTest.variant_mtls,
		FAPIRWID2ClientTest.variant_privatekeyjwt,
		FAPIRWID2ClientTest.variant_openbankinguk_mtls
	}
)

public class FAPIRWID2OBClientTestWithPrivateKeyJWTAndMTLSHolderOfKeyValidAudAsArray extends AbstractFAPIRWID2OBClientTest {

	@Variant(name = variant_openbankinguk_privatekeyjwt)
	public void setupOpenBankingUkPrivateKeyJwt() {
		super.setupOpenBankingUkPrivateKeyJwt();
	}

	@Override
	protected void addCustomValuesToIdToken(){

		callAndStopOnFailure(AddAudValueAsArrayToIdToken.class, "OIDCC-3.1.3.7-3");
	}

	@Override
	protected void addCustomSignatureOfIdToken(){

		callAndStopOnFailure(SignIdTokenBypassingNimbusChecks.class);
	}

}
