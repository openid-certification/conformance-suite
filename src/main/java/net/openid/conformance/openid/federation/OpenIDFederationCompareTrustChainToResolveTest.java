package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

import java.util.List;

import static net.openid.conformance.openid.federation.EntityUtils.appendWellKnown;
import static net.openid.conformance.openid.federation.EntityUtils.stripWellKnown;

@PublishTestModule(
	testName = "openid-federation-compare-trust-chain-to-resolve",
	displayName = "OpenID Federation: Compare manually built trust chain to resolved trust chain",
	summary =
		"The test will attempt to construct a trust chain from the configured entity to the trust anchor, " +
		"and then compare it to the trust chain obtained from the trust anchor's Resolve endpoint.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_identifier",
		"federation.de_trust_anchor",
		"federation.de_trust_anchor_jwks",
	}
)
public class OpenIDFederationCompareTrustChainToResolveTest extends OpenIDFederationAutomaticClientRegistrationTest {

	@Override
	public void additionalConfiguration() {
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		String fromEntity = stripWellKnown(env.getString("config", "federation.entity_identifier"));
		String trustAnchor = env.getString("config", "federation.de_trust_anchor");

		List<String> path;
		try {
			path = findPath(fromEntity, trustAnchor);
		} catch (CyclicPathException e) {
			throw new TestFailureException(getId(), e.getMessage());
		}

		if (path == null || path.isEmpty()) {
			throw new TestFailureException(getId(), "A trust chain from %s to %s can not be constructed".formatted(fromEntity, trustAnchor));
		}

		eventLog.startBlock("Fetching entity configuration for trust anchor %s".formatted(trustAnchor));
		env.putString("federation_endpoint_url", appendWellKnown(trustAnchor));
		callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
		callAndStopOnFailure(CallFetchEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.1");
		validateFetchResponse();
		callAndStopOnFailure(ExtractJWTFromFederationEndpointResponse.class,  "OIDFED-8.1.2");

		callAndContinueOnFailure(ExtractFederationEntityMetadataUrls.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		callAndContinueOnFailure(SetTrustAnchorEntityStatement.class, Condition.ConditionResult.FAILURE);
		eventLog.endBlock();

		JsonArray trustChainBuiltManually = buildTrustChain(path);
		JsonArray trustChainFromResolver = getTrustChainFromResolveEndpoint(env.getString("federation_resolve_endpoint"));
		if (trustChainFromResolver == null) {
			fireTestSkipped("Resolve response does not contain the trust_chain element.");
		}

		eventLog.startBlock("Comparing manually built trust chain to resolved trust chain");
		JsonObject trustChains = new JsonObject();
		trustChains.add("manual", trustChainBuiltManually);
		trustChains.add("resolved", trustChainFromResolver);
		env.putObject("trust_chains", trustChains);
		callAndContinueOnFailure(CompareTrustChains.class, Condition.ConditionResult.FAILURE, "OIDFED-10.2");
		eventLog.endBlock();

		fireTestFinished();
	}

	protected JsonArray getTrustChainFromResolveEndpoint(String resolveEndpoint) {
		if (resolveEndpoint == null) {
			fireTestSkipped("Trust anchor does not contain a federation_resolve_endpoint.");
		}

		env.putString("federation_endpoint_url", resolveEndpoint);
		env.putString("expected_sub", env.getString("primary_entity_statement_iss"));
		callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
		callAndContinueOnFailure(AppendSubToFederationEndpointUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-8.3.1");
		callAndContinueOnFailure(AppendAnchorToFederationEndpointUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-8.3.1");

		eventLog.startBlock("Retrieving and validating response from resolve endpoint %s".formatted(resolveEndpoint));

		callAndStopOnFailure(CallResolveEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-8.3.1");
		validateResolveResponse();
		callAndStopOnFailure(ExtractJWTFromFederationEndpointResponse.class,  "OIDFED-8.3.2");

		env.putString("expected_iss", env.getString("config", "federation.de_trust_anchor"));
		callAndContinueOnFailure(ExtractRegisteredClaimsFromFederationResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		call(sequence(ValidateFederationResponseBasicClaimsSequence.class));

		callAndContinueOnFailure(ExtractJWKsFromTrustAnchorEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		call(sequence(ValidateFederationResponseSignatureSequence.class));

		callAndContinueOnFailure(ExtractTrustChainFromResolveResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-3");

		eventLog.endBlock();

		JsonElement trustChainFromResolver = env.getElementFromObject("trust_chain_from_resolver", "trust_chain");
		if (trustChainFromResolver != null) {
			return trustChainFromResolver.getAsJsonArray();
		}
		return null;
	}

}
