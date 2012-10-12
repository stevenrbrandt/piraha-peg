package edu.lsu.cct.piraha;

import java.util.ArrayList;
import java.util.List;

public abstract class Pattern {
	public abstract boolean match(Matcher m);

	public void visit(Visitor v) {
		v.startVisit(this);
		v.finishVisit(this);
	}
	
	public abstract String decompile();
	
	public abstract boolean eq(Object obj);
	
	@Override
	public final boolean equals(Object obj) {
		if(obj != null && getClass() == obj.getClass()) {
			return eq(obj);
		}
		return false;
	}
	
	public List<String> expected(int n) { return new ArrayList<String>(); }
}
