package com.hypersocket.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

import com.hypersocket.resource.Resource;

public class ResourceUtils {

	public static String[] explodeValues(String values) {
		if(StringUtils.isBlank(values)) {
			return new String[] { };
		}
		StringTokenizer t = new StringTokenizer(values, "]|[");
		List<String> ret = new ArrayList<String>();
		while (t.hasMoreTokens()) {
			ret.add(t.nextToken());
		}
		return ret.toArray(new String[0]);
	}
	
	public static List<String> explodeCollectionValues(String values) {
		return Arrays.asList(explodeValues(values));
	}
	
	public static String addToValues(String values, String value) {
		List<String> vals;
		if(StringUtils.isNotBlank(value)) {
			vals = new ArrayList<String>(explodeCollectionValues(values));
		} else {
			vals = new ArrayList<String>();
		}
		vals.add(value);
		return implodeValues(vals);
	}
	
	public static <T extends Resource> String createCommaSeparatedString(Collection<T> resources) {
		return createDelimitedString(resources, ",");
	}
	
	public static <T extends Resource> String createDelimitedString(Collection<T> resources, String delimiter) {
		StringBuffer buf = new StringBuffer();
		for(Resource r : resources) {
			if(buf.length() > 0) {
				buf.append(delimiter);
			}
			buf.append(r.getName());
		}
		return buf.toString();
	}

	public static String implodeValues(String[] array) {
		return StringUtils.join(array, "]|[");	
	}
	
	public static String implodeValues(Collection<String> array) {
		return StringUtils.join(array.toArray(new String[0]), "]|[");	
	}

}
