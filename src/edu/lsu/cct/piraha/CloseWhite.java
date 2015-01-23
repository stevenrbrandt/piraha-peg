package edu.lsu.cct.piraha;

import edu.lsu.cct.util.Here;


public class CloseWhite extends Pattern {

	@Override
	public boolean match(Matcher m) {
		int pos = m.getTextPos();
		if(pos==0 || m.text.charAt(pos-1)=='\n') {
			int thresh = 0;
			int nt = m.whiteThresholds.size();
			if(nt > 0) {
				thresh = m.whiteThresholds.get(nt-1);
			}
			int newPos = pos;
			for(;newPos<m.text.length();newPos++) {
				char c = m.text.charAt(newPos);
				if(c == ' '||c == '\t') {
					;
				} else {
					break;
				}
			}
			if(newPos-pos <= thresh) {
				int n = m.whiteThresholds.remove(nt-1);
				if(nt == 1)
					m.white.setLength(0);
				else
					m.white.setLength(m.whiteThresholds.get(nt-2));
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public String decompile() {
		return "{CloseWhite}";
	}

	@Override
	public boolean eq(Object obj) {
		return obj instanceof CloseWhite;
	}

}
