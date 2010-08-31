package edu.lsu.cct.piraha;

class Range extends Pattern {
	char lo,hi;
	Range(char lo,char hi) {
		this.lo = lo;
		this.hi = hi;
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
		throw new RuntimeException();
	}
}