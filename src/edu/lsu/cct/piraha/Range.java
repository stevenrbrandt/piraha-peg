package edu.lsu.cct.piraha;

import java.util.Set;

public class Range extends Pattern {
	char lo,hi;
	Range(int lo,int hi) {
		this.lo = (char) lo;
		this.hi = (char) hi;
	}
	@Override
	public boolean match(Matcher m) {
		char c = m.text.charAt(m.getTextPos());
		if(lo <= c && c <= hi) {
			m.incrTextPos(1);
			return true;
		}
		return false;
	}
	@Override
	public String decompile() {
		throw new RuntimeException();
	}
	@Override
	public boolean eq(Object obj) {
		if(obj instanceof Range) {
			Range r = (Range)obj;
			return r.lo == lo && r.hi == hi;
		}
		return false;
	}
	public Range overlap(Range rr) {
		if(rr.hi+1 >= lo && rr.hi <= hi)
			return new Range(Math.min(lo, rr.lo),Math.max(hi,rr.hi));
		if(hi+1 >= rr.lo && hi <= rr.hi)
			return new Range(Math.min(lo, rr.lo),Math.max(hi,rr.hi));
		return null;
	}
	public String toString() {
		return lo+"-"+hi;
	}
}
