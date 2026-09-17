package net.openid.conformance.statistics;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How much space one collection takes up, from {@code collStats}. Passed straight through
 * to the client.
 *
 * @param collection      the collection name
 * @param count           documents in the collection
 * @param size            uncompressed size of those documents, in bytes
 * @param storageSize     size actually allocated on disk, in bytes
 * @param totalIndexSize  size of every index on the collection, in bytes
 */
@Schema(name = "StatisticsStorage")
public record StorageRow(String collection, long count, long size, long storageSize, long totalIndexSize) {
}
