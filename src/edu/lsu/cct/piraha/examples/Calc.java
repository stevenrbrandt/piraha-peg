package edu.lsu.cct.piraha.examples;

import edu.lsu.cct.piraha.Grammar;
import edu.lsu.cct.piraha.Group;
import edu.lsu.cct.piraha.Matcher;

public class Calc {
	public static Grammar makeMath() {
		Grammar math = new Grammar();
		// All the various was a double precision number (or integer) can be written
		math.compile("num", "([0-9]+\\.[0-9]+|[0-9]+\\.|\\.[0-9]+|[0-9]+)([eEdD](\\+|-|)[0-9]+|)");
		
		// The basic operators
		math.compile("addop","\\+|-");
		math.compile("mulop","\\*|/");
		math.compile("powop","\\*\\*");
		
		math.compile("neg","-"); // gives us a hook
		
		// All the different ways we can stick things together, includes parenthesis.
		// Note: The start of "expr" should not be {expr}
		// because that leads to infinite recursion.
		math.compile("expr","{mulexp}({addop}{mulexp})*");
		math.compile("mulexp","{powexp}({mulop}{powexp})*");
		math.compile("powexp","{num}({powop}{num})*|\\({-expr}\\)");
		
		return math;
	}
	
	public static void main(String[] args) {
		Grammar math = makeMath();
		
		Matcher m = math.matcher("expr", "3.0*2.0+1.0e1*(2+4)");//"3*3+4*4+2*(.95e0+1.05)+2**(1.5*2)"); // a small test
		boolean b = m.matches();
		System.out.println("match? "+b);
		if(b) {
			m.dumpMatches();
			System.out.println("node count="+count(m));
			System.out.println("eval="+evalExpr(m));
		}
	}

	private static int count(Group m) {
		int n = 1;
		for(int i=0;i<m.groupCount();i++) {
			n += count(m.group(i));
		}
		return n;
	}

	private static double eval(Group m) {
		if(m.getPatternName().equals("num")) {
			return getValue(m);
		} else {
			return evalExpr(m);
		}
	}

	private static double evalExpr(Group match) {

		if(hasPattern(match,"powexp|expr|mulexp|num","mulop|addop|powop","powexp|expr|mulexp|num")) {
			double n1 = eval(match.group(0));
			String op = getOp(match.group(1));
			double n2 = eval(match.group(2));
			return eval(n1,op,n2);
		} else if(hasPattern(match,"expr")) {
			return evalExpr(match.group(0));
		} else if(hasPattern(match,"powexp")) {
			return evalExpr(match.group(0));
		} else if(hasPattern(match,"mulexp")) {
			return evalExpr(match.group(0));
		} else if(hasPattern(match,"addexp")) {
			return evalExpr(match.group(0));
		} else if(hasPattern(match, "neg", "val")) {
			return -getValue(match.group(1));
		} else if(hasPattern(match, "neg", "num")) {
			return -getValue(match.group(1));
		} else if(hasPattern(match, "num")) {
			return getValue(match.group(0));
		}
		StringBuffer sb = new StringBuffer();
		sb.append("matches.size=");
		sb.append(match.groupCount());
		for(int i=0;i<match.groupCount();i++) {
			sb.append(", ");
			sb.append(match.group(i).getPatternName());
		}
		throw new RuntimeException(sb.toString());
	}

	private static double eval(double n1, String op, double n2) {
		if("-".equals(op))
			return n1-n2;
		else if("+".equals(op))
			return n1+n2;
		else if("**".equals(op))
			return Math.pow(n1,n2);
		else if("*".equals(op))
			return n1*n2;
		else if("/".equals(op))
			return n1/n2;
		else
			throw new RuntimeException("unknown op: "+op);
	}

	private static String getOp(Group match) {
		return match.substring();
	}

	private static double getValue(Group match) {
		return Double.parseDouble(match.substring());
	}

	private static boolean hasPattern(Group match, String... sl) {
		if(match.groupCount() != sl.length)
			return false;
		for(int i=0;i<sl.length;i++) {
			String haystack = "|"+sl[i]+"|";
			String needle = "|"+match.group(i).getPatternName()+"|";
			if(!haystack.contains(needle))
				return false;
		}
		return true;
	}
}
