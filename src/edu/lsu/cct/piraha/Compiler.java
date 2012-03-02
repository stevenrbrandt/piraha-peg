package edu.lsu.cct.piraha;

import java.util.ArrayList;
import java.util.List;

class Compiler {
	/**
	 * 
	 */
	private final Grammar grammar;
	int i;
	String pattern;
	final String patternName;
	Or or = null;
	
	public Pattern getPattern(boolean ignCase,boolean igcShow) {
		if(or != null) {
			or.patterns.add(mkSeq(patternList,ignCase,igcShow));
			patternList = null;
			return or;
		} else
			return mkSeq(patternList,ignCase,igcShow);
	}
	
	List<Pattern> patternList = new ArrayList<Pattern>();
	Compiler(Grammar grammar, String name,String pattern) {
		this.grammar = grammar;
		this.patternName = name;
		this.pattern = pattern;
		this.i = 0;
		while(next((char)0,false,false))
			;
	}
	Compiler(Grammar grammar, String name,String pattern,int i) {
		this.grammar = grammar;
		this.patternName = name;
		this.pattern = pattern;
		this.i = i;
		compile((char)0,false,false);
	}
	void compile(char term,boolean ignCase,boolean igcShow) {
		this.grammar.checked = false;
		while(next(term,ignCase,igcShow))
			;
	}
	boolean next(char term,boolean ignCase,boolean igcShow) {
		if(i >= pattern.length()) {
			if(term != 0)
				throw new ParseException(""+term+" not found in "+patternName+":"+pattern);
			return false;
		}
		char c = pattern.charAt(i++);
		if(c == '\\') {
			char cfinal = processEscape();//c2);
			if(cfinal == '\b')
				patternList.add(new Boundary());
			else if(cfinal >= '1' && cfinal <= '9')
				patternList.add(new BackRef(cfinal-'0',ignCase));
			else
				patternList.add(makeLiteral(cfinal,ignCase));
		} else if(c == '{') {
			StringBuffer sb1 = new StringBuffer();
			StringBuffer sb2 = new StringBuffer();
			boolean num = false, comma = false, other = false;
			for(;i < pattern.length();i++) {
				char c2 = pattern.charAt(i);
				if(c2 == '}') {
					i++;
					break;
				} else if(c2 == ',' && !comma) {
					comma = true;
				} else if(c2 >= '0' && c2 <= '9') {
					num = true;
					if(comma)
						sb2.append(c2);
					else
						sb1.append(c2);
				} else {
					other = true;
					num = true;
					if(comma)
						sb2.append(c2);
					else
						sb1.append(c2);
				}
			}
			if(num && !comma && !other) {
				int n = sb1.length()==0 ? 0 : Integer.parseInt(sb1.toString());
				Multi multi = new Multi(n,n);
				multi.pattern = patternList.remove(patternList.size()-1);
				patternList.add(multi);
			} else if(num && comma && !other) {
				int n1 = parseInt(sb1,0);//sb1.length()==0 ? 0 : Integer.parseInt(sb1.toString());
				int n2 = parseInt(sb2,Integer.MAX_VALUE);//sb2.length()==0 ? Integer.MAX_VALUE : Integer.parseInt(sb2.toString());
				if(n1 > n2)
					throw new ParseException(""+n1+" > "+n2+" in pattern "+patternName);
				Multi multi = new Multi(n1,n2);
				multi.pattern = patternList.remove(patternList.size()-1);
				patternList.add(multi);
			} else {
				String s = sb1.toString();
				if(comma)
					s += ',' + sb2.toString();
				if(s.startsWith("$"))
					patternList.add(new Name(s.substring(1)));
				else if(s.equals("brk"))
					patternList.add(new Break());
				else
					patternList.add(new Lookup(s,this.grammar));
			}
		} else if(c == '[') {
			// Very simple bracket rule
			// Ranges can only be for un-escaped characters
			Bracket b = new Bracket();
			if(i<pattern.length() && pattern.charAt(i)=='^') {
				b.neg = true;
				i++;
			}
			char c1 = processChar();
			while(true) {
				if(c1 == ']' && !escaped) {
					break;
				}
				if(endOfPattern) {
					throw new ParseException("] not found in "+pattern);
				}
				char c2 = processChar();
				boolean c2esc = escaped;
				if(c2 == '-' && !escaped) {
					char c3 = processChar();
					if(c3 == ']' && !escaped) {
						char c1l = Character.toLowerCase(c1);
						char c1u = Character.toUpperCase(c1);
						if(ignCase && c1l != c1u) {
							b.addRange(c1l,c1l);
							b.addRange(c1u,c1u);
						} else {
							b.addRange(c1,c1);
						}
						b.addRange('-','-');
						break;
					} else {
						char c1l = Character.toLowerCase(c1);
						char c1u = Character.toUpperCase(c1);
						char c3l = Character.toLowerCase(c3);
						char c3u = Character.toUpperCase(c3);
						if(ignCase && (c1l != c1u || c3l != c3u)) {
							b.addRange(c1l,c3l);
							b.addRange(c1u,c3u);
						} else {
							b.addRange(c1,c3);
						}
					}
					c1 = processChar();
				} else {
					char c1l = Character.toLowerCase(c1);
					char c1u = Character.toUpperCase(c1);
					if(ignCase && c1l != c1u) {
						b.addRange(c1l,c1l);
						b.addRange(c1u,c1u);
					} else {
						b.addRange(c1,c1);
					}
					c1 = c2;
					escaped = c2esc;
				}
			}
			patternList.add(b);
		} else if(c == '*') {
			Multi multi = new Multi(0,Integer.MAX_VALUE);
			multi.pattern = patternList.remove(patternList.size()-1);
			patternList.add(multi);
		} else if(c == '?') {
			Multi multi = new Multi(0,1);
			if(patternList.size()==0)
				throw new ParseException(pattern.substring(0,i)+"<-END in pattern "+patternName);
			multi.pattern = patternList.remove(patternList.size()-1);
			patternList.add(multi);
		} else if(c == '+') {
			Multi multi = new Multi(1,Integer.MAX_VALUE);
			multi.pattern = patternList.remove(patternList.size()-1);
			patternList.add(multi);
		} else if(c == '(') {
			Or orSave = or;
			List<Pattern> patternSave = patternList;
			or = null;
			patternList = new ArrayList<Pattern>();
			boolean lookAhead = false, negLookAhead = false, igcNext = ignCase, igcShowNext = false;
			if(i+1 < pattern.length() && pattern.charAt(i) == '?') {
				char c3 = pattern.charAt(i+1);
				if(c3 == '=') {
					lookAhead = true;
					i += 2;
				} else if(c3 == '!') {
					negLookAhead = true;
					i += 2;
				} else if(c3 == 'i' && i+2 < pattern.length() && pattern.charAt(i+2) == ':') {
					igcNext = true;
					i += 3;
					igcShowNext = true;
				} else if(c3 == '-' && i+3 < pattern.length() && pattern.charAt(i+2) == 'i' && pattern.charAt(i+3) == ':') {
					igcNext = false;
					i += 4;
					igcShowNext = true;
				}
			}
			compile(')',igcNext,igcShowNext);
			Pattern sub = getPattern(igcNext,igcShowNext);
			if(lookAhead)
				sub = new LookAhead(sub);
			else if(negLookAhead) 
				sub = new NegLookAhead(sub);
			patternSave.add(sub);
			or = orSave;
			patternList = patternSave;
		} else if(c == '.') {
			patternList.add(new Dot());
		} else if(c == '$') {
			patternList.add(new End());
		} else if(c == '^') {
			patternList.add(new Start());
		} else if(c == term && term != 0) {
			return false;
		} else if(c == '|') {
			if(or == null)
				or = new Or(ignCase,igcShow);
			or.patterns.add(mkSeq(patternList,ignCase,false));
			patternList = new ArrayList<Pattern>();
		} else if(c == ')') {
			throw new ParseException("Unexpected termination by ) in "+
					pattern.substring(0,i)+"<-END of "+c+" in pattern "+patternName);
		} else {
			patternList.add(makeLiteral(c,ignCase));
		}
		return true;
	}
	int parseInt(StringBuffer sb1,int def) {
		if(sb1.length()==0) return def;
		else return Integer.parseInt(sb1.toString());
	}
	public Pattern mkSeq(List<Pattern> patternList,boolean ignCase,boolean igcShow) {
		int n = patternList.size();
		if(n == 0)
			return new Nothing();
		else if(n == 1)
			return patternList.get(0);
		else
			return new Seq(patternList,ignCase,igcShow);
	}
	private Pattern makeLiteral(char c, boolean ignCase) {
		if(ignCase && Character.toLowerCase(c) != Character.toUpperCase(c))
			return new ILiteral(c);
		else
			return new Literal(c);
	}
	boolean escaped = false, endOfPattern = false;
	private char processChar() {
		char c = 0;
		escaped = false;
		if(i < pattern.length()) {
			c = pattern.charAt(i++);
			if(c == '\\') {
				escaped = true;
				c = processEscape();
			}
		} else {
			endOfPattern = true;
		}
		return c;
	}
	private char processEscape() {
		char cfinal = 0;
		char c2 = pattern.charAt(i);
		if(c2 == 'u' && i+5 < pattern.length()) {
			int val = 0;
			for(int j=1;j<5;j++) {
				char d = pattern.charAt(i+j);
				if(d >= '0' && d <= '0')
					val = 16*val+(d-'0');
				else if(d >= 'a' && d <= 'f')
					val = 16*val+(d-'a'+10);
				else if(d >= 'A' && d <= 'F')
					val = 16*val+(d-'A'+10);
				else
					throw new ParseException("Bad unicode sequence at position i="+i);
			}
			i += 5;
			cfinal = (char)val;
		} else if(c2 == 'n') {
			i ++;
			cfinal = '\n';
		} else if(c2 == 'r') {
			i ++;
			cfinal = '\r';
		} else if(c2 == 't') {
			i ++;
			cfinal = '\t';
		} else if(c2 == 'b') {
			i ++;
			cfinal = '\b';
		} else {
			i ++;
			cfinal = c2;
		}
		return cfinal;
	}
}
