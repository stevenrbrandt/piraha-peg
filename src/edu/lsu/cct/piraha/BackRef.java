package edu.lsu.cct.piraha;

public class BackRef extends Pattern {
	final int num;
	final boolean ignCase;
	
	public BackRef(int num,boolean ignCase) {
		this.num = num-1;
		this.ignCase = ignCase;
	}

	@Override
	public boolean match(Matcher m) {
		//System.out.println("num="+num+" mc="+m.matchCount());
		if(num >= m.groupCount())
			return false;
		Group backRef = m.group(num);
		int begin = backRef.getBegin();
		int end = backRef.getEnd();
		int n = end-begin;
		int pos = m.getTextPos();
		if(pos + n > m.text.length())
			return false;
		if (ignCase) {
			for (int i = 0; i < n; i++) {
				int nn = pos + i;
				char c1 = Character.toLowerCase(m.text.charAt(i+begin));
				char c2 = Character.toLowerCase(m.text.charAt(nn));
				//System.out.println("ign: "+c1+" <=> "+c2);
				if (c1 != c2) {
					return false;
				}
			}
		} else {
			for (int i = 0; i < n; i++) {
				int nn = pos + i;
				char c1 = m.text.charAt(i+begin);
				char c2 = m.text.charAt(nn);
				//System.out.println("!ign: "+c1+" <=> "+c2);
				if (c1 != c2) {
					return false;
				}
			}
		}
		m.setTextPos(pos+n);
		return true;
	}

	@Override
	public String decompile() {
		return "\\"+num;
	}

	@Override
	public boolean eq(Object obj) {
		BackRef ref = (BackRef)obj;
		return ref.num == num;
	}
}
