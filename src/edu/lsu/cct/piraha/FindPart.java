package edu.lsu.cct.piraha;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class FindPart {
	static int nextId = 1;
	private static int getNextId() {
		return nextId++;
	}
	final int id = getNextId();
	boolean optional = false, star = false, stitch = true, capture = true;

	@Override
	public boolean equals(Object o) {
		if(o instanceof FindPart) {
			FindPart fb = (FindPart)o;
			return id == fb.id;
		}
		return false;
	}
	@Override
	public int hashCode() {
		return id;
	}
	
	static void buildFinder(Group g,int index,List<FindPart> fl) {
		Group sub = g.group(index);
		String pn = sub.getPatternName();
		if(pn.equals("alt")) {
			FindPart fb = new FindPart();
			for(int i=0;i<sub.groupCount();i++) {
				buildFinder(sub,i,fb.next);
			}
			if(fb.next.size()==1)
				fl.add(fb.next.get(0));
			else
				fl.add(fb);
		} else if(pn.equals("sub")) {
			if(sub.groupCount()==2) {
				String quant = sub.group(1).substring();
				if(quant.equals("*")) {
					FindPart fb = new FindPart();
					fb.star = true;
					fl.add(fb);
					buildFinder(sub,0,fb.follow);
				} else if(quant.equals("?")){
					FindPart fb = new FindPart();
					fb.optional = true;
					fl.add(fb);
					buildFinder(sub,0,fb.follow);
				} else {
					throw new Error(quant);
				}
			} else {
				buildFinder(sub,0,fl);
			}
		} else if(pn.equals("seq")||pn.equals("pat")||pn.equals("subseq")) {
			FindPart fb = new FindPart();
			fl.add(fb);
			for(int i=0;i<sub.groupCount();i++) {
				buildFinder(sub,i,fb.follow);
			}
		} else if(pn.equals("elem")) {
			FindPart fb = new FindPart();
			fb.elem = g.group(index);
			fl.add(fb);
		} else {
			throw new Error(pn);
		}
	}
	
	List<FindPart> next = new ArrayList<FindPart>();
	List<FindPart> follow = new ArrayList<FindPart>();
	Group elem;
	public void find(Group input,Set<Group> results) {
		if(elem != null) {
			String pattern = elem.group(0).substring();
			if(elem.groupCount()>1) {
				System.out.println("searching:");
				elem.dumpMatches();
			}
			if(Finder.wildMatch(pattern,input.getPatternName())) {
				if(elem.groupCount()==2 && elem.group(1).getPatternName().equals("eq")) {
					if(elem.group(2).getPatternName().equals("squote")) {
						if(!elem.group(2).substring().equals(input.substring()))
							return;
					}
				}
				Iterator<FindPart> iter = next.iterator();
				while(iter.hasNext()) {
					FindPart fb = iter.next();
					for(int j=0;j<input.groupCount();j++) {
						fb.find(input.group(j),results);
					}
				}
				if(next.size()==0 && capture) {
					results.add(input);
				}
			}
		} else if(next.size()==0) {
			return;
		} else {
			//for(FindPart fb : next) {
			Iterator<FindPart> iter = next.iterator();
			while(iter.hasNext()) {
				FindPart fb = iter.next();
				fb.find(input,results);
			}
		}
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		toString(new HashSet<FindPart>(),sb);
		return sb.toString();
	}
	
	private void toString(HashSet<FindPart> used,StringBuffer sb) {
		sb.append('$');
		sb.append(id);
		if(optional)
			sb.append('~');
		sb.append(':');
		if(elem != null) {
			sb.append(".");
			sb.append(elem.substring());
		}
		if(used.contains(this)) {
			sb.append('@');
			return;
		}
		used.add(this);
		if(next.size()==1) {
			sb.append("->");
			next.get(0).toString(used,sb);
		} else if(next.size()>1) {
			sb.append("a(");
			next.get(0).toString(used,sb);
			for(int i=1;i<next.size();i++) {
				sb.append("|");
				next.get(i).toString(used, sb);
			}
			sb.append(")");
		} else if(elem == null && follow.size()==0) {
			sb.append(".<EMPTY>");
		}
		if(follow.size()>0) {
			sb.append("f[");
			for(int i=0;i<follow.size();i++) {
				if(i>0) {
					sb.append(", ");
				}
				follow.get(i).toString(used,sb);
			}
			sb.append(']');
		}
	}
}