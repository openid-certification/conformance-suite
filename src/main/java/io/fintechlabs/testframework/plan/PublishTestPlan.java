package io.fintechlabs.testframework.plan;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

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

	String summary() default "";

}
