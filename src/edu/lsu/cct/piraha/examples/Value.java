package edu.lsu.cct.piraha.examples;

import edu.lsu.cct.piraha.Group;

public class Value {
	Object o;
	Group g;
	public Value(Group g) {
		o = null;
		this.g = g;
	}
	public Value(Boolean b,Group g) {
		o = b;
		this.g = g;
	}
	public Value(Number n,Group g) {
		o = n;
		this.g = g;
	}
	boolean getBool() {
		/*if(o == null || !(o instanceof Boolean)) {
			g.dumpMatches();
		}*/
		if(o == null) return false;
		if(o instanceof Long) {
			return ((Long)o).longValue() != 0;
		}
		return ((Boolean)o).booleanValue();
	}
	double getDouble() {
		if(o == null) return 0;
		return ((Number)o).doubleValue();
	}
	long getLong() {
		if(o == null) return 0;
		return ((Number)o).longValue();
	}
	public boolean isInt() {
		return o == null || o instanceof Long;
	}
	public String toString() {
		if(o == null)
			return "Void";
		else if(o instanceof Boolean)
			return "Boolean "+o;
		else if(o instanceof Long)
			return "Long "+o;
		else
			return "Double "+o;
	}
}
