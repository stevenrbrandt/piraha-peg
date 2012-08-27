package edu.lsu.cct.piraha;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Or extends Pattern {
	List<Pattern> patterns;
	boolean ignCase,igcShow;
	
	public Or(boolean ignCase,boolean igcShow) {
		patterns = new ArrayList<Pattern>();
		this.ignCase = ignCase;
		this.igcShow = igcShow;
	}
	public Or(boolean ignCase,boolean igcShow,Pattern...pats) {
		patterns = new ArrayList<Pattern>(pats.length);
		for(int i=0;i<pats.length;i++)
			patterns.add(pats[i]);
		this.ignCase = ignCase;
		this.igcShow = igcShow;
	}

	@Override
	public boolean match(Matcher m) {
		int posSave = m.getTextPos();
		int sz = m.subMatches.size();
		for(int i=0;i<patterns.size();i++) {
			m.setTextPos(posSave);
			int nsz = m.subMatches.size();
			while(nsz > sz) {
				m.subMatches.removeLast();
				nsz--;
			}
			boolean b = Matcher.matchAll(patterns.get(i),m);
			if(b)
				return true;
		}
		return false;
	}


	@Override
	public void visit(Visitor v) {
		Visitor vv = v.startVisit(this);
		for(Pattern x : patterns) {
			x.visit(vv);
		}
		v.finishVisit(this);
	}


	@Override
	public String decompile() {
		StringBuffer sb = new StringBuffer();
		sb.append("(");
		if (igcShow) {
			if (ignCase)
				sb.append("?i:");
			else
				sb.append("?-i:");
		}
		for(int i=0;i<patterns.size();i++) {
			if(i > 0) sb.append("|");
			sb.append(patterns.get(i).decompile());
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public boolean eq(Object obj) {
		Or or = (Or)obj;
		if(or.patterns.size() != patterns.size())
			return false;
		for(int i=0;i<patterns.size();i++) {
			if(!or.patterns.get(i).equals(patterns.get(i)))
				return false;
		}
		return true;
	}
}
