package net.openid.conformance.condition.client;

import com.authlete.sd.Disclosure;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckSdJwtArrayElementDisclosures_UnitTest {

	private static final String FIRST = "[\"nationalities\", 0]";
	private static final String ALL = "[\"nationalities\", null]";

	// array element disclosures: ["salt", value]
	private static final String DE = "[\"salt-de\",\"DE\"]";
	private static final String FR = "[\"salt-fr\",\"FR\"]";

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckSdJwtArrayElementDisclosures cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckSdJwtArrayElementDisclosures();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private static String digestOf(String disclosureJson) {
		return digestOf(disclosureJson, "sha-256");
	}

	private static String digestOf(String disclosureJson, String sdAlg) {
		String base64url = Base64.getUrlEncoder().withoutPadding()
			.encodeToString(disclosureJson.getBytes(StandardCharsets.UTF_8));
		return Disclosure.parse(base64url).digest(sdAlg);
	}

	private static String placeholder(String disclosureJson) {
		return placeholder(disclosureJson, "sha-256");
	}

	private static String placeholder(String disclosureJson, String sdAlg) {
		return "{\"...\":\"" + digestOf(disclosureJson, sdAlg) + "\"}";
	}

	/**
	 * @param path the DCQL claim path, as JSON
	 * @param rawPayload the issuer-signed JWT payload, as JSON
	 * @param decoded the claims after applying the presented disclosures, as JSON
	 * @param disclosures the JSON text of each presented disclosure
	 */
	private void setup(String path, String rawPayload, String decoded, String... disclosures) {
		env.putString("credential_id", "my_credential");
		env.putObject("dcql_query", JsonParser.parseString("""
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": { "vct_values": [ "urn:eudi:pid:1" ] },
			      "claims": [ { "path": %s } ]
			    }
			  ]
			}
			""".formatted(path)).getAsJsonObject());

		JsonObject credential = new JsonObject();
		credential.add("claims", JsonParser.parseString(rawPayload).getAsJsonObject());
		JsonArray disclosureArray = new JsonArray();
		for (String d : disclosures) {
			disclosureArray.add(d);
		}
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		sdjwt.add("decoded", JsonParser.parseString(decoded).getAsJsonObject());
		sdjwt.add("disclosures", disclosureArray);
		env.putObject("sdjwt", sdjwt);
	}

	@Test
	public void first_onlyFirstSdElementDisclosed_passes() {
		setup(FIRST,
			"{\"nationalities\":[" + placeholder(DE) + "," + placeholder(FR) + "]}",
			"{\"nationalities\":[\"DE\"]}",
			DE);

		cond.execute(env);
	}

	@Test
	public void first_secondSdElementAlsoDisclosed_throws() {
		setup(FIRST,
			"{\"nationalities\":[" + placeholder(DE) + "," + placeholder(FR) + "]}",
			"{\"nationalities\":[\"DE\",\"FR\"]}",
			DE, FR);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void first_plainFirstElementPlusSdElementDisclosed_throws() {
		// index 0 is the always-visible "DE"; disclosing the selectively disclosable "FR" was not selected
		setup(FIRST,
			"{\"nationalities\":[\"DE\"," + placeholder(FR) + "]}",
			"{\"nationalities\":[\"DE\",\"FR\"]}",
			FR);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void first_laterSdElementDisclosedAfterUnresolvedPlaceholder_passes() {
		// the unresolved first placeholder may be a decoy digest, so the disclosed element may be index 0
		setup(FIRST,
			"{\"nationalities\":[" + placeholder("[\"salt-decoy\",\"XX\"]") + "," + placeholder(FR) + "]}",
			"{\"nationalities\":[\"FR\"]}",
			FR);

		cond.execute(env);
	}

	@Test
	public void first_wholeArrayIsOneDisclosure_passes() {
		// the issuer made the array, not its elements, selectively disclosable: the wallet cannot split it
		String arrayDisclosure = "[\"salt-arr\",\"nationalities\",[\"DE\",\"FR\"]]";
		setup(FIRST,
			"{\"_sd\":[\"" + digestOf(arrayDisclosure) + "\"]}",
			"{\"nationalities\":[\"DE\",\"FR\"]}",
			arrayDisclosure);

		cond.execute(env);
	}

	@Test
	public void sha384SdAlg_onlyFirstElementDisclosed_passes() {
		setup(FIRST,
			"{\"_sd_alg\":\"sha-384\",\"nationalities\":[" + placeholder(DE, "sha-384") + "," + placeholder(FR, "sha-384") + "]}",
			"{\"nationalities\":[\"DE\"]}",
			DE);

		cond.execute(env);
	}

	@Test
	public void sha384SdAlg_secondElementAlsoDisclosed_throws() {
		// proves the sha-384 digests are really matched, not just tolerated
		setup(FIRST,
			"{\"_sd_alg\":\"sha-384\",\"nationalities\":[" + placeholder(DE, "sha-384") + "," + placeholder(FR, "sha-384") + "]}",
			"{\"nationalities\":[\"DE\",\"FR\"]}",
			DE, FR);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void unsupportedSdAlg_throws() {
		setup(FIRST,
			"{\"_sd_alg\":\"not-a-hash\",\"nationalities\":[" + placeholder(DE) + "]}",
			"{\"nationalities\":[\"DE\"]}",
			DE);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void presentedElementsCannotBeAccountedFor_throws() {
		// "DE" is in the decoded array but its disclosure does not hash to the placeholder: the check
		// must not pass without having checked anything
		setup(FIRST,
			"{\"nationalities\":[{\"...\":\"not-the-digest-of-the-disclosure\"}]}",
			"{\"nationalities\":[\"DE\"]}",
			DE);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void first_noElementDisclosed_throws() {
		setup(FIRST,
			"{\"nationalities\":[" + placeholder(DE) + "]}",
			"{\"nationalities\":[]}");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void first_arrayClaimAbsent_throws() {
		setup(FIRST,
			"{\"_sd\":[\"someDigest\"]}",
			"{}");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void all_everySdElementDisclosed_passes() {
		setup(ALL,
			"{\"nationalities\":[" + placeholder(DE) + "," + placeholder(FR) + "]}",
			"{\"nationalities\":[\"DE\",\"FR\"]}",
			DE, FR);

		cond.execute(env);
	}

	@Test
	public void all_unresolvedPlaceholderRemains_passes() {
		// cannot be distinguished from a decoy digest, so it is logged and not failed
		setup(ALL,
			"{\"nationalities\":[" + placeholder(DE) + "," + placeholder(FR) + "]}",
			"{\"nationalities\":[\"DE\"]}",
			DE);

		cond.execute(env);
	}

	@Test
	public void all_noElementDisclosed_throws() {
		setup(ALL,
			"{\"nationalities\":[" + placeholder(DE) + "]}",
			"{\"nationalities\":[]}");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void indexOtherThanZero_throws() {
		setup("[\"nationalities\", 1]",
			"{\"nationalities\":[" + placeholder(DE) + "," + placeholder(FR) + "]}",
			"{\"nationalities\":[\"FR\"]}",
			FR);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void queryWithoutArraySelector_throws() {
		setup("[\"nationalities\"]",
			"{\"nationalities\":[\"DE\"]}",
			"{\"nationalities\":[\"DE\"]}");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
