package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilCheckDirectoryKeystore;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-brazildcr-happy-flow",
	displayName = "FAPI2-Security-Profile-ID2: Brazil DCR happy flow",
	summary = "Obtain a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration), register a new client on the target authorization server and perform an authorization flow.",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase",
		"resource.resourceUrl"
	}
)
public class FAPI2SPID2BrazilDCRHappyFlow extends AbstractFAPI2SPID2BrazilDCR {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		if (isBrazil) {
			if (brazilPayments) {
				callAndContinueOnFailure(FAPIBrazilCheckDirectoryKeystore.class, Condition.ConditionResult.FAILURE);
			}
		}
	}

}
