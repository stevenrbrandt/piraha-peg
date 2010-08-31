package edu.lsu.cct.piraha;

public class ILiteral extends Pattern {
	char lch,uch;
	public ILiteral(int val) {
		char ch = (char)val;
		this.lch = Character.toLowerCase(ch);
		this.uch = Character.toUpperCase(ch);
	}
	@Override
	public boolean match(Matcher m) {
		if(m.getTextPos() < m.text.length()) {
			char ch = m.text.charAt(m.getTextPos());
			if(lch == ch || uch == ch) {
				m.incrTextPos(1);
				return true;
			}
		}
		return false;
	}
	@Override
	public String decompile() {
		return Literal.outc(lch);
	}
	@Override
	public boolean eq(Object obj) {
		ILiteral ilit = (ILiteral)obj;
		return ilit.lch == lch && ilit.uch == uch;
	}
}
