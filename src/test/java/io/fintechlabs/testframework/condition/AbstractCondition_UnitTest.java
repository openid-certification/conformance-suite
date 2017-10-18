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

import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

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

	private AbstractConditionTester cond;
	
	@Spy
	private Environment env = new Environment();
	
	@Mock
	private EventLog eventLog;
	
	@Before
	public void setUp() throws Exception {
		
		cond = new AbstractConditionTester("UNIT-TEST", eventLog, false);
		
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
	
	
	/**
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

	}

	
	
}
