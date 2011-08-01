package edu.lsu.cct.piraha;


public class Multi extends Pattern {
	final int min,max;
	Pattern pattern;
	
	public Multi(int min,int max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public boolean match(Matcher m) {
		for(int i=0;i<max;i++) {
			int textSave = m.getTextPos();
			int sz = m.subMatches.size();
			try {
				if(!Matcher.matchAll(pattern,m) || textSave == m.getTextPos()) {
					m.setTextPos(textSave);
					int nsz = m.subMatches.size();
					while(nsz > sz) {
						m.subMatches.removeLast();
						nsz--;
					}
					return i >= min;
				}
			} catch (BreakException e) {
				return true;
			}
		}
		return true;
	}
	
	@Override public void visit(Visitor v) {
		Visitor vv = v.startVisit(this);
		pattern.visit(vv);
		v.finishVisit(this);
	}

	@Override
	public String decompile() {
		if(min == 1 && max == Integer.MAX_VALUE)
			return pattern.decompile()+"+";
		if(min == 0 && max == Integer.MAX_VALUE)
			return pattern.decompile()+"*";
		if(min == 0 && max == 1)
			return pattern.decompile()+"?";
		if(max == Integer.MAX_VALUE)
			return "{"+min+",}";
		return "{"+min+","+max+"}";
	}

	@Override
	public boolean eq(Object obj) {
		Multi m = (Multi)obj;
		return m.min == min && pattern.equals(m.pattern);
	}
}
