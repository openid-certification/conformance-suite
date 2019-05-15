package io.fintechlabs.testframework.pagination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Field;

import com.mongodb.AggregationOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.Cursor;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

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

	public Map getResults(DBCollection collection, CriteriaDefinition criteria) {

		return getResults(collection, criteria.getCriteriaObject(), Collections.emptyList());
	}

	public Map getResults(DBCollection collection, CriteriaDefinition criteria, Field fields) {

		return getResults(collection, criteria.getCriteriaObject(),
				Collections.singletonList(new BasicDBObject("$project", fields.getFieldsObject())));
	}

	public Map getResults(DBCollection collection, DBObject criteria, List<DBObject> projection) {

		return getResults(collection, Collections.singletonList(new BasicDBObject("$match", criteria)), projection);
	}

	public Map getResults(DBCollection collection, List<DBObject> selection, List<DBObject> projection) {

		List<DBObject> pipeline = new ArrayList<DBObject>(selection);

		// First get the total number of unfiltered results
		long total = aggregateCount(collection, pipeline);
		long filteredCount = total;

		// Update the criteria with search term, if any
		if (search != null && !search.isEmpty()) {
			// Mongo requires text search to come first in the pipeline
			pipeline.add(0, new BasicDBObject("$match", new BasicDBObject("$text", new BasicDBObject("$search", search))));

			// Count the filtered results
			filteredCount = aggregateCount(collection, pipeline);
		}

		// Sort and paginate
		pipeline.add(new BasicDBObject("$sort", getSortObject()));
		pipeline.add(new BasicDBObject("$skip", start));
		pipeline.add(new BasicDBObject("$limit", length));
		pipeline.addAll(projection);

		// Force the driver to include a 'cursor' option - Mongo complains otherwise.
		AggregationOptions options = AggregationOptions.builder()
				.outputMode(AggregationOptions.OutputMode.CURSOR)
				.build();

		Cursor cursor = collection.aggregate(pipeline, options);

		List<Map> results = StreamSupport.stream(Spliterators.spliteratorUnknownSize(cursor, Spliterator.ORDERED), false)
				.map(DBObject::toMap)
				.collect(Collectors.toList());

		Map<String, Object> response = new HashMap<>();
		response.put("draw", draw);
		response.put("recordsTotal", total);
		response.put("recordsFiltered", filteredCount);
		response.put("data", results);

		return response;
	}

	private static long aggregateCount(DBCollection collection, List<DBObject> selection) {

		// Have to do this explicitly since DBCollection only supports
		// criteria-based selection.

		List<DBObject> pipeline = new ArrayList<DBObject>(selection);
		pipeline.add(new BasicDBObject("$count", "count"));

		// Force the driver to include a 'cursor' option - Mongo complains otherwise.
		AggregationOptions options = AggregationOptions.builder()
				.outputMode(AggregationOptions.OutputMode.CURSOR)
				.build();

		Cursor cursor = collection.aggregate(pipeline, options);
		if (cursor.hasNext()) {
			DBObject result = cursor.next();
			return new BasicDBObject(result.toMap()).getLong("count");
		} else {
			return 0;
		}
	}

	private DBObject getSortObject() {

		BasicDBObjectBuilder sortOrder = BasicDBObjectBuilder.start();

		String[] orderParts = order.split(",");
		for (int i = 0; i < orderParts.length; i += 2) {
			String column = orderParts[i];
			String dir = (i + 1 < orderParts.length) ? orderParts[i + 1] : "asc";
			sortOrder.add(column, dir.equals("desc") ? -1 : 1);
		}

		return sortOrder.get();
	}
}
