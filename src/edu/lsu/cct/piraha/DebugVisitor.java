package edu.lsu.cct.piraha;

public class DebugVisitor extends Visitor {
	DebugOutput out;

	public DebugVisitor(DebugOutput out) {
		this.out = out;
	}
	public DebugVisitor() {
		this.out = DebugOutput.out;
	}
	
	private void printChar(char c) {
		if(c == ' ')
			out.print("[ ]");
		else
			out.print(c);
	}
	
	@Override
	public Visitor startVisit(Pattern p) {
		String cn = p.getClass().getName();
		int n = cn.lastIndexOf('.');
		cn = cn.substring(n+1);
		out.print(cn);
		out.print(": ");
		out.indent += 2;
		if(p instanceof Literal) {
			printChar(((Literal)p).ch);
		} else if(p instanceof ILiteral) {
			printChar(((ILiteral)p).lch);
		} else if(p instanceof Name) {
			out.print(((Name)p).name);
		} else if(p instanceof Range) {
			Range r = (Range)p;
			if(r.lo == r.hi)
				printChar(r.lo);
			else {
				printChar(r.lo);
				out.print(" to ");
				printChar(r.hi);
			}
		} else if(p instanceof Multi) {
			Multi m = (Multi)p;
			if(m.min == m.max)
				out.print(m.min);
			else
				out.print(m.min+" to "+m.max);
		} else if(p instanceof Lookup) {
			out.print(((Lookup)p).lookup);
		} else if(p instanceof Bracket) {
			out.print("neg="+((Bracket)p).neg);
		}
		out.println();
		return this;
	}
	@Override
	public void finishVisit(Pattern p) {
		out.indent -= 2;
	}
}
