package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
public class VCIExtractSdJwtVcTypeMetadataUrl_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIExtractSdJwtVcTypeMetadataUrl cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIExtractSdJwtVcTypeMetadataUrl();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void putVct(String vct) {
		JsonObject sdjwt = JsonParser.parseString("{\"credential\":{\"claims\":{\"vct\":\"" + vct + "\"}}}").getAsJsonObject();
		env.putObject("sdjwt", sdjwt);
	}

	@Test
	public void httpsVct_storesUrl() {
		putVct("https://issuer.example.com/credentials/pid");
		cond.execute(env);
		assertEquals("https://issuer.example.com/credentials/pid",
			env.getString("vci", "sdjwt_vc_type_metadata_url"));
	}

	@Test
	public void httpsVctMixedCaseScheme_storesUrl() {
		putVct("HTTPS://issuer.example.com/x");
		cond.execute(env);
		assertEquals("HTTPS://issuer.example.com/x",
			env.getString("vci", "sdjwt_vc_type_metadata_url"));
	}

	@Test
	public void urnVct_doesNotStoreUrl() {
		putVct("urn:eudi:pid:1");
		cond.execute(env);
		assertNull(env.getString("vci", "sdjwt_vc_type_metadata_url"));
	}

	@Test
	public void httpVct_doesNotStoreUrl() {
		putVct("http://issuer.example.com/pid");
		cond.execute(env);
		assertNull(env.getString("vci", "sdjwt_vc_type_metadata_url"));
	}

	@Test
	public void invalidUri_doesNotStoreUrlAndDoesNotThrow() {
		putVct("not a uri at all");
		cond.execute(env);
		assertNull(env.getString("vci", "sdjwt_vc_type_metadata_url"));
	}

	@Test
	public void missingVct_doesNotStoreUrl() {
		env.putObject("sdjwt", JsonParser.parseString("{\"credential\":{\"claims\":{}}}").getAsJsonObject());
		cond.execute(env);
		assertNull(env.getString("vci", "sdjwt_vc_type_metadata_url"));
	}

	@Test
	public void staleStateFromPriorCredentialIsCleared() {
		// Simulate a prior credential having fetched Type Metadata and set
		// chain_ready. The current credential has a non-HTTPS vct, so the
		// downstream chain MUST NOT see the prior credential's state.
		JsonObject vci = JsonParser.parseString("""
			{
				"sdjwt_vc_type_metadata_url": "https://prior.example.com/type",
				"sdjwt_vc_type_metadata": { "vct": "https://prior.example.com/type" },
				"sdjwt_vc_type_metadata_endpoint_response": { "body": "{}" },
				"sdjwt_vc_type_metadata_chain_ready": "true"
			}
			""").getAsJsonObject();
		env.putObject("vci", vci);
		putVct("urn:eudi:pid:1");
		cond.execute(env);

		assertNull(env.getString("vci", "sdjwt_vc_type_metadata_url"));
		assertNull(env.getElementFromObject("vci", "sdjwt_vc_type_metadata"));
		assertNull(env.getElementFromObject("vci", "sdjwt_vc_type_metadata_endpoint_response"));
		assertNull(env.getString("vci", "sdjwt_vc_type_metadata_chain_ready"));
	}

	@Test
	public void staleStateIsClearedEvenWhenNewVctIsAlsoHttps() {
		// New credential's HTTPS URL should overwrite the prior one and the
		// fetched document / chain_ready flag from the prior iteration must
		// be gone so downstream re-fetches and re-evaluates.
		JsonObject vci = JsonParser.parseString("""
			{
				"sdjwt_vc_type_metadata_url": "https://prior.example.com/type",
				"sdjwt_vc_type_metadata": { "vct": "https://prior.example.com/type" },
				"sdjwt_vc_type_metadata_chain_ready": "true"
			}
			""").getAsJsonObject();
		env.putObject("vci", vci);
		putVct("https://current.example.com/type");
		cond.execute(env);

		assertEquals("https://current.example.com/type",
			env.getString("vci", "sdjwt_vc_type_metadata_url"));
		assertNull(env.getElementFromObject("vci", "sdjwt_vc_type_metadata"));
		assertNull(env.getString("vci", "sdjwt_vc_type_metadata_chain_ready"));
	}
}
