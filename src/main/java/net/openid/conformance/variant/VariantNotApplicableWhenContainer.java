package net.openid.conformance.variant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for repeated {@link VariantNotApplicableWhen} annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VariantNotApplicableWhenContainer {

	VariantNotApplicableWhen[] value();

}
