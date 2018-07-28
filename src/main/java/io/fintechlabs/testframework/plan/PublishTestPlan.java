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

package io.fintechlabs.testframework.plan;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @author jricher
 *
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface PublishTestPlan {

	String testPlanName();

	String displayName();

	String profile() default "SAMPLE";

	String[] configurationFields() default {};

	/**
	 * Get the ordered list of test modules that are part
	 * of this plan.
	 */
	String[] testModuleNames() default {};

}
