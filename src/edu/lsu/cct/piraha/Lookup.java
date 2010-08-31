package edu.lsu.cct.piraha;

import java.util.LinkedList;

public class Lookup extends Pattern {
	final Grammar g;
	final String lookup;
	final String grammarName;
	final boolean capture;
	Pattern pattern = null;
	Grammar grammar = null;
	final String fullName;
	
	public Lookup(String lookup,Grammar g) {
		this.g = g;
		if(lookup.startsWith("-")) {
			lookup = lookup.substring(1);
			capture = false;
		} else {
			capture = true;
		}
		this.fullName = lookup;
		int n = lookup.indexOf(':');
		if(n >= 0) {
			grammarName = lookup.substring(0,n);
			lookup = lookup.substring(n+1);
		} else {
			grammarName = null;
			grammar = g;
		}
		this.lookup = lookup;
	}

	private void setup() {
		if(grammar == null) {
			if(g.grammars != null && g.grammars.containsKey(grammarName)) {
				grammar = g.grammars.get(grammarName);
			} else {
				throw new MatchException("No such grammar: "+grammarName);
			}
		}
		if(pattern == null) {
			if(grammar.patterns.containsKey(lookup))
				pattern = grammar.patterns.get(lookup);
			else
				throw new MatchException("No such grammar rule: "+lookup+" in "+grammar.patterns.keySet());
		}
	}

	@Override
	public boolean match(Matcher m) {
		setup();
		if (capture) {
			m.savedMatches.push(m.subMatches);
			m.subMatches = new LinkedList<Group>();
			String lookupSave = m.lookup;
			try {
				int before = m.getTextPos();
				m.lookup = fullName;
				boolean b = Matcher.matchAll(pattern, m);
				if (b) {
					int after = m.getTextPos();
					m.savedMatches.peek().add(new Group(fullName, before, after, m.subMatches,m.text));
				}
				return b;
			} finally {
				m.subMatches = m.savedMatches.pop();
				m.lookup = lookupSave;
			}
		} else {
			return Matcher.matchAll(pattern,m);
		}
	}
	
	@Override public void visit(Visitor v) {
		setup();
		v.startVisit(this);
		v.finishVisit(this);
	}

	@Override
	public String decompile() {
		return (capture ? "{" : "{-")+lookup+"}";
	}
	@Override
	public boolean eq(Object obj) {
		Lookup lh = (Lookup)obj;
		return lookup.equals(lh.lookup) && capture == lh.capture;
	}
}
