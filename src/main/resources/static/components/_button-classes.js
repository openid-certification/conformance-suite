/**
 * Shared button class helpers used by cts-button, cts-link-button, and cts-modal.
 *
 * Not a custom element — underscore prefix signals "internal helper module".
 */

/** @type {Object.<string, string>} Maps variant name → Bootstrap modifier class */
export const VARIANT_CLASSES = {
  light: "btn-light",
  info: "btn-info",
  primary: "btn-primary",
  danger: "btn-danger",
  secondary: "btn-secondary",
  success: "btn-success",
  warning: "btn-warning",
  dark: "btn-dark",
  // Outline variants — Bootstrap 5 supports these as a parallel family. Some
  // callers (cts-modal footer-buttons, legacy templates) pass `btn-outline-*`
  // in a descriptor or attribute; without explicit entries, the variant lookup
  // silently fell back to "light" and erased the outline intent.
  "outline-light": "btn-outline-light",
  "outline-info": "btn-outline-info",
  "outline-primary": "btn-outline-primary",
  "outline-danger": "btn-outline-danger",
  "outline-secondary": "btn-outline-secondary",
  "outline-success": "btn-outline-success",
  "outline-warning": "btn-outline-warning",
  "outline-dark": "btn-outline-dark",
};

/** @type {Object.<string, string>} Maps size name → Bootstrap modifier class (empty string = no class for md) */
export const SIZE_CLASSES = {
  sm: "btn-sm",
  md: "",
  lg: "btn-lg",
};

/**
 * Build the full Bootstrap button class string shared across all CTS button components.
 *
 * @param {Object} options
 * @param {string} [options.variant="light"] - Variant key (e.g. "primary", "danger"). Unknown values fall back to "light".
 * @param {string} [options.size="sm"] - Size key (e.g. "sm", "md", "lg"). Unknown values fall back to "sm".
 * @param {boolean} [options.fullWidth=false] - Appends `w-100` when true.
 * @returns {string} Full class string, e.g. `"btn btn-sm btn-primary bg-gradient border border-secondary"`
 */
export function buildButtonClasses({ variant = "light", size = "sm", fullWidth = false } = {}) {
  const variantClass = VARIANT_CLASSES[variant] ?? "btn-light";
  const sizeClass = SIZE_CLASSES[size] ?? "btn-sm";
  const sizeSegment = sizeClass ? `${sizeClass} ` : "";
  const widthSegment = fullWidth ? " w-100" : "";
  return `btn ${sizeSegment}${variantClass} bg-gradient border border-secondary${widthSegment}`;
}
