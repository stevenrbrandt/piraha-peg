package edu.lsu.cct.piraha;

import java.util.LinkedList;

public class Name extends Pattern {
	String name;
	
	public Name(String name) {
		this.name = name;
	}

	@Override
	public boolean match(Matcher m) {
		for(Group match : m.subMatches) {
			boolean b = comp(m,match);
			if(b)
				return b;
		}
		for(LinkedList<Group> list : m.savedMatches) {
			for (Group match : list) {
				boolean b = comp(m, match);
				if (b)
					return b;
			}
		}
		return false;
	}

	private boolean comp(Matcher m, Group match) {
		if(!match.getPatternName().equals(name))
			return false;
		for(int i=0;true;i++) {
			if(i+match.getBegin() >= match.getEnd()) {
				m.setTextPos(i+m.getTextPos());
				return true;
			}
			char c1 = m.text.charAt(i+match.getBegin());
			char c2 = m.text.charAt(i+m.getTextPos());
			if(c1 != c2)
				return false;
			if(i+m.getTextPos() >= m.text.length())
				return false;
		}
	}

	@Override
	public String decompile() {
		return "{$"+name+"}";
	}

	@Override
	public boolean eq(Object obj) {
		Name n = (Name)obj;
		return n.name.equals(name);
	}
}
