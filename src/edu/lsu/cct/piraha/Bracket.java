package edu.lsu.cct.piraha;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Bracket extends Pattern implements Comparable<Bracket> {
	boolean neg;
	List<Range> ranges = new LinkedList<Range>();
	
	public Bracket() {}
	public Bracket(boolean neg) {
		this.neg = neg;
	}
	
	void addRange(char lo,char hi,boolean ignCase) {
		if(ignCase) {
			addRange(Character.toLowerCase(lo), Character.toLowerCase(hi));
			addRange(Character.toUpperCase(lo), Character.toUpperCase(hi));
		} else {
			addRange(lo,hi);
		}
	}
	/**
	 * Adds a range between lo and hi but will not create an overlap. 
	 * @param lo
	 * @param hi
	 */
	Bracket addRange(char lo,char hi) {
		if(lo > hi) throw new ParseException("Invalid range "+lo+" > "+hi);
		Range rr = new Range(lo,hi), over = null;
		int i=0;
		for(;i<ranges.size();i++) {
			Range r = ranges.get(i);
			over = r.overlap(rr);
			if(over != null) {
				ranges.set(i, over);
				break;
			}
			if(rr.hi < r.lo) {
				ranges.add(i,rr);
				return this;
			}
		}
		if(over == null) {
			ranges.add(rr);
			return this;
		}
		while(i+1<ranges.size()) {
			Range r = ranges.get(i+1);
			over = over.overlap(r);
			if(over == null)
				return this;
			ranges.remove(i+1);
			ranges.set(i,over);
		}
		return this;
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
				if(!neg^true) {
					m.expected(new Expected(this));
				}
				return neg^true;
			}
		}
		if(neg)
			m.incrTextPos(1);
		if(!neg^false) {
			m.expected(new Expected(this));
		}
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
		for(int i=0;i<ranges.size();i++) {
			Range range = ranges.get(i);
			if(range.lo != range.hi) {
				outc(sb,range.lo);
				sb.append("-");
				outc(sb,range.hi);
			} else if(i==0 || i+1 == ranges.size()) {
				outc1(sb,range.lo);
			} else {
				outc(sb,range.lo);
			}
		}
		sb.append("]");
		return sb.toString();
	}

	private void outc1(StringBuffer sb, char c) {
		sb.append(Literal.outc(c));
	}
	private void outc(StringBuffer sb, char c) {
		if(c == '-') {
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
	
	/**
	 * Create a new bracket by logically or-ing against a given one.
	 * @param b
	 * @return
	 */
	public Bracket or(Bracket b) {
		Bracket nb = new Bracket();
		for(Range r : ranges) {
			nb.addRange(r.lo, r.hi);
		}
		for(Range r : b.ranges) {
			nb.addRange(r.lo, r.hi);
		}
		return nb;
	}
	
	/**
	 * Create a new bracket by performing a logical not.
	 * @return
	 */
	public Bracket not() {
		Bracket b = new Bracket();
		if(ranges.size()==0) {
			b.addRange(Character.MIN_VALUE,Character.MAX_VALUE);
			return b;
		}
		if(ranges.get(0).lo > Character.MIN_VALUE) {
			b.addRange(Character.MIN_VALUE,(char) (ranges.get(0).lo-1));
		}
		for(int i=1;i<ranges.size();i++) {
			b.addRange((char)(ranges.get(i-1).hi+1), (char)(ranges.get(i).lo-1));
		}
		if(ranges.get(ranges.size()-1).hi < Character.MAX_VALUE)
			b.addRange((char)(ranges.get(ranges.size()-1).hi+1),Character.MAX_VALUE);
		return b;
	}
	// TODO: Replace with a more efficient method
	public Bracket and(Bracket b) {
		return not().or(b.not()).not();
	}
	
	public static void test(char... ch) {
		Bracket b = new Bracket();
		for(int i=0;i<ch.length-2;i+=2) {
			b.addRange(ch[i],ch[i+1]);
		}
		boolean found = false;
		for(Range r : b.ranges) {
			if(r.lo == ch[ch.length-2] && r.hi == ch[ch.length-1])
				found = true;
		}
		if(!found)
			throw new Error();
	}
	public static void testSame(Bracket a,Bracket b) {
		if(!a.equals(b)) {
			throw new Error(a.decompile()+" != "+b.decompile());
		}
	}

	public boolean empty() {
		return ranges.size()==0;
	}
	
	@Override
	public int hashCode() {
		if(ranges.size()==0)
			return 0;
		return ranges.get(0).lo;
	}
	
	public static void main(String[] args) {
		test('a','d','e','h','a','h');
		test('e','h','a','d','a','h');
		test('e','h','a','c','a','c');
		test('a','c','k','m','j','j','j','m');
		test('a','c','k','m','h','h','i','j','h','m');
		Bracket b = new Bracket();
		b.addRange('b','m');
		Bracket b2 = new Bracket();
		b2.addRange('h','z');
		Bracket b3 = new Bracket();
		b3.addRange(Character.MIN_VALUE,'g');
		b3.addRange((char)('z'+1),Character.MAX_VALUE);
		testSame(b3,b2.not());
		Bracket b4 = new Bracket();
		b4.addRange('h','m');
		testSame(b.and(b2),b4);
		System.out.println("all tests passed");
	}

	@Override
	public int compareTo(Bracket b) {
		return hashCode() - b.hashCode();
	}
	
	@Override
	public List<String> expected(int n) {
		List<String> ex = new ArrayList<String>();
		ex.add(decompile());
		return ex;
	}
}
