package net.openid.conformance.openbanking_brasil.testmodules.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.MapDirectoryValues;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetDirectoryInfo;
import net.openid.conformance.openbanking_brasil.testmodules.support.UnmapDirectoryValues;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateWellKnownUriSteps;
import net.openid.conformance.openbanking_brasil.testmodules.support.consent.v2.ValidateConsentsFieldV2;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "preflight-check-test-v2",
	displayName = "Pre-flight checks will validate the mTLS certificate before requesting an access token using the Directory client_id provided in the test configuration. An SSA will be generated using the Open Banking Brasil Directory. Finally" +
		"a check of mandatory fields will be made",
	summary = "Pre-flight checks will validate the mTLS certificate before requesting an access token using the Directory client_id provided in the test configuration. An SSA will be generated using the Open Banking Brasil Directory. Finally" +
		"a check of mandatory fields will be made",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
        "directory.client_id"
	}
)

public class PreFlightCheckV2Module extends PreFlightCertCheckModule {

	@Override
    protected void runTests() {
		super.runTests();
		runInBlock("Pre-flight Consent field checks", () -> {
			callAndContinueOnFailure(ValidateConsentsFieldV2.class, Condition.ConditionResult.FAILURE);
		});
    }
}
