package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
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

@ExtendWith(MockitoExtension.class)
class EnsureDiscoveryMetadataResponseValid_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureDiscoveryMetadataResponseValid cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureDiscoveryMetadataResponseValid();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void put(String json) {
		env.putObject("discovery_endpoint_response", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void ok_json_succeeds() {
		put("""
			{ "status": 200, "headers": { "content-type": "application/json" } }""");
		cond.execute(env);
	}

	@Test
	public void ok_jsonWithCharset_inHeaderArray_succeeds() {
		put("""
			{ "status": 200, "headers": { "content-type": [ "application/json; charset=utf-8" ] } }""");
		cond.execute(env);
	}

	@Test
	public void non200_fails() {
		put("""
			{ "status": 203, "headers": { "content-type": "application/json" } }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void missingContentType_fails() {
		put("""
			{ "status": 200, "headers": {} }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void nonJsonContentType_fails() {
		put("""
			{ "status": 200, "headers": { "content-type": "text/html" } }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void ok_jsonUpperCase_succeeds() {
		put("""
			{ "status": 200, "headers": { "content-type": "Application/JSON" } }""");
		cond.execute(env);
	}

	@Test
	public void jsonPrefixedMediaType_fails() {
		put("""
			{ "status": 200, "headers": { "content-type": "application/json-seq" } }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void jsonPrefixedMediaTypeWithParameter_fails() {
		put("""
			{ "status": 200, "headers": { "content-type": "application/jsonx; charset=utf-8" } }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void missingStatus_fails() {
		put("""
			{ "headers": { "content-type": "application/json" } }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
