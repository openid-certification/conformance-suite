/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * This tests the behavior of the various utility methods in the 
 * abstract superclass used by most conditions. 
 * 
 * @author jricher
 *
 */
@RunWith(MockitoJUnitRunner.class)
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
	
	
	@Spy
	private Environment env = new Environment();
	
	@Mock
	private EventLog eventLog;
	
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
	
	@Before
	public void setUp() throws Exception {
		
		cond = new AbstractConditionTester(TEST_ID, eventLog, false);
		
		condReqs = new AbstractConditionTester(TEST_ID, eventLog, false, req1, req2);
		
		opt = new AbstractConditionTester(TEST_ID, eventLog, true);
		
		optReqs = new AbstractConditionTester(TEST_ID, eventLog, true, req1, req2);

		obj = new JsonParser().parse("{\"foo\": \"bar\", \"baz\": 1234}").getAsJsonObject();
		
		map = ImmutableMap.of("foo", "bar", "baz", 1234);
		
		
	}
	
	@Test
	public void testArgs_evenList() {
		Map<String, Object> args = cond.args("foo", "bar", "baz", "quz");
		
		assertThat(args.size()).isEqualTo(2);
		assertThat(args.get("foo")).isEqualTo("bar");
		assertThat(args.get("baz")).isEqualTo("quz");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testArgs_oddList() {
		cond.args("foo", "bar", "baz", "quz", "batman");		
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testArgs_null() {
		cond.args(null);		
	}

	@Test
	public void testArgs_emptyList() {
		Map<String, Object> args = cond.args();
		
		assertThat(args.size()).isEqualTo(0);
	}
	
	@Test
	public void testLog_jsonObj() {
		
		cond.log(obj);
		
		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
		
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
		
		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), eq(msg));
		
	}

	@Test
	public void testLog_map() {
		
		cond.log(map);
		
		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
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
		
		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
		
		JsonObject res = objCaptor.getValue();
		
		// one extra field for "msg"
		assertThat(res.size()).isEqualTo(obj.size() + 1);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(res.get("msg").getAsString()).isEqualTo(msg);
		
		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
		
	}
	
	@Test
	public void testLog_stringMap() {
		
		cond.log(msg, map);
		
		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
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

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
		
		JsonObject res = objCaptor.getValue();
		
		// one extra field for "result"
		assertThat(res.size()).isEqualTo(obj.size() + 1);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(res.get("result").getAsString()).isEqualTo("SUCCESS");
		
		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testLogSuccess_jsonObj_withReqs() {
		condReqs.logSuccess(obj);

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
		
		JsonObject res = objCaptor.getValue();
		
		// one extra field for "result" and "requirements"
		assertThat(res.size()).isEqualTo(obj.size() + 2);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(res.get("result").getAsString()).isEqualTo("SUCCESS");
		
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
		
		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "msg" and "results"
		assertThat(res.size()).isEqualTo(2);
		
		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("SUCCESS");

	}

	@Test
	public void testLogSuccess_string_withReqs() {
		condReqs.logSuccess(msg);
		
		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "msg" and "results" and "requirements"
		assertThat(res.size()).isEqualTo(3);
		
		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("SUCCESS");
		
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

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "result"
		assertThat(res.size()).isEqualTo(map.size() + 1);
		
		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("SUCCESS");

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}
	
	@Test
	public void testLogSuccess_map_withReqs() {
		condReqs.logSuccess(map);

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "result" and "requirements"
		assertThat(res.size()).isEqualTo(map.size() + 2);
		
		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("SUCCESS");

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

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "msg" and "result"
		assertThat(res.size()).isEqualTo(map.size() + 2);
		
		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("SUCCESS");

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}
	
	@Test
	public void testLogSuccess_stringMap_withReqs() {
		condReqs.logSuccess(msg, map);

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "msg" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(map.size() + 3);
		
		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("SUCCESS");

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

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
		
		JsonObject res = objCaptor.getValue();
		
		// one extra field for "msg" and "result"
		assertThat(res.size()).isEqualTo(obj.size() + 2);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(res.get("msg").getAsString()).isEqualTo(msg);
		
		assertThat(res.has("result")).isEqualTo(true);
		assertThat(res.get("result").getAsString()).isEqualTo("SUCCESS");
		
		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testLogSuccess_stringJsonObj_withReqs() {
		condReqs.logSuccess(msg, obj);

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
		
		JsonObject res = objCaptor.getValue();
		
		// one extra field for "msg" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(obj.size() + 3);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(res.get("msg").getAsString()).isEqualTo(msg);
		
		assertThat(res.has("result")).isEqualTo(true);
		assertThat(res.get("result").getAsString()).isEqualTo("SUCCESS");
		
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

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
		
		JsonObject res = objCaptor.getValue();
		
		// one extra field for "result"
		assertThat(res.size()).isEqualTo(obj.size() + 1);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(res.get("result").getAsString()).isEqualTo("FAILURE");
		
		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testLogFailure_jsonObj_withReqs() {
		condReqs.logFailure(obj);

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
		
		JsonObject res = objCaptor.getValue();
		
		// one extra field for "result" and "requirements"
		assertThat(res.size()).isEqualTo(obj.size() + 2);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(res.get("result").getAsString()).isEqualTo("FAILURE");
		
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
		
		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "msg" and "results"
		assertThat(res.size()).isEqualTo(2);
		
		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("FAILURE");

	}

	@Test
	public void testLogFailure_string_withReqs() {
		condReqs.logFailure(msg);
		
		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "msg" and "results" and "requirements"
		assertThat(res.size()).isEqualTo(3);
		
		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("FAILURE");
		
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

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "result"
		assertThat(res.size()).isEqualTo(map.size() + 1);
		
		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("FAILURE");

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}
	
	@Test
	public void testLogFailure_map_withReqs() {
		condReqs.logFailure(map);

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "result" and "requirements"
		assertThat(res.size()).isEqualTo(map.size() + 2);
		
		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("FAILURE");

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

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "msg" and "result"
		assertThat(res.size()).isEqualTo(map.size() + 2);
		
		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("FAILURE");

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}
	
	@Test
	public void testLogFailure_stringMap_withReqs() {
		condReqs.logFailure(msg, map);

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "msg" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(map.size() + 3);
		
		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("FAILURE");

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

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
		
		JsonObject res = objCaptor.getValue();
		
		// one extra field for "msg" and "result"
		assertThat(res.size()).isEqualTo(obj.size() + 2);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(res.get("msg").getAsString()).isEqualTo(msg);
		
		assertThat(res.has("result")).isEqualTo(true);
		assertThat(res.get("result").getAsString()).isEqualTo("FAILURE");
		
		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testLogFailure_stringJsonObj_withReqs() {
		condReqs.logFailure(msg, obj);

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
		
		JsonObject res = objCaptor.getValue();
		
		// one extra field for "msg" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(obj.size() + 3);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(res.get("msg").getAsString()).isEqualTo(msg);
		
		assertThat(res.has("result")).isEqualTo(true);
		assertThat(res.get("result").getAsString()).isEqualTo("FAILURE");
		
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

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
		
		JsonObject res = objCaptor.getValue();
		
		// one extra field for "result"
		assertThat(res.size()).isEqualTo(obj.size() + 1);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(res.get("result").getAsString()).isEqualTo("WARNING");
		
		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testLogFailure_jsonObj_withReqs_opt() {
		optReqs.logFailure(obj);

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
		
		JsonObject res = objCaptor.getValue();
		
		// one extra field for "result" and "requirements"
		assertThat(res.size()).isEqualTo(obj.size() + 2);

		assertThat(res.has("result")).isEqualTo(true);
		assertThat(res.get("result").getAsString()).isEqualTo("WARNING");
		
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
		
		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "msg" and "results"
		assertThat(res.size()).isEqualTo(2);
		
		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("WARNING");

	}

	@Test
	public void testLogFailure_string_withReqs_opt() {
		optReqs.logFailure(msg);
		
		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "msg" and "results" and "requirements"
		assertThat(res.size()).isEqualTo(3);
		
		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("WARNING");
		
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

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "result"
		assertThat(res.size()).isEqualTo(map.size() + 1);
		
		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("WARNING");

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}
	
	@Test
	public void testLogFailure_map_withReqs_opt() {
		optReqs.logFailure(map);

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "result" and "requirements"
		assertThat(res.size()).isEqualTo(map.size() + 2);
		
		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("WARNING");

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

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "msg" and "result"
		assertThat(res.size()).isEqualTo(map.size() + 2);
		
		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("WARNING");

		// make sure 'res' has everything 'map' does
		for (String key : map.keySet()) {
			assertThat(res.containsKey(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(map.get(key));
		}
	}
	
	@Test
	public void testLogFailure_stringMap_withReqs_opt() {
		optReqs.logFailure(msg, map);

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());

		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "msg" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(map.size() + 3);
		
		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("WARNING");

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

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
		
		JsonObject res = objCaptor.getValue();
		
		// one extra field for "msg" and "result"
		assertThat(res.size()).isEqualTo(obj.size() + 2);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(res.get("msg").getAsString()).isEqualTo(msg);
		
		assertThat(res.has("result")).isEqualTo(true);
		assertThat(res.get("result").getAsString()).isEqualTo("WARNING");
		
		// make sure 'res' has everything 'obj' does
		for (String key : obj.keySet()) {
			assertThat(res.has(key)).isEqualTo(true);
			assertThat(res.get(key)).isEqualTo(obj.get(key));
		}
	}

	@Test
	public void testLogFailure_stringJsonObj_withReqs_opt() {
		optReqs.logFailure(msg, obj);

		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
		
		JsonObject res = objCaptor.getValue();
		
		// one extra field for "msg" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(obj.size() + 3);

		assertThat(res.has("msg")).isEqualTo(true);
		assertThat(res.get("msg").getAsString()).isEqualTo(msg);
		
		assertThat(res.has("result")).isEqualTo(true);
		assertThat(res.get("result").getAsString()).isEqualTo("WARNING");
		
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
		try {
			cond.error(msg, cause);
			
			failBecauseExceptionWasNotThrown(ConditionError.class);
			
		} catch (ConditionError e) {
			assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME + ": " + msg);
			assertThat(e.getCause()).isNotNull();
			assertThat(e.getCause()).isEqualTo(cause);

			verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
			
			Map<String, Object> res = mapCaptor.getValue();
			
			// one extra field for "msg" and "results"
			assertThat(res.size()).isEqualTo(2);
			
			assertThat(res.containsKey("msg")).isEqualTo(true);
			assertThat(res.get("msg")).isEqualTo(msg);

			assertThat(res.containsKey("result")).isEqualTo(true);
			assertThat(res.get("result")).isEqualTo("FAILURE");
		}
	}
	
	@Test
	public void testError_string() {
		try {
			cond.error(msg);
			
			failBecauseExceptionWasNotThrown(ConditionError.class);
			
		} catch (ConditionError e) {
			assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME + ": " + msg);
			assertThat(e.getCause()).isNull();

			verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
			
			Map<String, Object> res = mapCaptor.getValue();
			
			// one extra field for "msg" and "results"
			assertThat(res.size()).isEqualTo(2);
			
			assertThat(res.containsKey("msg")).isEqualTo(true);
			assertThat(res.get("msg")).isEqualTo(msg);

			assertThat(res.containsKey("result")).isEqualTo(true);
			assertThat(res.get("result")).isEqualTo("FAILURE");
		}
	}
	
	@Test
	public void testError_throwable() {
		try {
			cond.error(cause);
			
			failBecauseExceptionWasNotThrown(ConditionError.class);
			
		} catch (ConditionError e) {
			assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME);
			assertThat(e.getCause()).isNotNull();
			assertThat(e.getCause()).isEqualTo(cause);
			
			verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
			
			Map<String, Object> res = mapCaptor.getValue();
			
			// one extra field for "msg" and "results"
			assertThat(res.size()).isEqualTo(2);
			
			assertThat(res.containsKey("msg")).isEqualTo(true);
			assertThat(res.get("msg")).isEqualTo(cause.getMessage());

			assertThat(res.containsKey("result")).isEqualTo(true);
			assertThat(res.get("result")).isEqualTo("FAILURE");
		}
	}
	
	@Test
	public void testError_stringThrowableMap() {
		try {
			cond.error(msg, cause, map);
			
			failBecauseExceptionWasNotThrown(ConditionError.class);
			
		} catch (ConditionError e) {
			assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME + ": " + msg);
			assertThat(e.getCause()).isNotNull();
			assertThat(e.getCause()).isEqualTo(cause);
			
			verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
			
			Map<String, Object> res = mapCaptor.getValue();
			
			// one extra field for "msg" and "result"
			assertThat(res.size()).isEqualTo(map.size() + 2);
			
			assertThat(res.containsKey("msg")).isEqualTo(true);
			assertThat(res.get("msg")).isEqualTo(msg);

			assertThat(res.containsKey("result")).isEqualTo(true);
			assertThat(res.get("result")).isEqualTo("FAILURE");

			// make sure 'res' has everything 'map' does
			for (String key : map.keySet()) {
				assertThat(res.containsKey(key)).isEqualTo(true);
				assertThat(res.get(key)).isEqualTo(map.get(key));
			}
		}
	}
	
	@Test
	public void testError_stringMap() {
		try {
			cond.error(msg, map);
			
			failBecauseExceptionWasNotThrown(ConditionError.class);
			
		} catch (ConditionError e) {
			assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME + ": " + msg);
			assertThat(e.getCause()).isNull();
			
			verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
			
			Map<String, Object> res = mapCaptor.getValue();
			
			// one extra field for "msg" and "result"
			assertThat(res.size()).isEqualTo(map.size() + 2);
			
			assertThat(res.containsKey("msg")).isEqualTo(true);
			assertThat(res.get("msg")).isEqualTo(msg);

			assertThat(res.containsKey("result")).isEqualTo(true);
			assertThat(res.get("result")).isEqualTo("FAILURE");

			// make sure 'res' has everything 'map' does
			for (String key : map.keySet()) {
				assertThat(res.containsKey(key)).isEqualTo(true);
				assertThat(res.get(key)).isEqualTo(map.get(key));
			}

		}
	}
	
	@Test
	public void testError_throwableMap() {
		try {
			cond.error(cause, map);
			
			failBecauseExceptionWasNotThrown(ConditionError.class);
			
		} catch (ConditionError e) {
			assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME);
			assertThat(e.getCause()).isNotNull();
			assertThat(e.getCause()).isEqualTo(cause);
			
			verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
			
			Map<String, Object> res = mapCaptor.getValue();
			
			// one extra field for "msg" and "result"
			assertThat(res.size()).isEqualTo(map.size() + 2);
			
			assertThat(res.containsKey("msg")).isEqualTo(true);
			assertThat(res.get("msg")).isEqualTo(cause.getMessage());

			assertThat(res.containsKey("result")).isEqualTo(true);
			assertThat(res.get("result")).isEqualTo("FAILURE");

			// make sure 'res' has everything 'map' does
			for (String key : map.keySet()) {
				assertThat(res.containsKey(key)).isEqualTo(true);
				assertThat(res.get(key)).isEqualTo(map.get(key));
			}

		}
	}
	
	@Test
	public void testError_stringThrowableJsonObject() {
		try {
			cond.error(msg, cause, obj);
			
			failBecauseExceptionWasNotThrown(ConditionError.class);
			
		} catch (ConditionError e) {
			assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME + ": " + msg);
			assertThat(e.getCause()).isNotNull();
			assertThat(e.getCause()).isEqualTo(cause);

			verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
			
			JsonObject res = objCaptor.getValue();
			
			// one extra field for "msg" and "result"
			assertThat(res.size()).isEqualTo(obj.size() + 2);

			assertThat(res.has("msg")).isEqualTo(true);
			assertThat(res.get("msg").getAsString()).isEqualTo(msg);
			
			assertThat(res.has("result")).isEqualTo(true);
			assertThat(res.get("result").getAsString()).isEqualTo("FAILURE");
			
			// make sure 'res' has everything 'obj' does
			for (String key : obj.keySet()) {
				assertThat(res.has(key)).isEqualTo(true);
				assertThat(res.get(key)).isEqualTo(obj.get(key));
			}
		}
	}
	
	@Test
	public void testError_stringJsonObject() {
		try {
			cond.error(msg, obj);
			
			failBecauseExceptionWasNotThrown(ConditionError.class);
			
		} catch (ConditionError e) {
			assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME + ": " + msg);
			assertThat(e.getCause()).isNull();

			verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
			
			JsonObject res = objCaptor.getValue();
			
			// one extra field for "msg" and "result"
			assertThat(res.size()).isEqualTo(obj.size() + 2);

			assertThat(res.has("msg")).isEqualTo(true);
			assertThat(res.get("msg").getAsString()).isEqualTo(msg);
			
			assertThat(res.has("result")).isEqualTo(true);
			assertThat(res.get("result").getAsString()).isEqualTo("FAILURE");
			
			// make sure 'res' has everything 'obj' does
			for (String key : obj.keySet()) {
				assertThat(res.has(key)).isEqualTo(true);
				assertThat(res.get(key)).isEqualTo(obj.get(key));
			}
		}
	}
	
	@Test
	public void testError_throwableJsonObject() {
		try {
			cond.error(cause, obj);
			
			failBecauseExceptionWasNotThrown(ConditionError.class);
			
		} catch (ConditionError e) {
			assertThat(e.getMessage()).isEqualTo(TEST_CLASS_NAME);
			assertThat(e.getCause()).isNotNull();
			assertThat(e.getCause()).isEqualTo(cause);

			verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), objCaptor.capture());
			
			JsonObject res = objCaptor.getValue();
			
			// one extra field for "msg" and "result"
			assertThat(res.size()).isEqualTo(obj.size() + 2);

			assertThat(res.has("msg")).isEqualTo(true);
			assertThat(res.get("msg").getAsString()).isEqualTo(cause.getMessage());
			
			assertThat(res.has("result")).isEqualTo(true);
			assertThat(res.get("result").getAsString()).isEqualTo("FAILURE");
			
			// make sure 'res' has everything 'obj' does
			for (String key : obj.keySet()) {
				assertThat(res.has(key)).isEqualTo(true);
				assertThat(res.get(key)).isEqualTo(obj.get(key));
			}
		}
	}

	/*
	 * Tests for the upload placeholder
	 */
	
	@Test
	public void testCreateUploadPlaceholder() {
		cond.createUploadPlaceholder();
		
		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "upload" and "result"
		assertThat(res.size()).isEqualTo(2);
		
		assertThat(res.containsKey("upload")).isEqualTo(true);
		assertThat(res.get("upload")).isInstanceOf(String.class); // it can be any string

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("REVIEW");
		
	}
	
	@Test
	public void testCreateUploadPlaceholder_withReqs() {
		condReqs.createUploadPlaceholder();
		
		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "upload" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(3);
		
		assertThat(res.containsKey("upload")).isEqualTo(true);
		assertThat(res.get("upload")).isInstanceOf(String.class); // it can be any string

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("REVIEW");
		
		@SuppressWarnings("unchecked")
		Collection<String> reqs = (Collection<String>) res.get("requirements");
		
		assertThat(reqs.size()).isEqualTo(2);
		assertThat(reqs.contains(req1));
		assertThat(reqs.contains(req2));
	}

	@Test
	public void testCreateUploadPlaceholder_string() {
		cond.createUploadPlaceholder(msg);
		
		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "msg" and "upload" and "result"
		assertThat(res.size()).isEqualTo(3);
		
		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);
		
		assertThat(res.containsKey("upload")).isEqualTo(true);
		assertThat(res.get("upload")).isInstanceOf(String.class); // it can be any string

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("REVIEW");
		
	}
	
	@Test
	public void testCreateUploadPlaceholder_string_withReqs() {
		condReqs.createUploadPlaceholder(msg);
		
		verify(eventLog).log(eq(TEST_ID), eq(TEST_CLASS_NAME), mapCaptor.capture());
		
		Map<String, Object> res = mapCaptor.getValue();
		
		// one extra field for "msg" and "upload" and "result" and "requirements"
		assertThat(res.size()).isEqualTo(4);
		
		assertThat(res.containsKey("msg")).isEqualTo(true);
		assertThat(res.get("msg")).isEqualTo(msg);
		
		assertThat(res.containsKey("upload")).isEqualTo(true);
		assertThat(res.get("upload")).isInstanceOf(String.class); // it can be any string

		assertThat(res.containsKey("result")).isEqualTo(true);
		assertThat(res.get("result")).isEqualTo("REVIEW");
		
		@SuppressWarnings("unchecked")
		Collection<String> reqs = (Collection<String>) res.get("requirements");
		
		assertThat(reqs.size()).isEqualTo(2);
		assertThat(reqs.contains(req1));
		assertThat(reqs.contains(req2));
	}
	

	/**
	 * This subclass exposes the utility methods used by Condition classes so that we can test them here. 
	 * 
	 * @author jricher
	 *
	 */
	private class AbstractConditionTester extends AbstractCondition {

		/**
		 * @param testId
		 * @param log
		 * @param optional
		 */
		public AbstractConditionTester(String testId, EventLog log, boolean optional) {
			super(testId, log, optional);
			// TODO Auto-generated constructor stub
		}

		/**
		 * @param testId
		 * @param log
		 * @param optional
		 * @param requirements
		 */
		public AbstractConditionTester(String testId, EventLog log, boolean optional, String... requirements) {
			super(testId, log, optional, requirements);
			// TODO Auto-generated constructor stub
		}

		/**
		 * @param testId
		 * @param log
		 * @param optional
		 * @param requirements
		 */
		public AbstractConditionTester(String testId, EventLog log, boolean optional, Set<String> requirements) {
			super(testId, log, optional, requirements);
			// TODO Auto-generated constructor stub
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
		 */
		@Override
		public Environment evaluate(Environment env) {
			// TODO Auto-generated method stub
			return null;

		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#args(java.lang.Object[])
		 */
		@Override
		public Map<String, Object> args(Object... a) {
			// TODO Auto-generated method stub
			return super.args(a);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#log(com.google.gson.JsonObject)
		 */
		@Override
		protected void log(JsonObject obj) {
			// TODO Auto-generated method stub
			super.log(obj);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#log(java.lang.String)
		 */
		@Override
		protected void log(String msg) {
			// TODO Auto-generated method stub
			super.log(msg);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#log(java.util.Map)
		 */
		@Override
		protected void log(Map<String, Object> map) {
			// TODO Auto-generated method stub
			super.log(map);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#log(java.lang.String, com.google.gson.JsonObject)
		 */
		@Override
		protected void log(String msg, JsonObject in) {
			// TODO Auto-generated method stub
			super.log(msg, in);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#log(java.lang.String, java.util.Map)
		 */
		@Override
		protected void log(String msg, Map<String, Object> map) {
			// TODO Auto-generated method stub
			super.log(msg, map);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#logSuccess(com.google.gson.JsonObject)
		 */
		@Override
		protected void logSuccess(JsonObject in) {
			// TODO Auto-generated method stub
			super.logSuccess(in);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#logSuccess(java.lang.String)
		 */
		@Override
		protected void logSuccess(String msg) {
			// TODO Auto-generated method stub
			super.logSuccess(msg);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#logSuccess(java.util.Map)
		 */
		@Override
		protected void logSuccess(Map<String, Object> map) {
			// TODO Auto-generated method stub
			super.logSuccess(map);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#logSuccess(java.lang.String, com.google.gson.JsonObject)
		 */
		@Override
		protected void logSuccess(String msg, JsonObject in) {
			// TODO Auto-generated method stub
			super.logSuccess(msg, in);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#logSuccess(java.lang.String, java.util.Map)
		 */
		@Override
		protected void logSuccess(String msg, Map<String, Object> map) {
			// TODO Auto-generated method stub
			super.logSuccess(msg, map);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#logFailure(com.google.gson.JsonObject)
		 */
		@Override
		protected void logFailure(JsonObject in) {
			// TODO Auto-generated method stub
			super.logFailure(in);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#logFailure(java.lang.String)
		 */
		@Override
		protected void logFailure(String msg) {
			// TODO Auto-generated method stub
			super.logFailure(msg);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#logFailure(java.util.Map)
		 */
		@Override
		protected void logFailure(Map<String, Object> map) {
			// TODO Auto-generated method stub
			super.logFailure(map);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#logFailure(java.lang.String, com.google.gson.JsonObject)
		 */
		@Override
		protected void logFailure(String msg, JsonObject in) {
			// TODO Auto-generated method stub
			super.logFailure(msg, in);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#logFailure(java.lang.String, java.util.Map)
		 */
		@Override
		protected void logFailure(String msg, Map<String, Object> map) {
			// TODO Auto-generated method stub
			super.logFailure(msg, map);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#error(java.lang.String, java.lang.Throwable)
		 */
		@Override
		protected Environment error(String message, Throwable cause) {
			// TODO Auto-generated method stub
			return super.error(message, cause);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#error(java.lang.String)
		 */
		@Override
		protected Environment error(String message) {
			// TODO Auto-generated method stub
			return super.error(message);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#error(java.lang.Throwable)
		 */
		@Override
		protected Environment error(Throwable cause) {
			// TODO Auto-generated method stub
			return super.error(cause);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#error(java.lang.String, java.lang.Throwable, java.util.Map)
		 */
		@Override
		protected Environment error(String message, Throwable cause, Map<String, Object> map) {
			// TODO Auto-generated method stub
			return super.error(message, cause, map);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#error(java.lang.String, java.util.Map)
		 */
		@Override
		protected Environment error(String message, Map<String, Object> map) {
			// TODO Auto-generated method stub
			return super.error(message, map);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#error(java.lang.Throwable, java.util.Map)
		 */
		@Override
		protected Environment error(Throwable cause, Map<String, Object> map) {
			// TODO Auto-generated method stub
			return super.error(cause, map);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#error(java.lang.String, java.lang.Throwable, com.google.gson.JsonObject)
		 */
		@Override
		protected Environment error(String message, Throwable cause, JsonObject in) {
			// TODO Auto-generated method stub
			return super.error(message, cause, in);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#error(java.lang.String, com.google.gson.JsonObject)
		 */
		@Override
		protected Environment error(String message, JsonObject in) {
			// TODO Auto-generated method stub
			return super.error(message, in);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#error(java.lang.Throwable, com.google.gson.JsonObject)
		 */
		@Override
		protected Environment error(Throwable cause, JsonObject in) {
			// TODO Auto-generated method stub
			return super.error(cause, in);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#getRequirements()
		 */
		@Override
		protected Set<String> getRequirements() {
			// TODO Auto-generated method stub
			return super.getRequirements();
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#createUploadPlaceholder(java.lang.String)
		 */
		@Override
		protected void createUploadPlaceholder(String msg) {
			// TODO Auto-generated method stub
			super.createUploadPlaceholder(msg);
			
		}

		/* (non-Javadoc)
		 * @see io.fintechlabs.testframework.condition.AbstractCondition#createUploadPlaceholder()
		 */
		@Override
		protected void createUploadPlaceholder() {
			// TODO Auto-generated method stub
			super.createUploadPlaceholder();
			
		}
		
		

	}

	
	
}
