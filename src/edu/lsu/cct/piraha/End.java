package edu.lsu.cct.piraha;


public class End extends Pattern {

	@Override
	public boolean match(Matcher m) {
		return m.getTextPos() == m.text.length();
	}

	@Override
	public String decompile() {
		return "$";
	}

	@Override
	public boolean eq(Object obj) {
		return true;
	}
}
