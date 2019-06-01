package io.fintechlabs.testframework.testmodule;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.fintechlabs.testframework.sequence.ConditionSequence;

/**
 * Provide a replacement for an existing sequence
 *
 * An accessory annotation is used inside a @Variant and causes a runAccessory() call within that test module to
 * run an alternative sequence.
 *
 * @see Variant
 *
 * @see io.fintechlabs.testframework.sequence.AbstractConditionSequence#runAccessory
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Accessory {

	String key();

	Class<? extends ConditionSequence>[] sequences() default {};

}
