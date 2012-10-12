package edu.lsu.cct.piraha;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class Seq extends Pattern {
	
	List<Pattern> patternList;
	boolean ignCase, igcShow;
	public Seq(List<Pattern> pattern,boolean ignCase,boolean igcShow) {
		this.patternList = pattern;
		this.ignCase = ignCase;
		this.igcShow = igcShow;
	}
	public Seq(Pattern...patterns) {
		this(false,false,patterns);
	}
	public Seq(boolean ignCase,boolean igcShow,Pattern...patterns) {
		patternList = new ArrayList<Pattern>(patterns.length);
		for(Pattern p : patterns)
			patternList.add(p);
	}

	@Override
	public boolean match(Matcher m) {
		//for(Pattern x = pattern;x != null;x = x.next) {
		for(int i=0;i<patternList.size();i++) {
			Pattern x = patternList.get(i);
			boolean b = x.match(m);
			if(!b) {
				m.expected(new Expected(this,i));
				return false;
			}
		}
		return true;
	}

	@Override
	public void visit(Visitor v) {
		Visitor vv = v.startVisit(this);
		for(Pattern x : patternList) {
			x.visit(vv);
		}
		v.finishVisit(this);
	}

	@Override
	public String decompile() {
		StringBuffer sb = new StringBuffer();
		if(igcShow) {
			sb.append("(");
			if(ignCase)
				sb.append("?i:");
			else
				sb.append("?-i:");
		}
		for(Pattern x : patternList)
			sb.append(x.decompile());
		if(igcShow)
			sb.append(")");
		return sb.toString();
	}

	@Override
	public boolean eq(Object obj) {
		Seq or = (Seq)obj;
		if(or.patternList.size() != patternList.size())
			return false;
		for(int i=0;i<patternList.size();i++) {
			if(!or.patternList.get(i).equals(patternList.get(i)))
				return false;
		}
		return true;
	}
	
	@Override
	public List<String> expected(int n) {
		List<String> ex = new ArrayList<String>();
		for(int i=n;i<patternList.size();i++) {
			List<String> nex = new ArrayList<String>();
			List<String> li = patternList.get(i).expected(0);
			for(int j=0;j<li.size();j++) {
				for(int k=0;k<ex.size();k++) {
					nex.add(ex.get(k)+li.get(j));
				}
			}
			ex = nex;
		}
		return ex;
	}
}
