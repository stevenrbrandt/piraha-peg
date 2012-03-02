package edu.lsu.cct.piraha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckVisitor extends Visitor {
	Map<String,Boolean> patterns;
	CheckVisitor child = null;
	List<Boolean> canBeZero = new ArrayList<Boolean>();
	String checking;
	boolean retry = true;
	public Map<String, Boolean> defaults;
	
	public CheckVisitor() {
		patterns = new HashMap<String,Boolean>();
	}
	private CheckVisitor(Map<String,Boolean> patterns) {
		this.patterns = patterns;
	}
	
	Boolean getDefault(String key) {
		if(defaults != null && defaults.containsKey(key)) {
			return defaults.get(key);
		}
		return Boolean.FALSE;
	}
	
	@Override
	public Visitor startVisit(Pattern p) {
		if(p instanceof Multi) {
			CheckVisitor cv = new CheckVisitor(patterns);
			child = cv;
			cv.checking = checking;
			return cv;
		} else if(p instanceof Or) {
			CheckVisitor cv = new CheckVisitor(patterns);
			child = cv;
			cv.checking = checking;
			return cv;
		} else if(p instanceof Seq) {
			CheckVisitor cv = new CheckVisitor(patterns);
			child = cv;
			cv.checking = checking;
			return cv;
		} else if(p instanceof Lookup) {
			CheckVisitor cv = new CheckVisitor(patterns);
			child = cv;
			Lookup ll = (Lookup)p;
			cv.checking = ll.lookup;
			return cv;
		}
		return this;
	}
	
	private boolean andZero() {
		for(Boolean b : child.canBeZero) {
			if(!b)
				return false;
		}
		return true;
	}
	
	private boolean orZero() {
		for(Boolean b : child.canBeZero) {
			if(b)
				return true;
		}
		return false;
	}
	
	@Override
	public void finishVisit(Pattern p) {
		if(p instanceof Multi) {
			Multi m = (Multi)p;
			if(m.max > 1 && andZero()) {
				System.out.println(child.canBeZero);
				System.out.println(patterns);
				//p.visit(new DebugVisitor());
				throw new ParseException(checking+
						": cannot have zero length pattern in quantifier: "+p.decompile());
			}
			if(m.min==0)
				canBeZero.add(Boolean.TRUE);
			else
				canBeZero.add(andZero());
		} else if(p instanceof Nothing) {
			canBeZero.add(Boolean.TRUE);
		} else if(p instanceof LookAhead) {
			canBeZero.add(Boolean.TRUE);
		} else if(p instanceof NegLookAhead) {
			canBeZero.add(Boolean.TRUE);
		} else if(p instanceof End) {
			canBeZero.add(Boolean.TRUE);
		} else if(p instanceof Start) {
			canBeZero.add(Boolean.TRUE);
		} else if(p instanceof Seq) {
			//p.visit(new DebugVisitor());
			canBeZero.add(andZero());
		} else if(p instanceof Or) {
			canBeZero.add(orZero());
		} else if(p instanceof Lookup) {
			Lookup l = (Lookup)p;
			if(patterns.containsKey(l.lookup)) {
				canBeZero.add(patterns.get(l.lookup));
			} else {
				Boolean defaultValue = getDefault(l.lookup);
				patterns.put(l.lookup,defaultValue);
				l.pattern.visit(child);
				boolean res = andZero();
				if(res) {
					patterns.put(l.lookup,Boolean.TRUE);
				}
				if(defaultValue ^ res)
					retry = true;
				canBeZero.add(res);
			}
		} else {
			canBeZero.add(Boolean.FALSE);
		}
	}
}
