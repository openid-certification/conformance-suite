package net.openid.conformance.info;

/**
 * Marker wrapper for HTTP responses whose payload contains a nested {@code config} JSON
 * object that should have {@link ConfigMigration#migrateLegacyClientAttestationKeys}
 * applied during JSON serialization. The Gson serializer registered in
 * {@link net.openid.conformance.CollapsingGsonHttpMessageConverter#getDbObjectCollapsingGson}
 * serializes {@link #inner} via the normal pipeline, then runs the migration on the
 * resulting tree before returning — so callers don't need to parse, mutate, and
 * re-serialize when the only post-processing step is the legacy-key migration.
 *
 * <p>Use this wrapper when migration is the <em>only</em> mutation; for endpoints that
 * mutate other fields too (e.g. {@code TestPlanApi#getTestPlan} injecting per-module
 * summaries), the explicit round-trip in the controller remains the right shape.
 */
public record ConfigMigratingResponse(Object inner) {
}
