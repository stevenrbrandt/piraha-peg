package edu.lsu.cct.piraha;

public class Start extends Pattern {

	@Override
	public boolean match(Matcher m) {
		return m.getTextPos() == 0;
	}

	@Override
	public String decompile() {
		return "^";
	}

	@Override
	public boolean eq(Object obj) {
		return true;
	}
}
