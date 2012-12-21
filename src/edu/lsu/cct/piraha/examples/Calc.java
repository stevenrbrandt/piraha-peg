package edu.lsu.cct.piraha.examples;

import edu.lsu.cct.piraha.Grammar;
import edu.lsu.cct.piraha.Group;
import edu.lsu.cct.piraha.Matcher;

public class Calc {
	public static Grammar makeMath() {
		Grammar math = new Grammar();
		// All the various was a double precision number (or integer) can be
		// written
		math.compile("num",
				"-?([0-9]+\\.[0-9]+|[0-9]+\\.|\\.[0-9]+|[0-9]+)([eEdD](\\+|-|)[0-9]+|)");

		// The basic operators
		math.compile("addop", "\\+|-");
		math.compile("mulop", "\\*|/");
		math.compile("powop", "\\*\\*");

		math.compile("neg", "-"); // gives us a hook

		// All the different ways we can stick things together, includes
		// parenthesis.
		// Note: The start of "expr" should not be {expr}
		// because that leads to infinite recursion.
		math.compile("mulexp", "{powexp}({mulop}{powexp})*");
		math.compile("powexp", "{num}({powop}{num})*|\\({expr}\\)");
		math.compile("expr", "{mulexp}({addop}{mulexp})*");

		return math;
	}
	
	Grammar math = makeMath();
	
	public double eval(String s) {
		Matcher m = math.matcher("expr",s.trim());
		if(m.matches())
			return evalExpr(m);
		else
			return Double.NaN;
	}

	public static void main(String[] args) {
		Grammar math = makeMath();

		Matcher m = math.matcher("expr", "1+2*4+4**3**2"); // answer is 262153
		
		boolean b = m.matches();
		System.out.println("match? " + b);
		if (b) {
			m.dumpMatchesXML();
			System.out.println("node count=" + count(m));
			System.out.println("eval=" + evalExpr(m));
		}
	}

	private static int count(Group m) {
		int n = 1;
		for (int i = 0; i < m.groupCount(); i++) {
			n += count(m.group(i));
		}
		return n;
	}

	private static double evalExpr(Group match) {
		String pn = match.getPatternName();
		if ("num".equals(pn)) {
			return Double.parseDouble(match.substring());
		} else if ("expr".equals(pn)) {
			double d = evalExpr(match.group(0));
			for(int i=1;i+1<match.groupCount();i+=2) {
				String op = match.group(i).substring();
				if("+".equals(op))
					d += evalExpr(match.group(i+1));
				else
					d -= evalExpr(match.group(i+1));
			}
			return d;
		} else if ("mulexp".equals(pn)) {
			double d = evalExpr(match.group(0));
			for(int i=1;i+1<match.groupCount();i+=2) {
				String op = match.group(i).substring();
				if("*".equals(op))
					d *= evalExpr(match.group(i+1));
				else
					d /= evalExpr(match.group(i+1));
			}
			return d;
		} else if ("powexp".equals(pn)) {
			int n = match.groupCount();
			double d = evalExpr(match.group(n-1));
			for(int i=n-2;i>0;i-=2) {
				d = Math.pow(evalExpr(match.group(i-1)), d);
			}
			return d;
		}
		return evalExpr(match.group(0));
	}
}
