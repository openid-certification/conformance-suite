/**
 * Documentation-only response models for the springdoc/OpenAPI document.
 *
 * <p>Nothing in this package is instantiated at runtime: controllers build their responses as
 * Maps/Gson objects/Mongo documents and Gson serializes them (field-based, serializeNulls — see
 * {@link net.openid.conformance.CollapsingGsonHttpMessageConverter}). These records exist solely
 * so {@code @ApiResponse(content = @Content(schema = @Schema(implementation = ...)))} can emit a
 * typed schema describing what actually goes over the wire.
 *
 * <p>The Jackson annotations here ({@code @JsonProperty("_id")} etc.) steer only springdoc's
 * schema introspection — Jackson never serializes these HTTP responses. When a controller changes
 * a response shape, the matching record here must be updated by hand.
 */
package net.openid.conformance.apidoc;
