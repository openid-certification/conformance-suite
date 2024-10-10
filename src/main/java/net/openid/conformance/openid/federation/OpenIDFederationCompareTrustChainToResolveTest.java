package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

import java.util.ArrayList;
import java.util.List;

import static net.openid.conformance.openid.federation.EntityUtils.appendWellKnown;
import static net.openid.conformance.openid.federation.EntityUtils.stripWellKnown;

@PublishTestModule(
	testName = "openid-federation-compare-trust-chain-to-resolve",
	displayName = "OpenID Federation: Compare trust chain to resolve result",
	summary = "This test verifies the behavior of the federation_resolve_endpoint of the entity's trust anchor. " +
		"The test will attempt to create a trust chain from the configured entity to the trust anchor, and compare the result with " +
		"the result obtained from the trust anchor's resolve endpoint, provided that it exists.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_identifier",
		"federation.trust_anchor",
		"federation.trust_anchor_jwks",
	}
)
public class OpenIDFederationCompareTrustChainToResolveTest extends AbstractOpenIDFederationTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		String fromEntity = stripWellKnown(env.getString("config", "federation.entity_identifier"));
		String trustAnchor = env.getString("config", "federation.trust_anchor");
		if (trustAnchor == null) {
			throw new TestFailureException(getId(), "The test configuration does not contain a trust anchor");
		}

		List<String> path = findPath(fromEntity, trustAnchor);
		if (path == null || path.isEmpty()) {
			throw new TestFailureException(getId(), "A trust chain from %s to %s can not be constructed".formatted(fromEntity, trustAnchor));
		}

		eventLog.startBlock("Fetching entity configuration for trust anchor %s".formatted(trustAnchor));
		env.putString("entity_statement_url", appendWellKnown(trustAnchor));
		callAndContinueOnFailure(CallFederationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ExtractFederationEntityMetadataUrls.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(SetTrustAnchorEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();

		eventLog.startBlock("Comparing manually built trust chain to resolved trust chain");
		List<String> trustChainBuiltManually = buildTrustChain(path);
		List<String> trustChainFromResolver = validateResolveEndpoint();

		JsonObject trustChains = new JsonObject();
		trustChains.add("manual", OIDFJSON.convertListToJsonArray(trustChainBuiltManually));
		trustChains.add("resolved", OIDFJSON.convertListToJsonArray(trustChainFromResolver));
		env.putObject("trust_chains", trustChains);

		callAndContinueOnFailure(CompareTrustChains.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();

		fireTestFinished();
	}

	protected List<String> buildTrustChain(List<String> path) {
		eventLog.startBlock("Building trust chain from %s to %s".formatted(path.get(0), path.get(path.size() - 1)));
		List<String> trustChain = new ArrayList<>();
		trustChain.add(env.getString("primary_entity_statement"));

		if (path.size() == 1) {
			return trustChain;
		}

		for (int i = 1; i < path.size(); i++) {
			String entityIdentifier = path.get(i);
			env.putString("entity_statement_url", appendWellKnown(entityIdentifier));
			callAndContinueOnFailure(CallFederationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			callAndContinueOnFailure(ExtractFederationEntityMetadataUrls.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

			String fetchEndpoint = env.getString("federation_fetch_endpoint");
			env.putString("entity_statement_url", fetchEndpoint);
			String sub = path.get(i - 1);
			env.putString("expected_sub", sub);
			callAndContinueOnFailure(AppendSubToFederationEndpointUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			callAndContinueOnFailure(CallFederationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			trustChain.add(env.getString("entity_statement"));
		}

		String trustAnchorEntityIdentifier = path.get(path.size() - 1);
		env.putString("entity_statement_url", appendWellKnown(trustAnchorEntityIdentifier));
		callAndContinueOnFailure(CallFederationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		trustChain.add(env.getString("entity_statement"));
		eventLog.endBlock();

		return trustChain;
	}


	protected List<String> validateResolveEndpoint() {
		final String resolveEndpoint = env.getString("federation_resolve_endpoint");
		if (resolveEndpoint == null) {
			fireTestSkipped("Trust anchor does not contain a federation_resolve_endpoint.");
		}

		env.putString("entity_statement_url", resolveEndpoint);
		env.putString("expected_sub", env.getString("primary_entity_statement_iss"));
		callAndContinueOnFailure(AppendSubToFederationEndpointUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(AppendAnchorToEntityStatementUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		eventLog.startBlock("Fetching and validating response from resolve endpoint %s".formatted(resolveEndpoint));

		callAndStopOnFailure(CallFederationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		env.mapKey("endpoint_response", "entity_statement_endpoint_response");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(EnsureContentTypeResolveResponseJwt.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		env.unmapKey("endpoint_response");

		env.putString("expected_iss", env.getString("config", "federation.trust_anchor"));
		callAndContinueOnFailure(ExtractBasicClaimsFromEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		call(sequence(ValidateEntityStatementBasicClaimsSequence.class));

		callAndContinueOnFailure(ExtractJWKsFromTrustAnchorEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		call(sequence(ValidateEntityStatementSignatureSequence.class));

		callAndContinueOnFailure(ExtractTrustChainFromResolveResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		eventLog.endBlock();

		JsonArray trustChain = env.getElementFromObject("trust_chain_from_resolver", "trust_chain").getAsJsonArray();
		List<String> trustChainList = new ArrayList<>();
		for (JsonElement jsonElement : trustChain) {
			trustChainList.add(OIDFJSON.getString(jsonElement));
		}
		return trustChainList;
	}

}
