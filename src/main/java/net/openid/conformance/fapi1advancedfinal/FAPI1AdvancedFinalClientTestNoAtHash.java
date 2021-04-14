package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.as.RemoveAtHashFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-missing-athash",
	displayName = "FAPI-RW-ID2: client test - id_token without an at_hash value from the authorization_endpoint, should be rejected",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)
@VariantNotApplicable(parameter = FAPIProfile.class, values = { "plain_fapi" })
public class FAPIRWID2ClientTestNoAtHash extends AbstractFAPIRWID2ClientTest {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(RemoveAtHashFromIdToken.class, "OIDCC-3.3.2.9");
	}

}
