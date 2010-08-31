package edu.lsu.cct.piraha;

public class Nothing extends Pattern {

	@Override
	public boolean match(Matcher m) {
		return true;
	}

	@Override
	public String decompile() {
		return "";
	}

	@Override
	public boolean eq(Object obj) {
		return true;
	}
}
