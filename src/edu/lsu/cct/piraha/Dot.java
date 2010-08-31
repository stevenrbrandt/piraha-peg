package edu.lsu.cct.piraha;

public class Dot extends Pattern {

	@Override
	public boolean match(Matcher m) {
		if(m.getTextPos() < m.text.length() && m.text.charAt(m.getTextPos()) != '\n') {
			m.incrTextPos(1);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String decompile() {
		return ".";
	}

	@Override
	public boolean eq(Object obj) {
		return true;
	}
}
