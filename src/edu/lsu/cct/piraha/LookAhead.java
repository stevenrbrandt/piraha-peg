package edu.lsu.cct.piraha;


public class LookAhead extends Pattern {
	
	Pattern pattern;
	
	public LookAhead(Pattern pattern) {
		this.pattern = pattern;
	}

	@Override
	public boolean match(Matcher m) {
		int pos = m.getTextPos();
		boolean b = Matcher.matchAll(pattern, m);
		if(b) {
			m.setTextPos(pos);
		}
		return b;
	}
	
	@Override public void visit(Visitor v) {
		Visitor vv = v.startVisit(this);
		pattern.visit(vv);
		v.finishVisit(this);
	}

	@Override
	public String decompile() {
		return "(?="+pattern.decompile()+")";
	}

	@Override
	public boolean eq(Object obj) {
		LookAhead lh = (LookAhead)obj;
		return pattern.equals(lh.pattern);
	}
}
