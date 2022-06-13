package net.openid.conformance.fapi2baselineid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.as.par.CreatePAREndpointResponse;
import net.openid.conformance.condition.as.par.SetParResponseExpiresInToGreaterThan600;
import net.openid.conformance.condition.as.par.SetParResponseExpiresInToLessThan5;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;


@PublishTestModule(
	testName = "fapi2-baseline-id2-client-test-ensure-par-expires_in-less-than-5-fails",
	displayName = "FAPI2-Baseline-ID2: sends a PAR response with an expires_in value greater than 600",
	summary = "This test should end with the client displaying an error message that the PAR response has an invalid expires_in value",
	profile = "FAPI2-Baseline-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_urcreatePAREndpointResponsei",
		"client.certificate",
		"client.jwks"
	}
)

public class FAPI2BaselineID2ClientTestEnsureParExpiresInLessThan5Fails extends AbstractFAPI2BaselineID2ClientExpectNothingAfterParResponse {


	@Override
	protected void addCustomValuesToParResponse() {
		callAndContinueOnFailure(SetParResponseExpiresInToLessThan5.class);
	}

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected String getParResponseErrorMessage() {
		return "Set PAR response expires_in < 5";
	}
}
