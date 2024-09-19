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

		/*

			I'm gonna need to set up my own fake entities on localhost so I can test this.

		*/

		/*

			Let's say that we start with [1] https://fapidev-as.authlete.net/.well-known/openid-federation and
			[2] https://trust-anchor.authlete.net/ as the trust anchor:

			We then want to check the authority_hints for [1]. If we find [2] in that list, go to that entity
			and build the resolved metadata using fetch.

			If [2] is not in that list, then we'll have to go into each of the entities in the authority_hints,
			fetch the metadata to see if the trust anchor is in there.

			The point of the above must be to build a linked list from [1] --> [X] --> ... --> [Z] --> [2],
			so that we know which entities go into the trust chain.

			Once that's done, we can assemble the full trust thingy from leaf to anchor or vice versa.

		 */

		String anchor = env.getString("config", "federation.anchor");
		//JsonElement authorityHintsElement = env.getElementFromObject("entity_statement_body", "authority_hints");

		JsonArray fakeAuthorityHints = new JsonArray();
		fakeAuthorityHints.add("https://demo.federation.eudi.wallet.developers.italia.it/");
		fakeAuthorityHints.add("https://federation.sandbox.raidiam.io/federation_entity/0fe98d4d-1dbb-4e9d-bd77-35ed0dcc222b/");
		fakeAuthorityHints.add("https://fapidev-as.authlete.net/");

		String primaryEntity = stripWellKnown(env.getString("config", "federation.entity_statement_url"));

		/*
		if (authorityHintsElement != null) {
			JsonArray authorityHints = fakeAuthorityHints; //authorityHintsElement.getAsJsonArray();
			eventLog.log(getName(), authorityHints.toString());
			for (JsonElement authorityHintElement : authorityHints) {

				String authorityHint = OIDFJSON.getString(authorityHintElement);
				String authorityHintUrl = appendWellKnown(authorityHint);

				if (Objects.equals(anchor, authorityHint)) {
					eventLog.log(getName(), "YEY FOUND THE ANCHOR!");
				}

				// Get the entity statement for the Superior
				env.putString("entity_statement_url", authorityHintUrl);
				callAndStopOnFailure(GetEntityStatement.class, Condition.ConditionResult.FAILURE);
			}
		}
		*/

		List<String> path = findPath(primaryEntity, anchor, new ArrayList<>());
		eventLog.log(getName(), "[" + Strings.join(path, ',') + "]");

		fireTestFinished();
	}

	protected List<String> findPath(String current, String trustAnchor, List<String> path) {
		path.add(current);

		if (current.equals(trustAnchor)) {
			return path;
		}

		String currentWellKnownUrl = appendWellKnown(current);
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
