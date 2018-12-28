package io.fintechlabs.testframework.example;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author jricher
 *
 */
@PublishTestPlan (
	testPlanName = "sample-test-plan",
	displayName = "Sample Test Plan",
	testModuleNames = {
		"sample-test",
		"ensure-redirect-uri-in-authorization-request",
		"ensure-redirect-uri-is-registered"
	},
	summary = "This is a test plan summary which gives the user more information about this test plan including possibly how to interact with the plan."
)
public class SampleTestPlan implements TestPlan {

}
