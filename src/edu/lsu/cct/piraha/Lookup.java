package edu.lsu.cct.piraha;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
		int before = m.getTextPos();
		PackRat pr = m.find(fullName,before);
		if(pr.filled) {
//			System.out.println("use: "+pr);
			if(!pr.matched)
				return false;
			m.subMatches.addAll(pr.subMatches);
			m.setTextPos(pr.after);
			return true;
		}
		m.savedMatches.push(m.subMatches);
		m.subMatches = new LinkedList<Group>();
		//String lookupSave = m.lookup;
		m.lookStack.push(lookup);
		try {
			m.lookup = fullName;
			boolean b = Matcher.matchAll(pattern, m);
			final int after = m.getTextPos();
			if (b) {
				if(capture) {
					LinkedList<Group> lm = new LinkedList<Group>();
					lm.add(new Group(fullName, before, after, m.subMatches,m.text));
					m.subMatches = lm;
				}
				m.savedMatches.peek().addAll(m.subMatches);
			}
			m.addPackRat(pr, b, after, m.subMatches);
//			System.out.println("store: "+pr);
			return b;
		} finally {
			m.subMatches = m.savedMatches.pop();
			//m.lookup = lookupSave;
			m.lookStack.pop();
		}
	}
	
	@Override public void visit(Visitor v) {
		setup();
		v.startVisit(this);
		v.finishVisit(this);
	}

	@Override
	public String decompile() {
		if(capture) {
			return "{"+lookup+"}";
		} else {
			return "{-"+lookup+"}";
		}
	}
	@Override
	public boolean eq(Object obj) {
		Lookup lh = (Lookup)obj;
		return lookup.equals(lh.lookup) && capture == lh.capture;
	}
}
