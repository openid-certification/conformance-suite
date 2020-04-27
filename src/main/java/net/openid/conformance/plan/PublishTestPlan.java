package net.openid.conformance.plan;

import net.openid.conformance.testmodule.TestModule;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

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
	 *
	 * As an alternative, the test module may implement the static method 'testModulesWithVariants()', that way allows
	 * variants to be overridden for each test module if desired.
	 */
	Class<? extends TestModule>[] testModules() default {};

	String summary() default "";

}
