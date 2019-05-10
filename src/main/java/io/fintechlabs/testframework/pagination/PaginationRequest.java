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

		// First get the total number of unfiltered results
		long total = collection.count(criteria);

		// Update the criteria with search term, if any
		if (search != null && !search.isEmpty()) {
			criteria = new BasicDBObject("$and",
					new DBObject[] {
							criteria,
							new BasicDBObject("$text", new BasicDBObject("$search", search))
					});
		}

		// Count the filtered results
		long filteredCount = collection.count(criteria);

		// Sort and paginate
		List<DBObject> pipeline = new ArrayList<DBObject>();

		pipeline.add(new BasicDBObject("$match", criteria));
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
