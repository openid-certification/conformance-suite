package net.openid.conformance.condition;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.hc.client5.http.classic.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * This tests the behavior of the various utility methods in the
 * abstract superclass used by most conditions.
 */
@ExtendWith(MockitoExtension.class)
public class AbstractCondition_UnitTest {

	private static final String TEST_ID = "UNIT-TEST";

	private static final String TEST_CLASS_NAME = AbstractConditionTester.class.getSimpleName();

	// plain condition
	private AbstractConditionTester cond;
	// condition with requirements
	private AbstractConditionTester condReqs;

	// optional condition
	private AbstractConditionTester opt;
	// optional condition with requirements
	private AbstractConditionTester optReqs;

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject obj;

	private Map<String, Object> map;

	@Captor
	private ArgumentCaptor<JsonObject> objCaptor;

	@Captor
	private ArgumentCaptor<Map<String, Object>> mapCaptor;

	private String msg = "message string";

	private String req1 = "requirement-1";

	private String req2 = "requirement-2";

	private Throwable cause = new Throwable("throwable message");

	@BeforeEach
	public void setUp() throws Exception {

		cond = new AbstractConditionTester();
		cond.setProperties(TEST_ID, eventLog, ConditionResult.FAILURE);

		condReqs = new AbstractConditionTester();
		condReqs.setProperties(TEST_ID, eventLog, ConditionResult.FAILURE, req1, req2);

		opt = new AbstractConditionTester();
		opt.setProperties(TEST_ID, eventLog, ConditionResult.INFO);

		optReqs = new AbstractConditionTester();
		optReqs.setProperties(TEST_ID, eventLog, ConditionResult.WARNING, req1, req2);

		obj = JsonParser.parseString("{\"foo\": \"bar\", \"baz\": 1234}").getAsJsonObject();

		map = ImmutableMap.of("foo", "bar", "baz", 1234);

	}

	@Test
	public void testArgs_evenList() {
		Map<String, Object> args = cond.args("foo", "bar", "baz", "quz");

		assertThat(args.size()).isEqualTo(2);
		assertThat(args.get("foo")).isEqualTo("bar");
		assertThat(args.get("baz")).isEqualTo("quz");
	}

	@Test
	public void testArgs_oddList() {
		assertThrows(IllegalArgumentException.class, () -> {
			cond.args("foo", "bar", "baz", "quz", "batman");
		});
	}

	@Test
	public void testArgs_null() {
		assertThrows(IllegalArgumentException.class, () -> {
			cond.args((Object[]) null);
		});
	}

	@Test
	public void testArgs_emptyList() {
		Map<String, Object> args = cond.args();

		assertThat(args.size()).isEqualTo(0);
	}

