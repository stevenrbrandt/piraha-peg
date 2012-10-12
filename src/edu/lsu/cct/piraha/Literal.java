package edu.lsu.cct.piraha;

import java.util.ArrayList;
import java.util.List;

public class Literal extends Pattern {
	final static String SPECIAL = "*{}[]+?()^$|.\\";
	char ch;
	public Literal(int val) {
		this.ch = (char) val;
	}
	@Override
	public boolean match(Matcher m) {
		if(m.getTextPos() < m.text.length() && m.text.charAt(m.getTextPos()) == ch) {
			//System.out.println("true");
			m.incrTextPos(1);
			return true;
		} else {
			m.expected(new Expected(this));
			//System.out.println("false");
			return false;
		}
	}
	
	public static String outc(char c) {
		if(SPECIAL.indexOf(c) >= 0)
			return "\\"+c;
		else
			return DebugOutput.outc(c);
	}
	
	@Override
	public String decompile() {
		return outc(ch);
	}
	@Override
	public boolean eq(Object obj) {
		Literal lit = (Literal)obj;
		return lit.ch == ch;
	}
	
	@Override
	public List<String> expected(int n) {
		List<String> ex = new ArrayList<String>();
		ex.add(decompile());
		return ex;
	}
}
