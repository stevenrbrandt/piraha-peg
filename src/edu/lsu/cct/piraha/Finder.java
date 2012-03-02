package edu.lsu.cct.piraha;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
		Set<Group> lig = new LinkedHashSet<Group>();
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
				FindPart newRoot = new FindPart();
				newRoot.next.add(fb);
				if(root.star) {
					FindPart blockStitch = new FindPart();
					blockStitch.stitch = false;
					FindPart dupfb = dup(fb);
					newRoot.next.add(dupfb);
					blockStitch.next.add(newRoot);
					stitch(dupfb,blockStitch);
				} else {
					FindPart bridge = new FindPart();
					newRoot.next.add(bridge);
					stitch(bridge,last);
				}
				stitch(fb,last);
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
		if(fb == null || !root.stitch)
			return;
		int n = 0;
		if(root.stitch) {
			for(int i=0;i<root.next.size();i++) {
				FindPart rooti = root.next.get(i);
				if(!rooti.stitch)
					continue;
				stitch(rooti,fb);
				n++;
			}
		}
		if(n==0) {
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

	public static boolean wildMatch(String wild, String str) {
		return wildMatch(wild,0,str,0);
	}

	private static boolean wildMatch(String wild, int i, String str, int j) {
		while(true) {
			if(i == wild.length() && j == str.length())
				return true;
			else if(i + 1 == wild.length() && j == str.length() && wild.charAt(i)=='*')
				return true;
			else if(i == wild.length() || j == str.length())
				return false;
			char wc = wild.charAt(i);
			char sc = str.charAt(j);
			if(wc == sc) {
				i++; j++;
			} else if(wc == '*') {
				if(wildMatch(wild,i+1,str,j))
					return true;
				j++;
			} else if(wc != sc) {
				return false;
			}
		}
	}
}
