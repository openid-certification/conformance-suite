package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.AuthzenSupport;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-batch-override-replaces-default-entity",
	displayName = "AuthZEN Evaluations API - Section 7.1.1: An evaluation's entity replaces the top-level default",
	summary = "Section 7.1.1: a key given in an evaluation \"overrides the corresponding top-level default\", and certification scenario 6.2.7 says \"There is no sub-field merging\". The scenario's own request cannot show merging, as its overriding resource repeats every member of the default, so this request overrides the subject with one that leaves out the default's properties. The first evaluation inherits bob with role admin writing the archived record-2, which is permitted (rule 6). The second gives only alice as the subject: replaced whole, alice writing an archived record is denied (rule 5); merged with the default, she would keep role admin and be permitted. Expects [true, false].\n" + AuthzenPDPEvaluationsBatchOverrideReplacesDefaultEntityTest.payload,
	profile = "AuthZEN"
)
@VariantNotApplicable(parameter = AuthzenSupport.class, values = {"core"})
public class AuthzenPDPEvaluationsBatchOverrideReplacesDefaultEntityTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob", "properties": { "role": "admin" } },
			"action": { "name": "write" },
			"resource": { "type": "record", "id": "record-2", "properties": { "status": "archived" } },
			"evaluations": [
				{},
				{
					"subject": { "type": "user", "id": "alice" }
				}
			]
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getExpectedEvaluationsResponseJson() {
		return """
			{
				"evaluations": [
					{ "decision": true },
					{ "decision": false }
				]
			}
			""";
	}

	@Override
	protected String[] additionalDecisionsRequirements() {
		// Section 7.1.1: a key in an evaluation "overrides the corresponding top-level default".
		return new String[] {"AUTHZEN-7.1.1"};
	}
}
