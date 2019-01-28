package io.fintechlabs.testframework.testmodule;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.fintechlabs.testframework.sequence.ConditionSequence;

/**
 * @author jricher
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Accessory {

	String key();

	Class<? extends ConditionSequence>[] sequences() default {};

}
