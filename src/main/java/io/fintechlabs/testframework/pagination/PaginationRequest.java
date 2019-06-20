package io.fintechlabs.testframework.pagination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Field;

import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;

public class PaginationRequest {

	private int draw;

	private int start;

	private int length;

	private String search;

	private String order;

	public int getDraw() {
		return draw;
	}

	public void setDraw(int draw) {
		this.draw = draw;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public Map getResults(MongoCollection<Document> collection, CriteriaDefinition criteria) {

		return getResults(collection, criteria.getCriteriaObject(), Collections.emptyList());
	}

	public Map getResults(MongoCollection<Document> collection, CriteriaDefinition criteria, Field fields) {

		return getResults(collection, criteria.getCriteriaObject(),
				Collections.singletonList(new Document("$project", fields.getFieldsObject())));
	}

	public Map getResults(MongoCollection<Document> collection, Bson criteria, List<Bson> projection) {

		return getResults(collection, Collections.singletonList(new Document("$match", criteria)), projection);
	}

	public Map getResults(MongoCollection<Document> collection, List<Bson> selection, List<Bson> projection) {

		List<Bson> pipeline = new ArrayList<Bson>(selection);

		// First get the total number of unfiltered results
		long total = aggregateCount(collection, pipeline);
		long filteredCount = total;

		// Update the criteria with search term, if any
		if (search != null && !search.isEmpty()) {
			// Mongo requires text search to come first in the pipeline
			pipeline.add(0, new Document("$match", new Document("$text", new Document("$search", "\"" + search + "\""))));

			// Count the filtered results
			filteredCount = aggregateCount(collection, pipeline);
		}

		// Sort and paginate
		pipeline.add(new Document("$sort", getSortObject()));
		pipeline.add(new Document("$skip", start));
		pipeline.add(new Document("$limit", length));
		pipeline.addAll(projection);

		List<Map> results = Lists.newArrayList(collection.aggregate(pipeline));

		Map<String, Object> response = new HashMap<>();
		response.put("draw", draw);
		response.put("recordsTotal", total);
		response.put("recordsFiltered", filteredCount);
		response.put("data", results);

		return response;
	}

	private static long aggregateCount(MongoCollection<Document> collection, List<Bson> selection) {

		// Have to do this explicitly since MongoCollection only supports
		// criteria-based selection.

		List<Bson> pipeline = new ArrayList<Bson>(selection);
		pipeline.add(new Document("$count", "count"));

		Document result = collection.aggregate(pipeline).first();

		return result != null ? ((Number) result.get("count")).longValue() : 0;
	}

	private Document getSortObject() {

		Document sortOrder = new Document();

		String[] orderParts = order.split(",");
		for (int i = 0; i < orderParts.length; i += 2) {
			String column = orderParts[i];
			String dir = (i + 1 < orderParts.length) ? orderParts[i + 1] : "asc";
			sortOrder.append(column, dir.equals("desc") ? -1 : 1);
		}

		return sortOrder;
	}
}
