package net.openid.conformance.plan;

import net.openid.conformance.testmodule.TestModule;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(TYPE)
@Retention(RUNTIME)
public @interface PublishTestPlan {

	String testPlanName();

	String displayName();

	String profile();

	String[] configurationFields() default {};

	/**
	 * Get the ordered list of test modules that are part
	 * of this plan.
	 */
	Class<? extends TestModule>[] testModules() default {};

	String summary() default "";

}
