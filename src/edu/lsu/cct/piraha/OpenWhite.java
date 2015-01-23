package edu.lsu.cct.piraha;

public class OpenWhite extends Pattern {

	@Override
	public boolean match(Matcher m) {
		int pos = m.getTextPos();
		if(pos==0 || m.text.charAt(pos-1)=='\n') {
			int newPos = pos;
			boolean newWhite = false;
			StringBuilder saveWhite = new StringBuilder();
			for(;newPos<m.text.length();newPos++) {
				char c = m.text.charAt(newPos);
				if(c == ' '||c == '\t') {
					int n = newPos - pos;
					if(n < m.white.length()) {
						if(c != m.white.charAt(n)) {
							return false;
						}
					} else {
						saveWhite.append(c);
						newWhite = true;
					}
				} else {
					break;
				}
			}
			if(newWhite) {
				m.whiteThresholds.add(newPos-pos);
				m.white.append(saveWhite);
			}
			return newWhite;
		} else {
			return false;
		}
	}

	@Override
	public String decompile() {
		return "{OpenWhite}";
	}

	@Override
	public boolean eq(Object obj) {
		return obj instanceof OpenWhite;
	}

}
