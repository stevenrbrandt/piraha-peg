package edu.lsu.cct.piraha;

import java.util.Map;

class ExtraVisitor extends Visitor {
	final Map<String, Boolean> visited;

	ExtraVisitor(Map<String, Boolean> visited) {
		this.visited = visited;
	}

	public void finishVisit(Pattern p) {
		if(p instanceof Lookup) {
			Lookup ll = (Lookup)p;
			String name = ll.lookup;
			if(!visited.containsKey(name))
				visited.put(name,Boolean.FALSE);
		}
	}
}