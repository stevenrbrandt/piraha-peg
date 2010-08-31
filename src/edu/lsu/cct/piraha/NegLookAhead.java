package edu.lsu.cct.piraha;


public class NegLookAhead extends Pattern {
	Pattern pattern;
	
	public NegLookAhead(Pattern pattern) {
		this.pattern = pattern;
	}

	@Override
	public boolean match(Matcher m) {
		int pos = m.getTextPos();
		boolean b = !Matcher.matchAll(pattern, m);
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
		return "(?!"+pattern.decompile()+")";
	}

	@Override
	public boolean eq(Object obj) {
		NegLookAhead nla = (NegLookAhead)obj;
		return nla.pattern.equals(pattern);
	}
}
