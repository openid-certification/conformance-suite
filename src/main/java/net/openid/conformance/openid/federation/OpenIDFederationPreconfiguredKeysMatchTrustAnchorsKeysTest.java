package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

import java.util.List;

import static net.openid.conformance.openid.federation.EntityUtils.appendWellKnown;
import static net.openid.conformance.openid.federation.EntityUtils.stripWellKnown;

@PublishTestModule(
	testName = "openid-federation-preconfigured-keys-match-trust-anchors-keys",
	displayName = "OpenID Federation: Preconfigured keys match trust anchor's keys",
	summary = "This test starts at the given entity and follows the chain up to the trust anchor. " +
		"When the trust anchor has been reached, the `jwks` specified in its Entity Configuration " +
		"are compared to the `trust_anchor_jwks` keys specified in the test configuration.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_identifier",
		"federation.trust_anchor",
		"federation.trust_anchor_jwks",
	}
)
public class OpenIDFederationPreconfiguredKeysMatchTrustAnchorsKeysTest extends AbstractOpenIDFederationTest {

	@Override
	public void additionalConfiguration() {
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		String fromEntity = stripWellKnown(env.getString("config", "federation.entity_identifier"));
		String trustAnchor = env.getString("config", "federation.trust_anchor");
		if (trustAnchor == null) {
			throw new TestFailureException(getId(), "The test configuration does not contain a trust anchor");
		}
		JsonElement trustAnchorJwks = env.getElementFromObject("config", "federation.trust_anchor_jwks");
		if (trustAnchorJwks != null && trustAnchorJwks.isJsonPrimitive()) {
			throw new TestFailureException(getId(), "The preconfigured trust anchor jwks is not a valid JSON object. " +
				"Please verify that your configuration does not contain errors.");
		}
		if (trustAnchorJwks == null || !trustAnchorJwks.isJsonObject()) {
			fireTestSkipped("The test configuration does not contain preconfigured trust anchor jwks.");
		}

		List<String> path;
		try {
			path = findPath(fromEntity, trustAnchor);
		} catch (CyclicPathException e) {
			throw new TestFailureException(getId(), e.getMessage());
		}

		if (path == null || path.isEmpty()) {
			throw new TestFailureException(getId(), "A trust chain from %s to %s can not be constructed".formatted(fromEntity, trustAnchor));
		}

		eventLog.startBlock("Retrieving entity configuration for trust anchor %s".formatted(trustAnchor));
		env.putString("federation_endpoint_url", appendWellKnown(trustAnchor));
		callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
		callAndStopOnFailure(CallEntityStatementEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-9");
		validateEntityStatementResponse();
		callAndStopOnFailure(ExtractJWTFromFederationEndpointResponse.class,  "OIDFED-9");
		callAndContinueOnFailure(ExtractJWKsFromEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		eventLog.endBlock();

		eventLog.startBlock("Validate that preconfigured keys match trust anchor's keys");
		callAndContinueOnFailure(ValidateJwksAreEqual.class, Condition.ConditionResult.FAILURE, "OIDFED-11.3");
		eventLog.endBlock();

		fireTestFinished();
	}

}
