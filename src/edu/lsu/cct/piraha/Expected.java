package edu.lsu.cct.piraha;

import java.util.HashSet;
import java.util.Set;

public class Expected {
	Seq seq;
	int n;
	Pattern pat;
	int pos;
	Set<String> possibilities = new HashSet<String>();
	int epos;
	Or or;
	
	void build(int maxTextPos) {
		pos = maxTextPos;
		if(pat != null) {
			possibilities.add(pat.decompile());
		} else if(seq != null) {
			StringBuffer sb = new StringBuffer();
			for(int j=n;j<seq.patternList.size();j++) {
				Pattern x = seq.patternList.get(j);
				if(x instanceof Literal)
					sb.append(x.decompile());
				else if(x instanceof ILiteral)
					sb.append(x.decompile());
				else if(x instanceof Bracket)
					sb.append(x.decompile());
				else
					break;
			}
			if(sb.length()>0)
				possibilities.add(sb.toString());
		}
	}
	
	public Expected() {
	}

	public Expected(Pattern pat) {
		this.pat = pat;
	}
	
	public Expected(Seq seq,int n) {
		this.seq = seq;
		this.n = n;
	}
	
	public Expected(Or or,int pos) {
		this.or = or;
		this.epos = pos;
	}
	
	public String toString() { return possibilities.toString(); }
}
