package net.openid.conformance.util

import com.google.gson.JsonArray
import com.google.gson.JsonObject

fun JsonObject.addProperty(name: String, vararg values: String)  {
	val arr = JsonArray()
	for (v in values) {
		arr.add(v)
	}
	this.add(name, arr)
}

fun JsonObject.addPropertyArray(name: String, v: String)  {
	val arr = JsonArray()
	arr.add(v)
	this.add(name, arr)
}

fun JsonObject.addProperties( vararg data: Pair<String, String>)  {
	for ((key, value) in data) {
		this.addProperty(key, value)
	}
}

