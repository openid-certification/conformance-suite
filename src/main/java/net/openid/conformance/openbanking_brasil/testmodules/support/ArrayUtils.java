package net.openid.conformance.openbanking_brasil.testmodules.support;

import java.lang.reflect.Array; 

public class arrayUtils {

	static public <T> T concatArrays(T array1, T array2) {
		if (!array1.getClass().isArray() || !array2.getClass().isArray()) {
			throw new IllegalArgumentException("Only arrays are accepted.");
		}
	
		Class<?> compType1 = array1.getClass().getComponentType();
		Class<?> compType2 = array2.getClass().getComponentType();
	
		if (!compType1.equals(compType2)) {
			throw new IllegalArgumentException("Two arrays have different types.");
		}
	
		int len1 = Array.getLength(array1);
		int len2 = Array.getLength(array2);
	
		@SuppressWarnings("unchecked")
		//the cast is safe due to the previous checks
		T result = (T) Array.newInstance(compType1, len1 + len2);
	
		System.arraycopy(array1, 0, result, 0, len1);
		System.arraycopy(array2, 0, result, len1, len2);
	
		return result;
	}
}