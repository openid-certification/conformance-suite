package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ExtractTrustChainFromRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExtractTrustChainFromRequestObject cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ExtractTrustChainFromRequestObject();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Initialize the environment with required objects
		env.putObject("trust_chain", new JsonObject());
	}

	@Test
	public void testEvaluate_trustChainInHeader() {
		JsonObject requestObject = new JsonObject();
		JsonObject header = new JsonObject();
		JsonArray trustChain = new JsonArray();
		trustChain.add("statement1");
		trustChain.add("statement2");
		header.add("trust_chain", trustChain);
		requestObject.add("header", header);
		requestObject.add("claims", new JsonObject());

		env.putObject("authorization_request_object", requestObject);

		cond.execute(env);

		JsonArray result = env.getElementFromObject("trust_chain", "trust_chain").getAsJsonArray();
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(2);
		assertThat(OIDFJSON.getString(result.get(0))).isEqualTo("statement1");
		assertThat(OIDFJSON.getString(result.get(1))).isEqualTo("statement2");
	}

	@Test
	public void testEvaluate_trustChainInClaims() {
		JsonObject requestObject = new JsonObject();
		requestObject.add("header", new JsonObject());
		JsonObject claims = new JsonObject();
		JsonArray trustChain = new JsonArray();
		trustChain.add("statement1");
		trustChain.add("statement2");
		claims.add("trust_chain", trustChain);
		requestObject.add("claims", claims);

		env.putObject("authorization_request_object", requestObject);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});

		JsonArray result = env.getElementFromObject("trust_chain", "trust_chain").getAsJsonArray();
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(2);
		assertThat(OIDFJSON.getString(result.get(0))).isEqualTo("statement1");
		assertThat(OIDFJSON.getString(result.get(1))).isEqualTo("statement2");
	}

	@Test
	public void testEvaluate_prioritizeHeader() {
		JsonObject requestObject = new JsonObject();

		JsonObject header = new JsonObject();
		JsonArray headerTrustChain = new JsonArray();
		headerTrustChain.add("header_statement");
		header.add("trust_chain", headerTrustChain);
		requestObject.add("header", header);

		JsonObject claims = new JsonObject();
		JsonArray claimsTrustChain = new JsonArray();
		claimsTrustChain.add("claims_statement");
		claims.add("trust_chain", claimsTrustChain);
		requestObject.add("claims", claims);

		env.putObject("authorization_request_object", requestObject);

		cond.execute(env);

		JsonArray result = env.getElementFromObject("trust_chain", "trust_chain").getAsJsonArray();
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(1);
		assertThat(OIDFJSON.getString(result.get(0))).isEqualTo("header_statement");

	}

	@Test
	public void testEvaluate_noTrustChain() {
		JsonObject requestObject = new JsonObject();
		requestObject.add("header", new JsonObject());
		requestObject.add("claims", new JsonObject());

		env.putObject("authorization_request_object", requestObject);

		cond.execute(env);

		assertThat(env.getElementFromObject("trust_chain", "trust_chain")).isNull();
	}
}
