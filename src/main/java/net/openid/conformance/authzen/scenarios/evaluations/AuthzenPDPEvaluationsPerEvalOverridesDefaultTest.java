package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-per-eval-overrides-default",
	displayName = "Authzen Evaluations API - Spec 7.1.1.1: Per-evaluation values override top-level defaults",
	summary = "Per spec 7.1.1.1, when a field is present in both the top-level request and a per-evaluation entry, the per-evaluation value takes precedence. Top-level action is `write` (would deny for bob/record-1); evaluation 1 overrides to `read` (permit). Evaluation 2 omits the action and inherits the top-level `write` (deny).\n" + AuthzenPDPEvaluationsPerEvalOverridesDefaultTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsPerEvalOverridesDefaultTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob" },
			"action": { "name": "write" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				{ "action": { "name": "read" } },
				{ }
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
}
