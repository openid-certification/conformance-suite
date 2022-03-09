package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.condition.as.RemoveAtHashFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.FAPIJARMType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-baseline-id2-client-test-missing-athash",
	displayName = "FAPI2-Baseline-ID2: client test - id_token without an at_hash value from the authorization_endpoint, should be rejected",
	profile = "FAPI2-Baseline-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"directory.keystore"
	}
)
@VariantNotApplicable(parameter = FAPIJARMType.class, values = { "plain_oauth" })
@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = { "plain_fapi" })
public class FAPI2BaselineID2ClientTestNoAtHash extends AbstractFAPI2BaselineID2ClientTest {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(RemoveAtHashFromIdToken.class, "OIDCC-3.3.2.9");
	}
	//TODO should not the client stop after receiving an invalid id token? this test should start waiting for timeout?
}
