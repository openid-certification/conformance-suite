package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VCIEnsureSelectiveDisclosureConformsToTypeMetadata_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIEnsureSelectiveDisclosureConformsToTypeMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureSelectiveDisclosureConformsToTypeMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * @param decodedPayload the Processed SD-JWT Payload (disclosures applied)
	 * @param rawClaims      the raw credential JWT payload — disclosed members are
	 *                       omitted and replaced by an {@code "_sd"} placeholder in
	 *                       their parent object; clear members appear literally. The
	 *                       walk only consults {@code _sd} <em>presence</em>, so the
	 *                       placeholder digest values are irrelevant.
	 * @param disclosures    raw disclosure JSON strings ({@code [salt, name, value]}
	 *                       or {@code [salt, value]})
	 */
	private void put(String decodedPayload, String rawClaims, String[] disclosures, String typeMetadataClaimsJson) {
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("decoded", JsonParser.parseString(decodedPayload).getAsJsonObject());
		JsonObject credential = new JsonObject();
		credential.add("claims", JsonParser.parseString(rawClaims).getAsJsonObject());
		sdjwt.add("credential", credential);
		JsonArray arr = new JsonArray();
		for (String d : disclosures) {
			arr.add(d);
		}
		sdjwt.add("disclosures", arr);
		env.putObject("sdjwt", sdjwt);

		JsonObject vci = new JsonObject();
		JsonObject tm = new JsonObject();
		tm.add("claims", JsonParser.parseString(typeMetadataClaimsJson).getAsJsonArray());
		vci.add("sdjwt_vc_type_metadata", tm);
		env.putObject("vci", vci);
	}

	@Test
	public void alwaysWithMatchingDisclosure_passes() {
		put(
			"{\"given_name\":\"Erika\"}",
			"{\"_sd\":[\"x\"]}",
			new String[]{ "[\"salt\",\"given_name\",\"Erika\"]" },
			"[ { \"path\": [\"given_name\"], \"sd\": \"always\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void alwaysWithoutMatchingDisclosure_fails() {
		// given_name is a clear claim (present literally in the raw payload), so
		// sd: always is violated.
		put(
			"{\"given_name\":\"Erika\"}",
			"{\"given_name\":\"Erika\"}",
			new String[]{ "[\"salt\",\"family_name\",\"Mustermann\"]" },
			"[ { \"path\": [\"given_name\"], \"sd\": \"always\" } ]"
		);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("§9.4"));
	}

	@Test
	public void neverWithoutMatchingDisclosure_passes() {
		put(
			"{\"given_name\":\"Erika\"}",
			"{\"given_name\":\"Erika\"}",
			new String[]{ "[\"salt\",\"family_name\",\"Mustermann\"]" },
			"[ { \"path\": [\"given_name\"], \"sd\": \"never\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void neverWithMatchingDisclosure_fails() {
		put(
			"{\"given_name\":\"Erika\"}",
			"{\"_sd\":[\"x\"]}",
			new String[]{ "[\"salt\",\"given_name\",\"Erika\"]" },
			"[ { \"path\": [\"given_name\"], \"sd\": \"never\" } ]"
		);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void allowedHasNoConstraint_passes() {
		put(
			"{\"given_name\":\"Erika\"}",
			"{\"_sd\":[\"x\"]}",
			new String[]{ "[\"salt\",\"given_name\",\"Erika\"]" },
			"[ { \"path\": [\"given_name\"], \"sd\": \"allowed\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void omittedSdHasNoConstraint_passes() {
		put(
			"{\"given_name\":\"Erika\"}",
			"{\"_sd\":[\"x\"]}",
			new String[]{ "[\"salt\",\"given_name\",\"Erika\"]" },
			"[ { \"path\": [\"given_name\"] } ]"
		);
		cond.execute(env);
	}

	@Test
	public void claimAbsentFromPayload_isNotApplicable() {
		// sd applies only when the claim is present in the credential.
		put(
			"{\"family_name\":\"Mustermann\"}",
			"{\"family_name\":\"Mustermann\"}",
			new String[]{ },
			"[ { \"path\": [\"given_name\"], \"sd\": \"always\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void multiSegmentStringPath_alwaysWithDisclosure_passes() {
		// address is a clear object; address.country is selectively disclosed inside it.
		put(
			"{\"address\":{\"country\":\"DE\"}}",
			"{\"address\":{\"_sd\":[\"x\"]}}",
			new String[]{ "[\"salt\",\"country\",\"DE\"]" },
			"[ { \"path\": [\"address\",\"country\"], \"sd\": \"always\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void multiSegmentStringPath_neverWithDisclosure_fails() {
		put(
			"{\"address\":{\"country\":\"DE\"}}",
			"{\"address\":{\"_sd\":[\"x\"]}}",
			new String[]{ "[\"salt\",\"country\",\"DE\"]" },
			"[ { \"path\": [\"address\",\"country\"], \"sd\": \"never\" } ]"
		);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void multiSegmentStringPath_alwaysWithoutDisclosure_fails() {
		// address.country is a clear claim, so sd: always is violated.
		put(
			"{\"address\":{\"country\":\"DE\"}}",
			"{\"address\":{\"country\":\"DE\"}}",
			new String[]{ "[\"salt\",\"locality\",\"Berlin\"]" },
			"[ { \"path\": [\"address\",\"country\"], \"sd\": \"always\" } ]"
		);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void arrayIndexAlwaysDisclosed_passes() {
		// nationalities[0] is a selectively-disclosed array element ({"...": digest}
		// in the raw payload); sd: always is satisfied.
		put(
			"{\"nationalities\":[\"GB\",\"DE\"]}",
			"{\"nationalities\":[{\"...\":\"d0\"},\"DE\"]}",
			new String[]{ "[\"salt\",\"GB\"]" },
			"[ { \"path\": [\"nationalities\",0], \"sd\": \"always\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void arrayIndexAlwaysClearElement_fails() {
		// nationalities[1] is a plain (clear) array element; sd: always is violated.
		put(
			"{\"nationalities\":[\"GB\",\"DE\"]}",
			"{\"nationalities\":[{\"...\":\"d0\"},\"DE\"]}",
			new String[]{ "[\"salt\",\"GB\"]" },
			"[ { \"path\": [\"nationalities\",1], \"sd\": \"always\" } ]"
		);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void arrayWildcardNeverOneElementDisclosed_fails() {
		// sd: never over all elements is violated because one element is disclosed.
		put(
			"{\"nationalities\":[\"GB\",\"DE\"]}",
			"{\"nationalities\":[{\"...\":\"d0\"},\"DE\"]}",
			new String[]{ "[\"salt\",\"GB\"]" },
			"[ { \"path\": [\"nationalities\",null], \"sd\": \"never\" } ]"
		);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void arrayWildcardNeverNoneDisclosed_passes() {
		// All elements are clear, so sd: never over the wildcard holds.
		put(
			"{\"nationalities\":[\"GB\",\"DE\"]}",
			"{\"nationalities\":[\"GB\",\"DE\"]}",
			new String[]{ },
			"[ { \"path\": [\"nationalities\",null], \"sd\": \"never\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void arrayWildcardAlwaysAllDisclosed_passes() {
		// Every element is a selectively-disclosed placeholder, so sd: always holds.
		put(
			"{\"nationalities\":[\"GB\",\"DE\"]}",
			"{\"nationalities\":[{\"...\":\"d0\"},{\"...\":\"d1\"}]}",
			new String[]{ "[\"salt\",\"GB\"]", "[\"salt\",\"DE\"]" },
			"[ { \"path\": [\"nationalities\",null], \"sd\": \"always\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void arrayIndexOutOfRange_isNotApplicable() {
		put(
			"{\"nationalities\":[\"GB\"]}",
			"{\"nationalities\":[\"GB\"]}",
			new String[]{ },
			"[ { \"path\": [\"nationalities\",5], \"sd\": \"always\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void pathDescendingPastDisclosedArrayElement_isInconclusiveAndSkipped() {
		// degrees[0] is itself a selectively-disclosed array element; relating the
		// disclosure to "type" inside it needs digest matching, so an sd:always on
		// the deeper path is skipped, not failed.
		put(
			"{\"degrees\":[{\"type\":\"BSc\"}]}",
			"{\"degrees\":[{\"...\":\"d0\"}]}",
			new String[]{ "[\"salt\",{\"type\":\"BSc\"}]" },
			"[ { \"path\": [\"degrees\",0,\"type\"], \"sd\": \"always\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void noClaimsArray_passes() {
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("disclosures", new JsonArray());
		env.putObject("sdjwt", sdjwt);
		JsonObject vci = new JsonObject();
		vci.add("sdjwt_vc_type_metadata", new JsonObject());
		env.putObject("vci", vci);
		cond.execute(env);
	}

	@Test
	public void twoElementDisclosuresIgnored() {
		// A [salt, value] array-element disclosure is not an object-property
		// disclosure, so given_name here is a clear claim and sd: always fails.
		put(
			"{\"given_name\":\"Erika\"}",
			"{\"given_name\":\"Erika\"}",
			new String[]{ "[\"salt\",\"Erika\"]" },
			"[ { \"path\": [\"given_name\"], \"sd\": \"always\" } ]"
		);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	// --- Regression cases for the path-precise selective-disclosure fix ---

	@Test
	public void siblingNameCollision_topLevelNotDisclosed_neverPasses() {
		// Top-level "country" is a clear claim; a same-named "country" nested under
		// "address" is the one selectively disclosed. The flat leaf-name matcher
		// wrongly flagged ["country"] sd:never; the path-precise walk does not.
		put(
			"{\"country\":\"DE\",\"address\":{\"country\":\"US\"}}",
			"{\"country\":\"DE\",\"address\":{\"_sd\":[\"x\"]}}",
			new String[]{ "[\"salt\",\"country\",\"US\"]" },
			"[ { \"path\": [\"country\"], \"sd\": \"never\" },"
				+ " { \"path\": [\"address\",\"country\"], \"sd\": \"allowed\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void siblingNameCollision_nestedDisclosed_neverFails() {
		// The nested address.country IS disclosed, so sd:never on it must fail —
		// proving the walk attributes the disclosure to the correct path.
		put(
			"{\"country\":\"DE\",\"address\":{\"country\":\"US\"}}",
			"{\"country\":\"DE\",\"address\":{\"_sd\":[\"x\"]}}",
			new String[]{ "[\"salt\",\"country\",\"US\"]" },
			"[ { \"path\": [\"address\",\"country\"], \"sd\": \"never\" } ]"
		);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void duplicateClaimNameDescent_isInconclusiveAndSkipped() {
		// Two disclosures share the claim name "address", so descending into a
		// disclosed "address" object to evaluate its children is ambiguous. An
		// sd:always on a child must be skipped, not failed.
		put(
			"{\"a\":{\"address\":{\"city\":\"X\"}},\"b\":{\"address\":{\"city\":\"Y\"}}}",
			"{\"a\":{\"_sd\":[\"x\"]},\"b\":{\"_sd\":[\"y\"]}}",
			new String[]{
				"[\"s1\",\"address\",{\"city\":\"X\"}]",
				"[\"s2\",\"address\",{\"city\":\"Y\"}]"
			},
			"[ { \"path\": [\"a\",\"address\",\"city\"], \"sd\": \"always\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void missingRawCredentialClaims_isSkippedNotFailed() {
		// Without the raw credential payload the disclosure status cannot be
		// determined, so an sd:always constraint is skipped rather than failed.
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("decoded", JsonParser.parseString("{\"given_name\":\"Erika\"}").getAsJsonObject());
		JsonArray disclosures = new JsonArray();
		disclosures.add("[\"salt\",\"given_name\",\"Erika\"]");
		sdjwt.add("disclosures", disclosures);
		env.putObject("sdjwt", sdjwt);

		JsonObject vci = new JsonObject();
		JsonObject tm = new JsonObject();
		tm.add("claims", JsonParser.parseString("[ { \"path\": [\"given_name\"], \"sd\": \"always\" } ]").getAsJsonArray());
		vci.add("sdjwt_vc_type_metadata", tm);
		env.putObject("vci", vci);

		cond.execute(env);
	}

	@Test
	public void parentObjectOnlySd_childTreatedAsNotIndependentlyDisclosed() {
		// The whole "address" object is selectively disclosed; "country" is a
		// literal member of the disclosure value, so it is not independently
		// selectively disclosable. sd:always on ["address"] is satisfied but
		// sd:always on ["address","country"] is not — overall a failure.
		put(
			"{\"address\":{\"country\":\"DE\"}}",
			"{\"_sd\":[\"x\"]}",
			new String[]{ "[\"salt\",\"address\",{\"country\":\"DE\"}]" },
			"[ { \"path\": [\"address\"], \"sd\": \"always\" },"
				+ " { \"path\": [\"address\",\"country\"], \"sd\": \"always\" } ]"
		);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void parentObjectSd_parentAlwaysPasses() {
		// The disclosed parent object itself satisfies sd:always on ["address"].
		put(
			"{\"address\":{\"country\":\"DE\"}}",
			"{\"_sd\":[\"x\"]}",
			new String[]{ "[\"salt\",\"address\",{\"country\":\"DE\"}]" },
			"[ { \"path\": [\"address\"], \"sd\": \"always\" } ]"
		);
		cond.execute(env);
	}
}
