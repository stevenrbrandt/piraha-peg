package edu.lsu.cct.piraha;

import java.util.LinkedList;
import java.util.List;

public class Bracket extends Pattern {
	boolean neg;
	List<Range> ranges = new LinkedList<Range>();
	
	public void addRange(char lo,char hi) {
		if(lo > hi) throw new ParseException("Invalid range "+lo+" > "+hi);
		ranges.add(new Range(lo,hi));
	}
	
	public void addChar(char v) {
		ranges.add(new Range(v,v));
	}

	@Override
	public boolean match(Matcher m) {
		if(m.getTextPos() >= m.text.length())
			return false;
		char c = m.text.charAt(m.getTextPos());
		for(Range range : ranges) {
			if(range.lo <= c && c <= range.hi) {
				if(!neg)
					m.incrTextPos(1);
				return neg^true;
			}
		}
		if(neg)
			m.incrTextPos(1);
		return neg^false;
	}
	
	@Override public void visit(Visitor v) {
		Visitor vv = v.startVisit(this);
		for(Range range : ranges) {
			range.visit(vv);
		}
		v.finishVisit(this);
	}

	@Override
	public String decompile() {
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		if(neg)
			sb.append("^");
		for(Range range : ranges) {
			if(range.lo != range.hi) {
				outc(sb,range.lo);
				sb.append("-");
				outc(sb,range.hi);
			} else {
				outc(sb,range.lo);
			}
		}
		sb.append("]");
		return sb.toString();
	}

	private void outc(StringBuffer sb, char c) {
		if(c == '-' || c == ']') {
			sb.append("\\");
		}
		sb.append(Literal.outc(c));
	}

	@Override
	public boolean eq(Object obj) {
		Bracket b = (Bracket)obj;
		if(ranges.size() != b.ranges.size())
			return false;
		for(int i=0;i<ranges.size();i++) {
			Range brange = b.ranges.get(i);
			Range range = ranges.get(i);
			if(brange.lo != range.lo)
				return false;
			if(brange.hi != range.hi)
				return false;
		}
		return true;
	}
}
