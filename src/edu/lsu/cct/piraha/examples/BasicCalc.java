package edu.lsu.cct.piraha.examples;

import edu.lsu.cct.piraha.Grammar;
import edu.lsu.cct.piraha.Group;
import edu.lsu.cct.piraha.Matcher;

public class BasicCalc {
	public static Grammar makeMath() {
		Grammar math = new Grammar();
		// All the various was a double precision number (or integer) can be
		// written
		math.compile("num","-?[0-9]+");

		// The basic operators
		math.compile("addop", "\\+|-");

		math.compile("addexpr", "{num}({addop}{num})*");
		
		return math;
	}
	
	Grammar math = makeMath();

	public static void main(String[] args) {
		Grammar math = makeMath();

		Matcher m = math.matcher("addexpr", "3+4-2*(9-4)");
		
		boolean b = m.matches();
		System.out.println("match? " + b);
		if (b) {
			m.dumpMatches();
			System.out.println("eval=" + evalExpr(m));
		} else {
			System.out.println(m.near()); // report errors
		}
	}
	
	private static double evalExpr(Group match) {
		double answer = Double.parseDouble(match.group(0).substring());
		for(int i=1;i<match.groupCount();i+=2) {
			String op = match.group(i).toString();
			double addend = Double.parseDouble(match.group(i+1).substring());
			if("+".equals(op))
				answer += addend;
			else
				answer -= addend;
		}
		return answer;
	}
}