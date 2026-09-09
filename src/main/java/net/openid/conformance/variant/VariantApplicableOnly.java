package net.openid.conformance.variant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts a test module to the listed values of a variant parameter.
 *
 * <p>The default is inclusion: a new value of a variant parameter (for example a new ecosystem
 * added to a {@code fapi_profile} enum) gets every test module unless a module explicitly opts
 * out with {@link VariantNotApplicable}. That is the safe default, because a missed exclusion
 * shows up as a visible test failure whereas a missed inclusion silently drops coverage.
 * {@link VariantNotApplicable} is therefore the normal annotation.
 *
 * <p>Use this annotation only for a module that exists for one (or two) specific values: it
 * exercises a rule or feature that only those values define (a Brazil payment consent signature,
 * a CDR sharing_duration, a ConnectID purpose, a KSA exp limit, ...). Such a module can never
 * apply to a value that lacks the feature, so listing the values it is for is the accurate
 * statement and new values are correctly excluded. Do not use it for a generic module that
 * several values happen to have opted out of (registered redirect URIs, grant management,
 * refresh-token mandatoriness): those stay {@link VariantNotApplicable} so a new value inherits
 * them until someone decides otherwise.
 *
 * <p>Annotations compose across the class hierarchy: each {@link VariantNotApplicable} removes
 * its values from the module's allowed set and each {@link VariantApplicableOnly} intersects
 * the allowed set with its values. {@code values} must not be empty.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(VariantApplicableOnlyContainer.class)
public @interface VariantApplicableOnly {

	Class<? extends Enum<?>> parameter();
	String[] values();

}
