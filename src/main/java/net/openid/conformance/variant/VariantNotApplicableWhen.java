package net.openid.conformance.variant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks variant parameter values as not applicable when another variant
 * parameter has specific value(s).
 *
 * <p>This enables conditional variant dependencies where a variant parameter
 * is only meaningful when another variant has certain values.
 *
 * <p>Example: VCICredentialOfferParameterVariant is not applicable when
 * VCIWalletAuthorizationCodeFlowVariant is "wallet_initiated" (because
 * wallet-initiated flows don't have credential offers):
 *
 * <pre>
 * &#64;VariantNotApplicableWhen(
 *     parameter = VCICredentialOfferParameterVariant.class,
 *     values = {"*"},
 *     whenParameter = VCIWalletAuthorizationCodeFlowVariant.class,
 *     hasValues = {"wallet_initiated"}
 * )
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(VariantNotApplicableWhenContainer.class)
public @interface VariantNotApplicableWhen {

	/**
	 * The variant parameter whose values become not applicable.
	 */
	Class<? extends Enum<?>> parameter();

	/**
	 * The values of the parameter that become not applicable.
	 * Use {"*"} to indicate all values of the parameter.
	 */
	String[] values();

	/**
	 * The condition variant parameter that controls applicability.
	 */
	Class<? extends Enum<?>> whenParameter();

	/**
	 * The condition value(s). When the whenParameter has any of these values,
	 * the specified values of parameter() become not applicable.
	 */
	String[] hasValues();

}
