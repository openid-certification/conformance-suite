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

	/**
	 * Label for this plan in the plan picker once its spec family is selected. The picker
	 * already shows the family and, as a group heading, the entity under test (from
	 * {@link #profile()}), so this names only what tells the plan apart from its siblings,
	 * e.g. "Form Post Basic". A plan with a {@link #specVersion()} starts with it, e.g.
	 * "1.0 Final + HAIP" or "ID2".
	 */
	String shortName();

	/**
	 * Whether the plan is part of the OpenID Foundation certification program. The plan picker
	 * shows a "Certification" badge on certifiable plans and hides the others until the user
	 * ticks "Show non-certifiable plans".
	 */
	boolean certifiable();

	/**
	 * Whether the plan is an alpha (possibly incomplete or incorrect) version. The plan picker
	 * shows an "Alpha" badge on it.
	 */
	boolean alpha() default false;

	String profile();

	String specFamily();

	String specVersion() default "";

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
