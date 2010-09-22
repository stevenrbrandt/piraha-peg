package edu.lsu.cct.piraha.examples;

import edu.lsu.cct.piraha.DebugOutput;
import edu.lsu.cct.piraha.DebugVisitor;
import edu.lsu.cct.piraha.Grammar;
import edu.lsu.cct.piraha.Matcher;
import edu.lsu.cct.piraha.ParseException;
import edu.lsu.cct.piraha.Pattern;

/**
 * Just a handful of basic tests
 * @author sbrandt
 *
 */
public class Test {

	
	public static void test(String pat, String text, int matchsize) {
		Grammar g = new Grammar();
		Pattern p = g.compile("d", pat);//"a(0|1|2|3|4|5|6a|7|8|9)*x");
		String pdc = p.decompile();
		Pattern pe;
		DebugVisitor dv = new DebugVisitor();
		try {
			pe = g.compile("e",pdc);
		} catch (ParseException e) {
			p.visit(dv);
			DebugOutput.out.println("pat="+pat);
			DebugOutput.out.println("pdc="+pdc);
			throw new RuntimeException("parser decompile error "+pat+" != "+pdc,e);
		}
		if(!pe.equals(p)) {
			p.visit(dv);
			pe.visit(dv);
			throw new RuntimeException("decompile error "+pat+" != "+pe.decompile());
		}
		Matcher m = g.matcher("d",text);//"a6ax");
		//m.getPattern().diag();
		m.getPattern().visit(new DebugVisitor(DebugOutput.out));
		boolean found = m.matches();
		DebugOutput.out.print("match "+text+" using "+pat+" => "+found+" ");
		if(found) {
			DebugOutput.out.print("["+m.getBegin()+","+m.getEnd()+"]");
			m.dumpMatchesXML();
		}
		DebugOutput.out.println();
		DebugOutput.out.println();
		if(found) assert(matchsize == m.getEnd() - m.getBegin());
		else      assert(matchsize == -1);
	}
	
	static boolean assertOn = false;
	
	static public boolean turnAssertOn() {
		assertOn = true;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		assert(turnAssertOn());
		if(!assertOn)
			throw new RuntimeException("Assertions are not enabled");
		
		test("(\\[([^\\]])*)+","[",1);
		test("a(0|1|2|3|4|5|6a|7|8|9)*x","a6a72x",6);
		test("a(0|1|2|3|4|5|6a|7|8|9)*x","a6a72",-1);
		test("<{d}>|x","<<x>>",5);
		test("<{d}>|x","<<x>",-1); // this one should fail, no balancing >
		test("[a-z0-9]+","abcd0123",8);
		test("(foo|)","bar",0);
		test("(?i:foo)","bar",-1); // should fail
		test("[^\n]+","foo",3);
		test("[\u0000-\uffff]*","foo",3);
		test("(?=foo)foo","foo",3);
		test("(?!foo)foo","foo",-1);
		test("(?i:foo)","FOO",3);
		test("(?-i:foo)","foo",3);
		test("[^]+","foobar",6);
		test("[^](\\b|{d})","foo.bar",3);
		test("(?i:[a-z]+)","FOO",3);
		test("(?i:[fo]+)","FOO",3);
		test("\\t","\t",1);
		test("\t","\t",1);
		test("[a-c]+","bbca",4);
		test("(aaa|aa)aa$","aaaa",-1);
		test("a*a","aaaa",-1);
		test("\\b((?!apple\\b)[a-z]+)\\b","grape",5);
		test("(?!foo)","foo",-1);
		test("(?=foo)","foo",0);
		test("(?=[^\\.]*\\.)","abcd.",0);
		try {
			test("a**","a",1);
			assert(false);
		} catch (ParseException e) {
			;
		}

		Grammar g = new Grammar();
		g.compile("x", "(?i:[xyz])");
		g.compile("y","(?i:{x}a\\1)");
		Matcher m = g.matcher("y", "Xax");
		assert(m.find());
		
		// Check name matches
		g = new Grammar();
		g.compile("n", "[^<>]+");
		g.compile("t", "<{n}>(;<{n}>)*(,<{$n}>)*");
		g.diag(DebugOutput.out);
		m = g.matcher("t", "<a>;<bc>;<def>,<bc>,<bc>,<b>");
		assert(m.matches() && m.substring().equals("<a>;<bc>;<def>,<bc>,<bc>"));
		
		Grammar xml = new Grammar();
		xml.compileFile(Test.class.getResourceAsStream("xml.peg"));
		m = xml.matcher("tag","<a><b><c/></b></a>");
		assert(m.matches());
		
		// Test composabality
		g = new Grammar();
		g.importGrammar("math", Calc.makeMath());
		g.compile("vector", "{math:expr}(,{math:expr}){0,}");
		m = g.matcher("vector","1+2,(8+3)*9-4,4,9+7");
		assert(m.matches());
		
		test("[\\a-c]+","abab",4);
		test("[a-\\c]+","abab",4);
		test("[\\a-\\c]+","acb",3);
		test("[a-]+","a-a-",4);
		test("[\\a-]+","a-a-",4);
		test("[\\a\\-]+","a-a-",4);
		test("[a\\-]+","a-a-",4);
		test("[-a]+","a-a-",4);
		test("(\\[(\\\\[^]|[^\\]\\\\])*\\]|\\\\[^]|[^ \t\r\n\b])+","xxx",3);
		//test("[^ \t\r\n\b]+","abc",3);
		
		g = new Grammar();
		g.compile("import", "import");
		g.compile("pat","((?!a|{import}_).)+");
		m = g.matcher("pat","foo_import_bar");
		m.find();
		System.out.println(m.substring());
	}
}
