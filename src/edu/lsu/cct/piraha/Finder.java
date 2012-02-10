package edu.lsu.cct.piraha;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is a utility for extracting Groups from
 * within a parse tree. So, for example, if your
 * parse tree looks like this:
 * <pre>
 * pat:
  [0] seq:
    [0] elem:
      [0] name=(a)
    [1] elem:
      [0] name=(b)
    [2] elem:
      [0] name=(c)
   </pre>
 * 
 * You can extract all the elems by doing this:
   <pre>
   Group input = [the above parse tree]
   Finder f = new Finder("pat.seq.elem");
   Set results = f.find(input);
   </pre>
 * Special syntax:
 * Optional pattern: (.pattern)?
 * Alternatives: (.pat|.seq)
 * Quantifier (match any number of occurrences of the thing inside) : (.pat)* 
 * Wild Card in name (the example would match anything starting with p) : .p*
 * @author sbrandt
 *
 */
public class Finder {
	Grammar g = new Grammar();
	Group rawPattern;
	FindPart findBase;
	
	public Finder(String pattern) {
		g.compile("name","[a-zA-Z*][a-zA-Z_0-9*]*");
		g.compile("s","\\b[ \t\r\n]*");
		g.compile("seq","({sub}|{elem})(\\.{elem}|{sub})*");
		g.compile("subseq","(\\.{elem}|{sub})+");
		g.compile("num","[0-9]+");
		g.compile("value","count|index");
		g.compile("check","{value}{-s}{cmp}{-s}{num}|\\({check}({-s}{and_or}{-s}{check})*\\)");
		g.compile("cmp","<=?|>=?|!=|=");
		g.compile("range_set","({num}|{check}({-s}{and_or}{-s}{check})*)");
		g.compile("and_or","[&|]");
		g.compile("quant","[*?]");
		g.compile("alt","{subseq}(\\|{subseq})*");
		g.compile("sub","\\({alt}\\)({quant}|)");
		g.compile("dquote","\"(\\\\[^]|[^\\\\\"])*\"");
		g.compile("squote","'(\\\\[^]|[^\\\\'])*'");
		g.compile("eq","=~?");
		g.compile("elem","{name}(\\[{range_set}\\]|)({eq}({squote}|{dquote})|)");
		g.compile("pat","\\.?{seq}$");
		Matcher m = g.matcher(pattern);
		if(!m.matches())
			throw new Error(""+m.near());
		m.dumpMatches();
		rawPattern = m.group();
		List<FindPart> fl = new ArrayList<FindPart>();
		FindPart.buildFinder(new Group(rawPattern), 0, fl);
		FindPart st = Finder.stitch(fl);
		findBase = Finder.strip(new HashSet<FindPart>(),st);
	}
	
	public Set<Group> find(Group gr) {
		Set<Group> lig = new HashSet<Group>();
		findBase.find(gr, lig);
		return lig;
	}

	static FindPart stitch(List<FindPart> fl) {
		if(fl.size()==0)
			return null;
		FindPart root = null;
		FindPart last = null;
		for(int i=fl.size()-1;i>=0;i--) {
			last = root;
			root = fl.get(i);
			FindPart fb = stitch(root.follow);
			root.follow = new ArrayList<FindPart>();
			if(root.optional || root.star) {
				FindPart bridge = new FindPart();
				FindPart newRoot = new FindPart();
				newRoot.next.add(fb);
				if(root.star) {
					newRoot.next.add(dup(fb));
					stitch(fb,newRoot);
				} else {
					newRoot.next.add(bridge);
				}
				stitch(fb,last);
				stitch(bridge,last);
				root = newRoot;
				continue;
			}
			if(fb != null) {
				for(int j=0;j<root.next.size();j++) {
					stitch(root.next.get(j),fb);
				}
				if(root.next.size()==0) {
					root.next.add(fb);
				}
			}
			for(int j=0;j<root.next.size();j++) {
				FindPart rootj = root.next.get(j);
				FindPart nb = stitch(rootj.follow);
				rootj.follow = new ArrayList<FindPart>();
				stitch(rootj,nb);
			}
			if(last != null) {
				for(int j=0;j<root.next.size();j++) {
					FindPart rootj = root.next.get(j);
					stitch(rootj,last);
				}
				if(root.next.size()==0) {
					root.next.add(last);
				}
			}
		}
		return root;
	}

	private static FindPart dup(FindPart fb) {
		FindPart nfb = new FindPart();
		nfb.elem = fb.elem;
		for(FindPart f : fb.next) {
			nfb.next.add(dup(f));
		}
		return nfb;
	}

	private static void stitch(FindPart root, FindPart fb) {
		if(fb == null)
			return;
		for(int i=0;i<root.next.size();i++) {
			stitch(root.next.get(i),fb);
		}
		if(root.next.size()==0) {
			root.next.add(fb);
		}
	}

	static FindPart strip(HashSet<FindPart> used,FindPart fb) {
		if(used.contains(fb)) {
			return fb;
		}
		used.add(fb);
		while(fb.next.size()==1 && fb.elem == null) {
			fb = fb.next.get(0);
		}
		for(int i=0;i<fb.next.size();i++) {
			fb.next.set(i,strip(used,fb.next.get(i)));
		}
		return fb;
	}

	static class FindPart {
		static int nextId = 1;
		private static int getNextId() {
			return nextId++;
		}
		final int id = getNextId();
		boolean optional = false, star = false;

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
				if(Find.wildMatch(pattern,input.getPatternName())) {
					for(FindPart fb : next) {
						for(int j=0;j<input.groupCount();j++) {
							fb.find(input.group(j),results);
						}
					}
					if(next.size()==0) {
						results.add(input);
					}
				}
			} else if(next.size()==0) {
				return;
			} else {
				for(FindPart fb : next) {
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
						sb.append(", ");;
					}
					follow.get(i).toString(used,sb);
				}
				sb.append(']');
			}
		}
	}
}
