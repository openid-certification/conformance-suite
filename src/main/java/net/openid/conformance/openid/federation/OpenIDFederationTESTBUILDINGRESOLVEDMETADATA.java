package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@PublishTestModule(
	testName = "openid-federation-TESTBUILDINGRESOLVEDMETADATA",
	displayName = "OpenID Federation: TESTBUILDINGRESOLVEDMETADATA",
	summary = "",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_statement_url",
		"federation.anchor",
		"federation.trust_anchor_jwks",
	}
)
public class OpenIDFederationTESTBUILDINGRESOLVEDMETADATA extends AbstractOpenIDFederationTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		String fromEntity = stripWellKnown(env.getString("config", "federation.entity_statement_url"));
		String trustAnchor = env.getString("config", "federation.anchor");

		List<String> path = findPath(fromEntity, trustAnchor);
		eventLog.log(getName(), "[" + Strings.join(path, ',') + "]");

		fireTestFinished();
	}

	protected List<String> findPath(String fromEntity, String trustAnchor) {
		return findPath(fromEntity, trustAnchor, new ArrayList<>());
	}

	protected List<String> findPath(String fromEntity, String trustAnchor, List<String> path) {
		path.add(fromEntity);

		if (fromEntity.equals(trustAnchor)) {
			return path;
		}

		String currentWellKnownUrl = appendWellKnown(fromEntity);
		env.putString("entity_statement_url", currentWellKnownUrl);
		callAndStopOnFailure(GetEntityStatement.class, Condition.ConditionResult.FAILURE);

		JsonElement authorityHintsElement = env.getElementFromObject("entity_statement_body", "authority_hints");
		if (authorityHintsElement == null) {
			return null;
		}
		JsonArray authorityHints = authorityHintsElement.getAsJsonArray();
		if (authorityHints.isJsonNull() || authorityHints.isEmpty()) {
			return null;
		}

		for (JsonElement authorityHintElement : authorityHints) {
			String authorityHint = OIDFJSON.getString(authorityHintElement);
			List<String> result = findPath(authorityHint, trustAnchor, new ArrayList<>(path));
			if (result != null) {
				return result;
			}
		}

		return null;
	}
}
