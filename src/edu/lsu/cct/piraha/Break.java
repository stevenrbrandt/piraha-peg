package edu.lsu.cct.piraha;

public class Break extends Pattern {

	@Override
	public boolean match(Matcher m) {
		throw new BreakException();
	}

	@Override
	public String decompile() {
		return "{brk}";
	}

	@Override
	public boolean eq(Object obj) {
		return obj instanceof Break;
	}

}
