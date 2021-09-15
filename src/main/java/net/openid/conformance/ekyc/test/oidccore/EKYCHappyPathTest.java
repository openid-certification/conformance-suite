package net.openid.conformance.ekyc.test.oidccore;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ekyc-server-happypath",
	displayName = "eKYC Happy Path Server Test",
	summary = "Tests primarily 'happy' flows.",
	profile = "OIDCC",
	configurationFields = {
		"trust_framework",
		"verified_claim_names"
	}
)
public class EKYCHappyPathTest extends BaseEKYCTestWithOIDCCore {

}
