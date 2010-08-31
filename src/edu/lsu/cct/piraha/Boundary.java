package edu.lsu.cct.piraha;

public class Boundary extends Pattern {

	@Override
	public boolean match(Matcher m) {
		int n = m.getTextPos();
		if(n==0 || n==m.text.length()) {
			return true;
		}
		if(!identc(m.text.charAt(n)) || !identc(m.text.charAt(n-1))) {
			return true;
		}
		return false;
	}
	
	private boolean identc(char c) {
		if(c >= 'a' && c <= 'z')
			return true;
		if(c >= 'A' && c <= 'Z')
			return true;
		if(c >= '0' && c <= '9')
			return true;
		return c == '_';
	}

	@Override
	public String decompile() {
		return "\\b";
	}

	@Override
	public boolean eq(Object obj) {
		return true;
	}
}
