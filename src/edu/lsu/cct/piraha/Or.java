package edu.lsu.cct.piraha;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Or extends Pattern {
	List<Pattern> patterns;
	boolean ignCase,igcShow;
	
	public Or(boolean ignCase,boolean igcShow) {
		patterns = new ArrayList<Pattern>();
		this.ignCase = ignCase;
		this.igcShow = igcShow;
	}
	public Or(Pattern...pats) {
		this(false,false,pats);
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
		List<Expected> expecteds = new ArrayList<Expected>();
		StringBuilder whSave = new StringBuilder();
		List<Integer> whListSave = new ArrayList<Integer>();
		whSave.append(m.white);
		for(int n : m.whiteThresholds)
			whListSave.add(n);
		for(int i=0;i<patterns.size();i++) {
			m.setTextPos(posSave);
			int nsz = m.subMatches.size();
			while(nsz > sz) {
				m.subMatches.removeLast();
				nsz--;
			}
			
			// Possibly this can be optimized.
			m.white.setLength(0);
			m.white.append(whSave);
			m.whiteThresholds.clear();
			for(int n : whListSave)
				m.whiteThresholds.add(n);
			
			boolean b = Matcher.matchAll(patterns.get(i),m);
			if(b)
				return true;
			else if(m.expected != null)
				expecteds.add(m.expected);
		}
		int max = -1;
		for(Expected e : expecteds) {
			if(e.pos > max && e.possibilities.size()>0) {
				max = e.pos;
			}
		}
		Expected ex = new Expected();
		ex.epos = max;
		for(Expected e : expecteds) {
			if(e.pos == max) {
				for(String s : e.possibilities) {
					ex.possibilities.add(s);
				}
			}
		}
		m.expected(ex);
		// new version
//		LinkedList<Group> save = m.subMatches;
//		m.subMatches = new LinkedList<Group>();
//		LinkedList<Group> best = m.subMatches;
//		int bestPos = -1;
//		for(int i=0;i<patterns.size();i++) {
//			m.setTextPos(posSave);
//			m.subMatches = new LinkedList<Group>();
//			boolean b = Matcher.matchAll(patterns.get(i), m);
//			if(b) {
//				int np = m.getTextPos();
//				if(np > bestPos) {
//					best = m.subMatches;
//					bestPos = np;
//				}
//			}
//		}
//		if(bestPos >= 0) {
//			save.addAll(best);
//			m.subMatches = save;
//			m.setTextPos(bestPos);
//			return true;
//		}
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
		if(!igcShow && patterns.size()==1) {
			return patterns.get(0).decompile();
		}
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
	
	@Override
	public List<String> expected(int n) {
		List<String> ex = new ArrayList<String>();
		for(int i=0;i<patterns.size();i++) {
			List<String> li = patterns.get(i).expected(0);
			for(String s : li)
				ex.add(s);
		}
		return ex;
	}
}