	@Test
	public void testLog_jsonObj() {

		cond.log(obj);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		assertThat(res.size()).isEqualTo(obj.size());

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}

	}

	@Test
	public void testLog_string() {

		cond.log(msg);

		verify(eventLog).log(eq(TEST_CLASS_NAME), eq(msg));

	}

	@Test
	public void testLog_map() {

		cond.log(map);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		assertThat(res.size()).isEqualTo(map.size());

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}

	}

	@Test
	public void testLog_stringJsonObj() {

		cond.log(msg, obj);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "msg"
		assertThat(res.size()).isEqualTo(obj.size() + 1);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("msg"))).isEqualTo(msg);

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}

	}

	@Test
	public void testLog_stringMap() {

		cond.log(msg, map);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg"
		assertThat(res.size()).isEqualTo(map.size() + 1);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}

	/*
	 * Test all the "success" methods
	 */

	@Test
	public void testLogSuccess_jsonObj() {
		cond.logSuccess(obj);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "result"
		assertThat(res.size()).isEqualTo(obj.size() + 1);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("result"))).isEqualTo("SUCCESS");

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testLogSuccess_jsonObj_withReqs() {
		condReqs.logSuccess(obj);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "result" and "requirements"
		assertThat(res.size()).isEqualTo(obj.size() + 2);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("result"))).isEqualTo("SUCCESS");

		assertThat(res.has("requirements")).isEqualTo(true);
		assertThat(res.get("requirements").isJsonArray()).isEqualTo(true);

		JsonArray reqs = res.get("requirements").getAsJsonArray();
		assertThat(reqs.contains(new JsonPrimitive(req1))).isEqualTo(true);
		assertThat(reqs.contains(new JsonPrimitive(req2))).isEqualTo(true);

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testLogSuccess_string() {
		cond.logSuccess(msg);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "results"
		assertThat(res.size()).isEqualTo(2);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("SUCCESS");

	}

	@Test
	public void testLogSuccess_string_withReqs() {
		condReqs.logSuccess(msg);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "results" and "requirements"
		assertThat(res.size()).isEqualTo(3);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("SUCCESS");

		assertThat(res.containsKey("requirements"));
		assertThat(res.get("requirements")).isInstanceOf(Collection.class);

		@SuppressWarnings("unchecked")
		Collection<String> reqs = (Collection<String>) res.get("requirements");

		assertThat(reqs.size()).isEqualTo(2);
		assertThat(reqs.contains(req1));
		assertThat(reqs.contains(req2));

	}

	@Test
	public void testLogSuccess_map() {
		cond.logSuccess(map);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "result"
		assertThat(res.size()).isEqualTo(map.size() + 1);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("SUCCESS");

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}

	@Test
	public void testLogSuccess_map_withReqs() {
		condReqs.logSuccess(map);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "result" and "requirements"
		assertThat(res.size()).isEqualTo(map.size() + 2);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("SUCCESS");

		assertThat(res.containsKey("requirements"));
		assertThat(res.get("requirements")).isInstanceOf(Collection.class);

		@SuppressWarnings("unchecked")
		Collection<String> reqs = (Collection<String>) res.get("requirements");

		assertThat(reqs.size()).isEqualTo(2);
		assertThat(reqs.contains(req1));
		assertThat(reqs.contains(req2));

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}

	@Test
	public void testLogSuccess_stringMap() {
		cond.logSuccess(msg, map);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "result"
		assertThat(res.size()).isEqualTo(map.size() + 2);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("SUCCESS");

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}

	@Test
	public void testLogSuccess_stringMap_withReqs() {
		condReqs.logSuccess(msg, map);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(map.size() + 3);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("SUCCESS");

		assertThat(res.containsKey("requirements"));
		assertThat(res.get("requirements")).isInstanceOf(Collection.class);

		@SuppressWarnings("unchecked")
		Collection<String> reqs = (Collection<String>) res.get("requirements");

		assertThat(reqs.size()).isEqualTo(2);
		assertThat(reqs.contains(req1));
		assertThat(reqs.contains(req2));

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}

	@Test
	public void testLogSuccess_stringJsonObj() {
		cond.logSuccess(msg, obj);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "msg" and "result"
		assertThat(res.size()).isEqualTo(obj.size() + 2);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("msg"))).isEqualTo(msg);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("result"))).isEqualTo("SUCCESS");

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testLogSuccess_stringJsonObj_withReqs() {
		condReqs.logSuccess(msg, obj);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "msg" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(obj.size() + 3);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("msg"))).isEqualTo(msg);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("result"))).isEqualTo("SUCCESS");

		assertThat(res.has("requirements")).isEqualTo(true);
		assertThat(res.get("requirements").isJsonArray()).isEqualTo(true);

		JsonArray reqs = res.get("requirements").getAsJsonArray();
		assertThat(reqs.contains(new JsonPrimitive(req1))).isEqualTo(true);
		assertThat(reqs.contains(new JsonPrimitive(req2))).isEqualTo(true);

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	/*
	 * Test all the "failure" methods when "optional" is set to "false"
	 */

	@Test
	public void testLogFailure_jsonObj() {
		cond.logFailure(obj);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "result"
		assertThat(res.size()).isEqualTo(obj.size() + 1);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("result"))).isEqualTo("FAILURE");

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testLogFailure_jsonObj_withReqs() {
		condReqs.logFailure(obj);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "result" and "requirements"
		assertThat(res.size()).isEqualTo(obj.size() + 2);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("result"))).isEqualTo("FAILURE");

		assertThat(res.has("requirements")).isEqualTo(true);
		assertThat(res.get("requirements").isJsonArray()).isEqualTo(true);

		JsonArray reqs = res.get("requirements").getAsJsonArray();
		assertThat(reqs.contains(new JsonPrimitive(req1))).isEqualTo(true);
		assertThat(reqs.contains(new JsonPrimitive(req2))).isEqualTo(true);

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testLogFailure_string() {
		cond.logFailure(msg);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "results"
		assertThat(res.size()).isEqualTo(2);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("FAILURE");

	}

	@Test
	public void testLogFailure_string_withReqs() {
		condReqs.logFailure(msg);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "results" and "requirements"
		assertThat(res.size()).isEqualTo(3);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("FAILURE");

		assertThat(res.containsKey("requirements"));
		assertThat(res.get("requirements")).isInstanceOf(Collection.class);

		@SuppressWarnings("unchecked")
		Collection<String> reqs = (Collection<String>) res.get("requirements");

		assertThat(reqs.size()).isEqualTo(2);
		assertThat(reqs.contains(req1));
		assertThat(reqs.contains(req2));

	}

	@Test
	public void testLogFailure_map() {
		cond.logFailure(map);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "result"
		assertThat(res.size()).isEqualTo(map.size() + 1);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("FAILURE");

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}

	@Test
	public void testLogFailure_map_withReqs() {
		condReqs.logFailure(map);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "result" and "requirements"
		assertThat(res.size()).isEqualTo(map.size() + 2);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("FAILURE");

		assertThat(res.containsKey("requirements"));
		assertThat(res.get("requirements")).isInstanceOf(Collection.class);

		@SuppressWarnings("unchecked")
		Collection<String> reqs = (Collection<String>) res.get("requirements");

		assertThat(reqs.size()).isEqualTo(2);
		assertThat(reqs.contains(req1));
		assertThat(reqs.contains(req2));

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}

	@Test
	public void testLogFailure_stringMap() {
		cond.logFailure(msg, map);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "result"
		assertThat(res.size()).isEqualTo(map.size() + 2);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("FAILURE");

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}

	@Test
	public void testLogFailure_stringMap_withReqs() {
		condReqs.logFailure(msg, map);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(map.size() + 3);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("FAILURE");

		assertThat(res.containsKey("requirements"));
		assertThat(res.get("requirements")).isInstanceOf(Collection.class);

		@SuppressWarnings("unchecked")
		Collection<String> reqs = (Collection<String>) res.get("requirements");

		assertThat(reqs.size()).isEqualTo(2);
		assertThat(reqs.contains(req1));
		assertThat(reqs.contains(req2));

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}

	@Test
	public void testLogFailure_stringJsonObj() {
		cond.logFailure(msg, obj);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "msg" and "result"
		assertThat(res.size()).isEqualTo(obj.size() + 2);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("msg"))).isEqualTo(msg);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("result"))).isEqualTo("FAILURE");

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testLogFailure_stringJsonObj_withReqs() {
		condReqs.logFailure(msg, obj);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "msg" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(obj.size() + 3);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("msg"))).isEqualTo(msg);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("result"))).isEqualTo("FAILURE");

		assertThat(res.has("requirements")).isEqualTo(true);
		assertThat(res.get("requirements").isJsonArray()).isEqualTo(true);

		JsonArray reqs = res.get("requirements").getAsJsonArray();
		assertThat(reqs.contains(new JsonPrimitive(req1))).isEqualTo(true);
		assertThat(reqs.contains(new JsonPrimitive(req2))).isEqualTo(true);

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	/*
	 * Test all the "failure" methods when "optional" is set to "true"
	 */

	@Test
	public void testLogFailure_jsonObj_opt() {
		opt.logFailure(obj);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "result"
		assertThat(res.size()).isEqualTo(obj.size() + 1);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("result"))).isEqualTo("INFO");

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testLogFailure_jsonObj_withReqs_opt() {
		optReqs.logFailure(obj);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "result" and "requirements"
		assertThat(res.size()).isEqualTo(obj.size() + 2);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("result"))).isEqualTo("WARNING");

		assertThat(res.has("requirements")).isEqualTo(true);
		assertThat(res.get("requirements").isJsonArray()).isEqualTo(true);

		JsonArray reqs = res.get("requirements").getAsJsonArray();
		assertThat(reqs.contains(new JsonPrimitive(req1))).isEqualTo(true);
		assertThat(reqs.contains(new JsonPrimitive(req2))).isEqualTo(true);

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testLogFailure_string_opt() {
		opt.logFailure(msg);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "results"
		assertThat(res.size()).isEqualTo(2);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("INFO");

	}

	@Test
	public void testLogFailure_string_withReqs_opt() {
		optReqs.logFailure(msg);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "results" and "requirements"
		assertThat(res.size()).isEqualTo(3);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("WARNING");

		assertThat(res.containsKey("requirements"));
		assertThat(res.get("requirements")).isInstanceOf(Collection.class);

		@SuppressWarnings("unchecked")
		Collection<String> reqs = (Collection<String>) res.get("requirements");

		assertThat(reqs.size()).isEqualTo(2);
		assertThat(reqs.contains(req1));
		assertThat(reqs.contains(req2));

	}

	@Test
	public void testLogFailure_map_opt() {
		opt.logFailure(map);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "result"
		assertThat(res.size()).isEqualTo(map.size() + 1);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("INFO");

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}

	@Test
	public void testLogFailure_map_withReqs_opt() {
		optReqs.logFailure(map);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "result" and "requirements"
		assertThat(res.size()).isEqualTo(map.size() + 2);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("WARNING");

		assertThat(res.containsKey("requirements"));
		assertThat(res.get("requirements")).isInstanceOf(Collection.class);

		@SuppressWarnings("unchecked")
		Collection<String> reqs = (Collection<String>) res.get("requirements");

		assertThat(reqs.size()).isEqualTo(2);
		assertThat(reqs.contains(req1));
		assertThat(reqs.contains(req2));

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}

	@Test
	public void testLogFailure_stringMap_opt() {
		opt.logFailure(msg, map);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "result"
		assertThat(res.size()).isEqualTo(map.size() + 2);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("INFO");

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}

	@Test
	public void testLogFailure_stringMap_withReqs_opt() {
		optReqs.logFailure(msg, map);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(map.size() + 3);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("WARNING");

		assertThat(res.containsKey("requirements"));
		assertThat(res.get("requirements")).isInstanceOf(Collection.class);

		@SuppressWarnings("unchecked")
		Collection<String> reqs = (Collection<String>) res.get("requirements");

		assertThat(reqs.size()).isEqualTo(2);
		assertThat(reqs.contains(req1));
		assertThat(reqs.contains(req2));

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}

	@Test
	public void testLogFailure_stringJsonObj_opt() {
		opt.logFailure(msg, obj);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "msg" and "result"
		assertThat(res.size()).isEqualTo(obj.size() + 2);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("msg"))).isEqualTo(msg);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("result"))).isEqualTo("INFO");

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testLogFailure_stringJsonObj_withReqs_opt() {
		optReqs.logFailure(msg, obj);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "msg" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(obj.size() + 3);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("msg"))).isEqualTo(msg);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("result"))).isEqualTo("WARNING");

		assertThat(res.has("requirements")).isEqualTo(true);
		assertThat(res.get("requirements").isJsonArray()).isEqualTo(true);

		JsonArray reqs = res.get("requirements").getAsJsonArray();
		assertThat(reqs.contains(new JsonPrimitive(req1))).isEqualTo(true);
		assertThat(reqs.contains(new JsonPrimitive(req2))).isEqualTo(true);

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	/*
	 * Tests for the error() methods. These all call logFailure and throw a ConditionError with certain parameters.
	 *
	 * These test for the non-optional conditions with no requirements. Since all the error functions call
	 * logFailure, the only differences in output in those conditions should be within logFailure, which is tested
	 * separately above.
	 */

	@Test
	public void testError_stringThrowable() {
		ConditionError e = cond.error(msg, cause);
		assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME + ": " + msg);
		assertThat(e.getCause()).isNotNull();
		assertThat(e.getCause()).isEqualTo(cause);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "results" and "error" and "error_class" and "stracktrace"
		assertThat(res.size()).isEqualTo(5);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("FAILURE");
	}

	@Test
	public void testError_string() {
		ConditionError e = cond.error(msg);

		assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME + ": " + msg);
		assertThat(e.getCause()).isNull();

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "results"
		assertThat(res.size()).isEqualTo(2);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("FAILURE");
	}

	@Test
	public void testError_throwable() {
		ConditionError e = cond.error(cause);

		assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME);
		assertThat(e.getCause()).isNotNull();
		assertThat(e.getCause()).isEqualTo(cause);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "results" and "error" and "error_class" and "stracktrace"
		assertThat(res.size()).isEqualTo(5);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(cause.getMessage());

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("FAILURE");
	}

	@Test
	public void testError_stringThrowableMap() {
		ConditionError e = cond.error(msg, cause, map);

		assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME + ": " + msg);
		assertThat(e.getCause()).isNotNull();
		assertThat(e.getCause()).isEqualTo(cause);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "result" and "error" and "error_class" and "stracktrace"
		assertThat(res.size()).isEqualTo(map.size() + 5);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("FAILURE");

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}

	@Test
	public void testError_stringMap() {
		ConditionError e = cond.error(msg, map);
		assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME + ": " + msg);
		assertThat(e.getCause()).isNull();

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "result"
		assertThat(res.size()).isEqualTo(map.size() + 2);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("FAILURE");

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}

	}

	@Test
	public void testError_throwableMap() {
		ConditionError e = cond.error(cause, map);

		assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME);
		assertThat(e.getCause()).isNotNull();
		assertThat(e.getCause()).isEqualTo(cause);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "result" and "error" and "error_class" and "stracktrace"
		assertThat(res.size()).isEqualTo(map.size() + 5);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(cause.getMessage());

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("FAILURE");

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}

	}

	@Test
	public void testError_stringThrowableJsonObject() {
		ConditionError e = cond.error(msg, cause, obj);

		assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME + ": " + msg);
		assertThat(e.getCause()).isNotNull();
		assertThat(e.getCause()).isEqualTo(cause);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "msg" and "result" and "error" and "error_class" and "stracktrace"
		assertThat(res.size()).isEqualTo(obj.size() + 5);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("msg"))).isEqualTo(msg);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("result"))).isEqualTo("FAILURE");

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testError_stringJsonObject() {
		ConditionError e = cond.error(msg, obj);

		assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME + ": " + msg);
		assertThat(e.getCause()).isNull();

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "msg" and "result"
		assertThat(res.size()).isEqualTo(obj.size() + 2);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("msg"))).isEqualTo(msg);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("result"))).isEqualTo("FAILURE");

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testError_throwableJsonObject() {
		ConditionError e = cond.error(cause, obj);

		assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME);
		assertThat(e.getCause()).isNotNull();
		assertThat(e.getCause()).isEqualTo(cause);

		verify(eventLog).log(eq(TEST_CLASS_NAME), objCaptor.capture());

		JsonObject res = objCaptor.getValue();

		// one extra field for "msg" and "result" and "error" and "error_class" and "stracktrace"
		assertThat(res.size()).isEqualTo(obj.size() + 5);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("msg"))).isEqualTo(cause.getMessage());

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(OIDFJSON.getString(res.get("result"))).isEqualTo("FAILURE");

		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	/*
	 * Tests for the upload placeholder
	 */

	@Test
	public void testCreateUploadPlaceholder() {
		cond.createBrowserInteractionPlaceholder();

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "upload" and "result"
		assertThat(res.size()).isEqualTo(2);

		assertThat(res.containsKey("upload")).isEqualTo(true);
		assertThat(res.get("upload")).isInstanceOf(String.class); // it can be any string

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("REVIEW");

	}

	@Test
	public void testCreateUploadPlaceholder_withReqs() {
		condReqs.createBrowserInteractionPlaceholder();

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "upload" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(3);

		assertThat(res.containsKey("upload")).isEqualTo(true);
		assertThat(res.get("upload")).isInstanceOf(String.class); // it can be any string

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("REVIEW");

		@SuppressWarnings("unchecked")
		Collection<String> reqs = (Collection<String>) res.get("requirements");

		assertThat(reqs.size()).isEqualTo(2);
		assertThat(reqs.contains(req1));
		assertThat(reqs.contains(req2));
	}

	@Test
	public void testCreateUploadPlaceholder_string() {
		cond.createBrowserInteractionPlaceholder(msg);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "upload" and "result"
		assertThat(res.size()).isEqualTo(3);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("upload")).isEqualTo(true);
		assertThat(res.get("upload")).isInstanceOf(String.class); // it can be any string

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("REVIEW");

	}

	@Test
	public void testCreateUploadPlaceholder_string_withReqs() {
		condReqs.createBrowserInteractionPlaceholder(msg);

		verify(eventLog).log(eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();

		// one extra field for "msg" and "upload" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(4);

		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("upload")).isEqualTo(true);
		assertThat(res.get("upload")).isInstanceOf(String.class); // it can be any string

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result").toString()).isEqualTo("REVIEW");

		@SuppressWarnings("unchecked")
		Collection<String> reqs = (Collection<String>) res.get("requirements");

		assertThat(reqs.size()).isEqualTo(2);
		assertThat(reqs.contains(req1));
		assertThat(reqs.contains(req2));
	}

	/**
	 * This subclass exposes the utility methods used by Condition classes so that we can test them here.
	 */
	private class AbstractConditionTester extends AbstractCondition {

		/* (non-Javadoc)
		 * @see Condition#evaluate(Environment)
		 */
		@Override
		public Environment evaluate(Environment env) {
			// TODO Auto-generated method stub
			return null;

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#args(java.lang.Object[])
		 */
		@Override
		public Map<String, Object> args(Object... a) {
			// TODO Auto-generated method stub
			return super.args(a);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#log(com.google.gson.JsonObject)
		 */
		@Override
		protected void log(JsonObject obj) {
			// TODO Auto-generated method stub
			super.log(obj);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#log(java.lang.String)
		 */
		@Override
		protected void log(String msg) {
			// TODO Auto-generated method stub
			super.log(msg);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#log(java.util.Map)
		 */
		@Override
		protected void log(Map<String, Object> map) {
			// TODO Auto-generated method stub
			super.log(map);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#log(java.lang.String, com.google.gson.JsonObject)
		 */
		@Override
		protected void log(String msg, JsonObject in) {
			// TODO Auto-generated method stub
			super.log(msg, in);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#log(java.lang.String, java.util.Map)
		 */
		@Override
		protected void log(String msg, Map<String, Object> map) {
			// TODO Auto-generated method stub
			super.log(msg, map);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#logSuccess(com.google.gson.JsonObject)
		 */
		@Override
		protected void logSuccess(JsonObject in) {
			// TODO Auto-generated method stub
			super.logSuccess(in);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#logSuccess(java.lang.String)
		 */
		@Override
		protected void logSuccess(String msg) {
			// TODO Auto-generated method stub
			super.logSuccess(msg);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#logSuccess(java.util.Map)
		 */
		@Override
		protected void logSuccess(Map<String, Object> map) {
			// TODO Auto-generated method stub
			super.logSuccess(map);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#logSuccess(java.lang.String, com.google.gson.JsonObject)
		 */
		@Override
		protected void logSuccess(String msg, JsonObject in) {
			// TODO Auto-generated method stub
			super.logSuccess(msg, in);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#logSuccess(java.lang.String, java.util.Map)
		 */
		@Override
		protected void logSuccess(String msg, Map<String, Object> map) {
			// TODO Auto-generated method stub
			super.logSuccess(msg, map);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#logFailure(com.google.gson.JsonObject)
		 */
		@Override
		protected void logFailure(JsonObject in) {
			// TODO Auto-generated method stub
			super.logFailure(in);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#logFailure(java.lang.String)
		 */
		@Override
		protected void logFailure(String msg) {
			// TODO Auto-generated method stub
			super.logFailure(msg);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#logFailure(java.util.Map)
		 */
		@Override
		protected void logFailure(Map<String, Object> map) {
			// TODO Auto-generated method stub
			super.logFailure(map);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#logFailure(java.lang.String, com.google.gson.JsonObject)
		 */
		@Override
		protected void logFailure(String msg, JsonObject in) {
			// TODO Auto-generated method stub
			super.logFailure(msg, in);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#logFailure(java.lang.String, java.util.Map)
		 */
		@Override
		protected void logFailure(String msg, Map<String, Object> map) {
			// TODO Auto-generated method stub
			super.logFailure(msg, map);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#getRequirements()
		 */
		@Override
		protected Set<String> getRequirements() {
			// TODO Auto-generated method stub
			return super.getRequirements();

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#createUploadPlaceholder(java.lang.String)
		 */
		@Override
		protected String createBrowserInteractionPlaceholder(String msg) {
			// TODO Auto-generated method stub
			return super.createBrowserInteractionPlaceholder(msg);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#createUploadPlaceholder()
		 */
		@Override
		protected String createBrowserInteractionPlaceholder() {
			// TODO Auto-generated method stub
			return super.createBrowserInteractionPlaceholder();

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#createHttpClient(Environment)
		 */
		@Override
		protected HttpClient createHttpClient(Environment env, boolean restrictAllowedTLSVersions, boolean disableRedirectHandling) throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException, KeyManagementException {
			// TODO Auto-generated method stub
			return super.createHttpClient(env, restrictAllowedTLSVersions, disableRedirectHandling);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#createRestTemplate(Environment)
		 */
		@Override
		protected RestTemplate createRestTemplate(Environment env) throws UnrecoverableKeyException, KeyManagementException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, IOException {
			// TODO Auto-generated method stub
			return super.createRestTemplate(env);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#error(java.lang.String, java.lang.Throwable)
		 */
		@Override
		protected ConditionError error(String message, Throwable cause) {
			// TODO Auto-generated method stub
			return super.error(message, cause);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#error(java.lang.String)
		 */
		@Override
		protected ConditionError error(String message) {
			// TODO Auto-generated method stub
			return super.error(message);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#error(java.lang.Throwable)
		 */
		@Override
		protected ConditionError error(Throwable cause) {
			// TODO Auto-generated method stub
			return super.error(cause);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#error(java.lang.String, java.lang.Throwable, java.util.Map)
		 */
		@Override
		protected ConditionError error(String message, Throwable cause, Map<String, Object> map) {
			// TODO Auto-generated method stub
			return super.error(message, cause, map);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#error(java.lang.String, java.util.Map)
		 */
		@Override
		protected ConditionError error(String message, Map<String, Object> map) {
			// TODO Auto-generated method stub
			return super.error(message, map);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#error(java.lang.Throwable, java.util.Map)
		 */
		@Override
		protected ConditionError error(Throwable cause, Map<String, Object> map) {
			// TODO Auto-generated method stub
			return super.error(cause, map);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#error(java.lang.String, java.lang.Throwable, com.google.gson.JsonObject)
		 */
		@Override
		protected ConditionError error(String message, Throwable cause, JsonObject in) {
			// TODO Auto-generated method stub
			return super.error(message, cause, in);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#error(java.lang.String, com.google.gson.JsonObject)
		 */
		@Override
		protected ConditionError error(String message, JsonObject in) {
			// TODO Auto-generated method stub
			return super.error(message, in);

		}

		/* (non-Javadoc)
		 * @see AbstractCondition#error(java.lang.Throwable, com.google.gson.JsonObject)
		 */
		@Override
		protected ConditionError error(Throwable cause, JsonObject in) {
			// TODO Auto-generated method stub
			return super.error(cause, in);

		}

	}

}
