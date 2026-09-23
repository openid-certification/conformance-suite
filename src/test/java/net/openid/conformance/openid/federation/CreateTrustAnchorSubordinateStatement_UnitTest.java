package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CreateTrustAnchorSubordinateStatement_UnitTest {

	private static final String TRUST_ANCHOR = "https://suite.example/test/federation/trust-anchor";
	private static final String FETCH_ENDPOINT = TRUST_ANCHOR + "/fetch";

	private Environment env;
	private CreateTrustAnchorSubordinateStatement condition;

	@BeforeEach
	public void setUp() {
		env = new Environment();
		env.putString("trust_anchor_entity_identifier", TRUST_ANCHOR);
		env.putObject("trust_anchor", new JsonObject());
		env.putString("trust_anchor", "metadata.federation_entity.federation_fetch_endpoint", FETCH_ENDPOINT);
		env.putObject("federation_response_jwt", JsonParser.parseString("""
			{
				"claims": {
					"iss": "https://rp.example",
					"sub": "https://rp.example",
					"iat": 100,
					"exp": 200,
					"jwks": { "keys": [] },
					"metadata": { "openid_relying_party": { "client_name": "Test RP" } },
					"authority_hints": ["https://suite.example/test/federation/trust-anchor"],
					"trust_mark_issuers": {},
					"trust_mark_owners": {}
				}
			}
			""").getAsJsonObject());
		condition = new CreateTrustAnchorSubordinateStatement();
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), Condition.ConditionResult.INFO);
	}

	@ParameterizedTest
	@ValueSource(strings = {"https://suite.example/test/federation/fetch", FETCH_ENDPOINT, "https://other.example/fetch"})
	public void usesTrustAnchorMetadataRegardlessOfSharedFetchEndpoint(String sharedFetchEndpoint) {
		env.putString("federation_fetch_endpoint", sharedFetchEndpoint);
		condition.execute(env);
		assertEquals(FETCH_ENDPOINT, env.getString("trust_anchor_fetch_response_claims", "source_endpoint"));
	}

	@Test
	public void usesTrustAnchorMetadataWithoutSharedFetchEndpoint() {
		condition.execute(env);
		assertEquals(FETCH_ENDPOINT, env.getString("trust_anchor_fetch_response_claims", "source_endpoint"));
	}

	@Test
	public void preservesSubjectClaimsAndRemovesEntityConfigurationOnlyClaims() {
		JsonObject original = env.getElementFromObject("federation_response_jwt", "claims").getAsJsonObject().deepCopy();
		condition.execute(env);
		JsonObject claims = env.getObject("trust_anchor_fetch_response_claims");

		assertEquals(TRUST_ANCHOR, env.getString("trust_anchor_fetch_response_claims", "iss"));
		for (String claim : new String[]{"sub", "iat", "exp", "jwks", "metadata"}) {
			assertEquals(original.get(claim), claims.get(claim), claim);
		}
		for (String claim : new String[]{"authority_hints", "trust_mark_issuers", "trust_mark_owners"}) {
			assertFalse(claims.has(claim), claim);
		}
	}

}
