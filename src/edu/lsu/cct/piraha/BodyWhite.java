package edu.lsu.cct.piraha;

//import edu.lsu.cct.util.Here;

public class BodyWhite extends Pattern {

	@Override
	public boolean match(Matcher m) {
		int pos = m.getTextPos();
		if(pos==0 || m.text.charAt(pos-1)=='\n') {
			int newPos = pos;
			for(;newPos<m.text.length();newPos++) {
				char c = m.text.charAt(newPos);
				if(c == ' '||c == '\t') {
					int n = newPos - pos;
					if(n < m.white.length()) {
						if(c != m.white.charAt(n)) {
							return false;
						}
						if(n+1 == m.white.length()) {
							m.setTextPos(newPos+1);
							return true;
						}
					} else {
						return false;
					}
				} else {
					return false;
				}
			}
		}
		return false;
	}

	@Override
	public String decompile() {
		return "{BodyWhite}";
	}

	@Override
	public boolean eq(Object obj) {
		return obj instanceof BodyWhite;
	}

}
