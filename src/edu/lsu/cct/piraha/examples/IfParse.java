package edu.lsu.cct.piraha.examples;

import edu.lsu.cct.piraha.Grammar;
import edu.lsu.cct.piraha.Group;
import edu.lsu.cct.piraha.Matcher;

public class IfParse {

	public static boolean parse(Cpp cpp, String text) {
		Matcher m = cpp.g.matcher("if_expr", text);
		if(m.matches()) {
			return parseMatch(cpp,m).getBool();
		} else {
			throw new Error("["+text+"]: "+m.near().toString());
		}
	}
	private static Value parseMatch(Cpp cpp,Group g) {
		if(g.groupCount() > 0 && g.getPatternName().endsWith("_expr")) {
			Group g2 = g.group(0);//g.groupCount()-1);
			Value first = parseMatch(cpp,g2);
			//for(int i=g.groupCount()-3;i>=0;i-=2) {
			for(int i=0;i+2<g.groupCount();i+=2) {
				//Value first = parseMatch(cpp,g.group(i));
				Group op = g.group(i+1);
				if(op.substring().equals("&&") && !first.getBool())
					return first;
				if(op.substring().equals("||") && first.getBool())
					return first;
				Value second = parseMatch(cpp,g.group(i+2));
				first = parsePair(cpp,first,op,second,g);
			}
			return first;
		} else if(g.getPatternName().equals("num")) {
			String n = g.substring();
			if(n.endsWith("L")) {
				return new Value(Long.parseLong(n.substring(0,n.length()-1)),g);
			} else {
				return new Value(Long.parseLong(n),g);
			}
		} else if(g.getPatternName().equals("isdefined")) {
			if(cpp.defines.containsKey(g.group(0).substring()))
				return new Value(Boolean.TRUE,g);
			return new Value(Boolean.FALSE,g);
		} else if(g.getPatternName().equals("true")) {
			return new Value(Boolean.TRUE,g);
		} else if(g.getPatternName().equals("false")) {
			return new Value(Boolean.FALSE,g);
		} else if(g.getPatternName().equals("not")) {
			return new Value(!parseMatch(cpp, g.group(0)).getBool(), g);
		} else if(g.getPatternName().equals("if_expr")) {
			return new Value(!parseMatch(cpp, g.group(0)).getBool(), g);
		} else if(g.getPatternName().equals("word")) {
			return new Value(g);
		}
		throw new Error(g.getPatternName()+","+g.groupCount()+": "+g.near().toString());
	}
	private static Value parsePair(Cpp cpp,Value first,Group op,Value second,Group eqn) {
		String ops = op.substring();
		if(ops.equals("||")) {
			//System.out.println("first="+first+" "+op+" second="+second);
			return new Value(first.getBool() || second.getBool(),eqn);
		} else if(ops.equals("&&")) {
			return new Value(first.getBool() && second.getBool(),eqn);
		} else if(ops.equals("+")) {
			if(longMath(first,second)) {
				return new Value(first.getLong() + second.getLong(),eqn);
			} else {
				return new Value(first.getDouble() + second.getDouble(),eqn);
			}
		} else if(ops.equals("-")) {
			if(longMath(first,second)) {
				return new Value(first.getLong() - second.getLong(),eqn);
			} else {
				return new Value(first.getDouble() - second.getDouble(),eqn);
			}
		} else if(ops.equals("*")) {
			if(longMath(first,second)) {
				return new Value(first.getLong() * second.getLong(),eqn);
			} else {
				return new Value(first.getDouble() * second.getDouble(),eqn);
			}
		} else if(ops.equals("/")) {
			if(longMath(first,second)) {
				return new Value(first.getLong() / second.getLong(),eqn);
			} else {
				return new Value(first.getDouble() / second.getDouble(),eqn);
			}
		} else if(ops.equals("<=")) {
			if(longMath(first,second)) {
				return new Value(first.getLong() <= second.getLong(),eqn);
			} else {
				return new Value(first.getDouble() <= second.getDouble(),eqn);
			}
		} else if(ops.equals(">=")) {
			if(longMath(first,second)) {
				return new Value(first.getLong() >= second.getLong(),eqn);
			} else {
				return new Value(first.getDouble() >= second.getDouble(),eqn);
			}
		} else if(ops.equals("==")) {
			if(longMath(first,second)) {
				return new Value(first.getLong() == second.getLong(),eqn);
			} else {
				return new Value(first.getDouble() == second.getDouble(),eqn);
			}
		} else if(ops.equals("!=")) {
			if(longMath(first,second)) {
				return new Value(first.getLong() != second.getLong(),eqn);
			} else {
				return new Value(first.getDouble() != second.getDouble(),eqn);
			}
		} else if(ops.equals("<")) {
			if(longMath(first,second)) {
				return new Value(first.getLong() < second.getLong(),eqn);
			} else {
				return new Value(first.getDouble() < second.getDouble(),eqn);
			}
		} else if(ops.equals(">")) {
			if(longMath(first,second)) {
				return new Value(first.getLong() > second.getLong(),eqn);
			} else {
				return new Value(first.getDouble() > second.getDouble(),eqn);
			}
		} else
			throw new Error("op error "+op+": "+op.near());
	}
	
	private static boolean longMath(Value v1,Value v2) {
		return v1.isInt() && v2.isInt();
	}

	public static void buildGrammar(Grammar g) {
		g.compile("isdefined","defined( +{word}| *\\( *{word} *\\))");
		g.compile("not","! *{if_expr}");
		addOps(g,"if_expr","{num}|{isdefined}|{word}|\\({-if_expr}\\)|{not}",
				new String[][]{
					new String[]{"bool","\\|\\||\\&\\&"},
					new String[]{"comp","<=|>=|==|!=|>|<"},
					new String[]{"add","\\+|\\-"},
					new String[]{"mul","\\*|/"},
					});
	}

	private static void addOps(Grammar g, String name, String peg, String[][] ops) {
		String prev_name = name;
		for(int i=0;i<ops.length;i++) {
			String op_name = ops[i][0];
			String op_pat = ops[i][1];
			String pat_name = op_name+"_expr";
			String pat_op_name = op_name+"_op";
			String pat_expr = " *{"+pat_name+"}( *{"+pat_op_name+"} *{"+pat_name+"})*";
			g.compile(prev_name,pat_expr);
			g.compile(pat_op_name,op_pat);
			prev_name = pat_name;
		}
		g.compile(prev_name,peg);
	}
}
