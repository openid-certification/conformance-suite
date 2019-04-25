package io.fintechlabs.testframework.pagination;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
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

	public Map getResults(DBCollection collection, Query query) {

		// First get the total number of unfiltered results

		long total = collection.count(query.getQueryObject());

		// Filter, sort and paginate

		if (search != null && !search.isEmpty()) {
			TextCriteria searchCriteria = new TextCriteria();

			searchCriteria.matching(search);

			query.addCriteria(searchCriteria);
		}

		DBCursor cursor = collection.find(query.getQueryObject(), query.getFieldsObject());

		cursor.sort(getSortObject());

		cursor.skip(start);
		cursor.limit(length);

		List<Map> results = cursor.toArray().stream().map(DBObject::toMap).collect(Collectors.toList());

		Map<String, Object> response = new HashMap<>();
		response.put("draw", draw);
		response.put("recordsTotal", total);
		response.put("recordsFiltered", cursor.count());
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
