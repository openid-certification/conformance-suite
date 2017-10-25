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
import static org.mockito.Matchers.eq;

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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

	/**
	 * 
	 */
	private static final String TEST_ID = "UNIT-TEST";

	private AbstractConditionTester cond;
	
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

	private String msg;
	
	@Before
	public void setUp() throws Exception {
		
		cond = new AbstractConditionTester(TEST_ID, eventLog, false);
		
		obj = new JsonParser().parse("{\"foo\": \"bar\", \"baz\": 1234}").getAsJsonObject();
		
		map = ImmutableMap.of("foo", "bar", "baz", 1234);
		
		msg = "message string";
		
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
		
		verify(eventLog).log(eq(TEST_ID), eq(cond.getMessage()), objCaptor.capture());
		
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
		
		verify(eventLog).log(eq(TEST_ID), eq(cond.getMessage()), eq(msg));
		
	}

	@Test
	public void testLog_map() {
		
		cond.log(map);
		
		verify(eventLog).log(eq(TEST_ID), eq(cond.getMessage()), mapCaptor.capture());
		
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
		
		verify(eventLog).log(eq(TEST_ID), eq(cond.getMessage()), objCaptor.capture());
		
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
		
		verify(eventLog).log(eq(TEST_ID), eq(cond.getMessage()), mapCaptor.capture());
		
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
