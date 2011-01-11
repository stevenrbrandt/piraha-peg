package edu.lsu.cct.piraha.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.lsu.cct.piraha.DebugVisitor;
import edu.lsu.cct.piraha.Grammar;
import edu.lsu.cct.piraha.Group;
import edu.lsu.cct.piraha.Matcher;

public class Kranc {
	Grammar g = new Grammar();
	DebugVisitor dv = new DebugVisitor();
	//private int calcNumber;
	private String thornName;
	private List<Group> inhers = new ArrayList<Group>();
	private List<Group> ikpars = new ArrayList<Group>();
	private List<Group> ekpars = new ArrayList<Group>();
	private List<Group> kpars  = new ArrayList<Group>();
	private List<Group> ipars  = new ArrayList<Group>();
	private List<Group> rpars  = new ArrayList<Group>();
	private List<Group> calcs  = new ArrayList<Group>();
	public Kranc() {
		g.compile("w0","([ \t\r\n]|#.*|\\(\\*((?!\\*\\))[^])*\\*\\))*");
		g.compile("w1","([ \t\r\n]|#.*|\\(\\*((?!\\*\\))[^])*\\*\\))+");
		g.compile("name","(?i:[a-z_][a-z0-9_:]*)");
		g.compile("dquote","\"(\\\\[^]|[^\\\\\"])*\"");
		g.compile("thorn","{-w0}@THORN{-w1}{name}");
		g.compile("args","({-w0}{expr}({-w0},{-w0}{expr})*|)");
		g.compile("fun","{name}{-w0}\\[{args}{-w0}\\]");
		g.compile("div","{fun}{-w0}->{-w0}{expr}");
		g.compile("deriv","DERIVATIVES{-w1}{div}({-w0},{-w0}{div})*{-w0}(,{-w0}|)@END_DERIVATIVES");
		g.compile("def","DEFINE{-w1}({fun}|{name}){-w0}={-w0}{expr}");
		g.compile("num","[0-9]+");
		g.compile("term","{fun}|{name}|{num}|{list}|{dquote}|\\({-w0}{@expr}\\)|[+-][ \t]*{-term}");
		g.compile("end_thorn","@END_THORN");
		g.compile("kranc", "{thorn}*({-w0}@({deriv}|{jac}|{tens}|{sym}|{conn}|{group}|{extra}|"+
				"{def}|{calc}|{inher}|{kpar}|{rpar}|{ipar}))*{-w0}{end_thorn}{-w0}$");
		g.addOps("expr", "{-term}", "{-w0}", new String[][]{
				new String[]{"comp","<=|>=|==|!=|<|>"},
				new String[]{"add","([+-]|<>)"},
				new String[]{"mul","[*/]|"},
				new String[]{"pow","\\^"},
		});
		g.compile("jac","JACOBIAN{-w0}\\{{-w0}{name}({-w0},{-w0}{name})*{-w0}\\}");
		g.compile("conn","CONNECTION{-w0}\\{{-w0}{name}({-w0},{-w0}{name})*{-w0}\\}");
		g.compile("tens","TENSORS{-w0}({list}|{fun}|{name})({-w0},{-w0}({list}|{fun}|{name}))*{-w0}(,{-w0}|)@END_TENSORS");
		g.compile("symexp","{fun}|\\{{-w0}{fun}({-w0},{-w0}{name})+\\}");
		g.compile("sym","SYMMETRIC{-w0}{symexp}({-w0},{-w0}{symexp})*");
		g.compile("group","GROUPS{-w0}(({fun}|{name}){-w0}->{-w0}{name}{-w0}(,{-w0}|))*@END_GROUPS");
		g.compile("extra_list","\\{{-w0}{name}{-w0}(,{-w0}{name}{-w0})*\\}");
		g.compile("extra","EXTRA_GROUPS{-w0}"+
				"({name}{-w0}->{-w0}{extra_list}{-w0}(,{-w0}|))*{-w0}@END_EXTRA_GROUPS");

		g.compile("list","\\{({@expr}({-w0},{-w0}{@expr})*|){-w0}\\}");
		g.compile("short","({fun}|{name}){-w0}");
                g.compile("shorts","@SHORTHANDS{-w1}{short}({-w0},{-w0}{short})*{-w0}(,{-w0}|)@END_SHORTHANDS");
		g.compile("eqn","({fun}|{name}){-w0}->{-w0}{expr}");
		g.compile("eqns","@EQUATIONS{-w1}{eqn}({-w0},{-w0}{eqn})*{-w0}(,{-w0}|)@END_EQUATIONS");
		g.compile("calc_par","@{name}{-w1}{expr}({-w0},{-w0}{expr})*");
		g.compile("calc","CALCULATION{-w1}{name}{-w0}"+
				"({shorts}{-w0}|{eqns}{-w0}|{calc_par}{-w0})*"+
				"@END_CALCULATION");
		g.compile("inher","INHERITED_IMPLEMENTATION{-w1}{name}");
		
		g.compile("ikpar","INHERITED_KEYWORD_PARAMETER{-w1}{name}{-w0}(@{name}{-w1}{expr}({-w0},{-w0}{expr})*{-w0})*@END_INHERITED_KEYWORD_PARAMETER");
		g.compile("ekpar","EXTENDED_KEYWORD_PARAMETER{-w1}{name}{-w0}(@{name}{-w1}{expr}({-w0},{-w0}{expr})*{-w0})*@END_EXTENDED_KEYWORD_PARAMETER");
		g.compile("kpar","KEYWORD_PARAMETER{-w1}{name}{-w0}(@{name}{-w1}{expr}({-w0},{-w0}{expr})*{-w0})*@END_KEYWORD_PARAMETER");
                g.compile("allowed","{dquote}{-w0}->{-w0}{dquote}");
		g.compile("alloweds","@ALLOWED_VALUES{-w0}({allowed}({-w0},{-w0}{allowed})*{-w0})*({-w0},{-w0})*@END_ALLOWED_VALUES");
		g.compile("ipar","INT_PARAMETER{-w1}{name}{-w0}({alloweds}{-w0}|{calc_par}{-w0})*@END_INT_PARAMETER");
		g.compile("rpar","REAL_PARAMETER{-w1}{name}{-w0}(@{name}{-w1}{expr}({-w0},{-w0}{expr})*{-w0})*@END_REAL_PARAMETER");
		//g.diag(DebugOutput.out);
	}
	public void doFile(String inputfile, String outputfile) throws IOException {
		File f = new File(inputfile);
		String c = Grammar.readContents(f);
		Matcher m = g.matcher("kranc",c);
		boolean b = m.matches();
		//trim(m).dumpMatches();
		//m.dumpMatches();
		System.out.println(b);
		if(!b)
			System.err.println(m.near().toString());
		formatOutput(outputfile,m);
	}
	public void formatOutput(String file,Group g) throws IOException {
		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter pw = new PrintWriter(bw);
		pw.println("$Path = Join[$Path, {\"../../../repos/Kranc/Tools/CodeGen\",");
		pw.println("                     \"../../../repos/Kranc/Tools/MathematicaMisc\"}];");
		pw.println("Get[\"KrancThorn`\"];");
		pw.println("SetEnhancedTimes[False];");
		pw.println("SetSourceLanguage[\"C\"];");
		pw.println();
		pw.println("evolutionTimelevels = 3;");
		formatOutput(pw,g);
		pw.flush();
	}
	public void formatOutput(PrintWriter pw,Group g) {
		String m = g.getPatternName();
		if("kranc".equals(m)) {
			for(int i=0;i<g.groupCount();i++)
				formatOutput(pw,g.group(i));
		} else if("thorn".equals(m)) {
			thornName = g.group(0).substring();
		} else if("deriv".equals(m)) {
                        pw.println();
			pw.print("partialDerivatives = {");
			for(int i=0;i<g.groupCount();i++) {
				if(i==0) pw.println();
				else pw.println(",");
				pw.print("  ");
				pw.print(g.group(i).substring());
			}
			pw.println();
			pw.println("};");
		} else if("jac".equals(m)) {
			pw.println("ResetJacobians;");
			pw.print("DefineJacobian[");
			for(int i=0;i<g.groupCount();i++) {
				if(i > 0) pw.print(", ");
				pw.print(g.group(i).substring());
			}
			pw.println("];");
		} else if("tens".equals(m)) {
			Set<String> tensors = new HashSet<String>();
			for(int i=0;i<g.groupCount();i++) {
				String name = null;
				String mm = g.group(i).getPatternName();
				if("name".equals(mm)) {
					name = g.group(i).substring();
					declareTensor(pw, g.group(i), tensors, name);
				} else if("list".equals(mm)) {
					String n2 = g.group(i).group(0).getPatternName();
					if(!"fun".equals(n2))
						throw new Error("syntax error "+n2+" "+g.group(i).group(0).near());
					String n3 = g.group(i).group(0).group(0).getPatternName();
					if(!"name".equals(n3))
						throw new Error("syntax error "+n2+" "+g.group(i).group(0).group(0).near());
					name = g.group(i).group(0).group(0).substring();
					declareTensor(pw, g.group(i).group(0), tensors, name);
					Group gg = g.group(i);
					if(gg.groupCount()>1 && "list".equals(gg.group(1).getPatternName())) {
						for(int j=1;j<gg.groupCount();j++) {
							pw.print("AssertSymmetricIncreasing[");
							pw.print(gg.group(0).substring());
							for(int k=0;k<gg.group(j).groupCount();k++) {
								pw.print(", ");
								pw.print(gg.group(j).group(k).substring());
							}
							pw.println("];");
						}
					} else {
						pw.print("AssertSymmetricIncreasing[");
						for(int j=0;j<gg.groupCount();j++) {
							if(j > 0) pw.print(", ");
							pw.print(gg.group(j).substring());
						}
						pw.println("];");
					}
				} else if("fun".equals(mm)) {
					name = g.group(i).group(0).substring();
					declareTensor(pw, g.group(i), tensors, name);
					pw.print("AssertSymmetricIncreasing[");
					pw.print(g.group(i).substring());
					pw.println("];");
				}
			}
		} else if("conn".equals(m)) {
			pw.print("DefineConnection[");
			for(int i=0;i<g.groupCount();i++) {
				if(i > 0) pw.print(", ");
				pw.print(g.group(i).substring());
			}
			pw.println("];");
		} else if("group".equals(m)) {
                        pw.println();
			pw.print("declaredGroups = {");
			for(int i=0;i+1<g.groupCount();i+=2) {
				if(i==0) pw.println();
				else pw.println(",");
				pw.print("  SetGroupName[CreateGroupFromTensor[");
				pw.print(g.group(i).substring());
				pw.print("], \"");
				pw.print(g.group(i+1).substring());
				pw.print("\"]");
			}
			pw.println();
			pw.println("};");
			pw.println("declaredGroupNames = Map[First, declaredGroups];");
                        pw.println("allGroups = declaredGroups;");
		} else if("extra".equals(m)) {
                        pw.println();
			pw.print("extraGroups = {");
			for(int i=0;i+1<g.groupCount();i+=2) {
				if(i==0) pw.println();
				else pw.println(",");
				pw.print("  {\"");
				pw.print(g.group(i).substring());
				pw.print("\", ");
				pw.print(g.group(i+1).substring());
				pw.print("}");
			}
			pw.println();
			pw.println("};");
			pw.println("allGroups = Join[declaredGroups, extraGroups];");
		} else if("def".equals(m)) {
                        pw.println();
			pw.print(g.group(0).substring());
			pw.print(" = ");
			pw.print(g.group(1).substring());
			pw.println(";");
		/*} else if("calcs".equals(m)) {
			for(int i=0;i<g.groupCount();i++) {
				calcNumber = i;
				formatOutput(pw, g.group(i));
			}
			pw.print("calculations = {");
			for(int i=0;i<g.groupCount();i++) {
				if(i > 0) pw.print(", ");
				pw.print("calc");
				pw.print(i);
			}
			pw.println("};");*/
		} else if("calc".equals(m)) {
			/*pw.print("calc");
			pw.print(calcNumber);
			pw.println(" =");
			pw.println("{");
			for(int i=1;i<g.groupCount();i++) {
				String mm = g.group(i).getPatternName();
				if("calc_par".equals(mm)) {
					pw.print("  ");
					pw.print(g.group(i).group(0).substring());
					pw.print(" -> ");
					pw.print(g.group(i).group(1).substring());
					pw.println(",");
				} else if("eqns".equals(mm)) {
					formatOutput(pw, g.group(i));
				} else {
					throw new Error("bad calc near "+g.group(i).near());
				}
			}
			pw.print("  Name -> \"");
			pw.print(thornName);
			pw.print("_\" <> ");
			pw.print(g.group(0).substring());
			pw.println();
			pw.println("};");*/
			calcs.add(g);
		} else if("shorts".equals(m)) {
			pw.println("  Shorthands -> {");
			for(int i=0;i<g.groupCount();i++) {
				if(i>0) pw.println(",");
				pw.print("    ");
				pw.print(g.group(i).substring());
			}
                        pw.println();
			pw.println("  },");
		} else if("eqns".equals(m)) {
			pw.println("  Equations -> {");
			for(int i=0;i<g.groupCount();i++) {
				if(i>0) pw.println(",");
				pw.print("    ");
				pw.print(g.group(i).group(0).substring());
				pw.print(" -> ");
				pw.print(g.group(i).group(1).substring());
			}
			pw.println();
			pw.println("  },");
		} else if("inher".equals(m)) {
                        inhers.add(g);
		} else if("ikpar".equals(m)) {
			ikpars.add(g);
		} else if("ekpar".equals(m)) {
			ekpars.add(g);
		} else if("kpar".equals(m)) {
			kpars .add(g);
		} else if("ipar".equals(m)) {
                        ipars .add(g);
		} else if("rpar".equals(m)) {
			rpars .add(g);
                } else if("alloweds".equals(m)) {
			pw.println("    AllowedValues -> {");
			for(int i=0;i<g.groupCount();i++) {
				if(i>0) pw.println(",");
				pw.print("      { Value -> ");
				pw.print(g.group(i).group(0).substring());
				pw.print(", Description -> ");
				pw.print(g.group(i).group(1).substring());
				pw.print(" }");
			}
			pw.println();
			pw.println("    },");
                    
		} else if("end_thorn".equals(m)) {
			for(int calcNum=0;calcNum < calcs.size();calcNum++) {
				Group calc = calcs.get(calcNum);
                                pw.println();
				pw.print("calc");
				pw.print(calcNum);
				pw.println(" = {");
				for(int i=1;i<calc.groupCount();i++) {
					String mm = calc.group(i).getPatternName();
					if("calc_par".equals(mm)) {
						pw.print("  ");
						pw.print(calc.group(i).group(0).substring());
						pw.print(" -> ");
						pw.print(calc.group(i).group(1).substring());
						pw.println(",");
					} else if("shorts".equals(mm)) {
						formatOutput(pw, calc.group(i));
					} else if("eqns".equals(mm)) {
						formatOutput(pw, calc.group(i));
					} else {
						throw new Error("bad calc near "+calc.group(i).near());
					}
				}
				pw.print("  Name -> \"");
				pw.print(calc.group(0).substring());
				pw.println("\"");
				pw.println("};");
			}
                        pw.println();
			pw.print("calculations = {");
			for(int i=0;i<calcs.size();i++) {
				if(i > 0) pw.print(", ");
				pw.print("calc");
				pw.print(i);
			}
			pw.println("};");
			calcs = new ArrayList<Group>();
			
                        pw.println();
			pw.print("inheritedImplementations = {");
			for(int inum=0; inum<inhers.size();inum++) {
                                Group inher = inhers.get(inum);
				if(inum > 0) pw.print(", ");
				pw.print('"');
				pw.print(inher.group(0).substring());
				pw.print('"');
			}
			pw.println("};");
                        inhers = new ArrayList<Group>();
                        
                        pw.println();
			pw.println("inheritedKeywordParameters = {");
			for(int knum = 0;knum < ikpars.size();knum++) {
				Group ikpar = ikpars.get(knum);
				pw.println("  {");
				for(int i=1;i+1<ikpar.groupCount();i+=2) {
					pw.print("    ");
					pw.print(ikpar.group(i).substring());
					pw.print(" -> ");
					pw.print(ikpar.group(i+1).substring());
					pw.println(",");
				}
				pw.print("    Name -> ");
				pw.println(ikpar.group(0).substring());
				if(knum+1 == ikpars.size())
					pw.println("  }");
				else
					pw.println("  },");
			}
			pw.println("};");
			ikpars = new ArrayList<Group>();
                        
                        pw.println();
			pw.println("extendedKeywordParameters = {");
			for(int knum = 0;knum < ekpars.size();knum++) {
				Group ekpar = ekpars.get(knum);
				pw.println("  {");
				for(int i=1;i+1<ekpar.groupCount();i+=2) {
					pw.print("    ");
					pw.print(ekpar.group(i).substring());
					pw.print(" -> ");
					pw.print(ekpar.group(i+1).substring());
					pw.println(",");
				}
				pw.print("    Name -> ");
				pw.println(ekpar.group(0).substring());
				if(knum+1 == ekpars.size())
					pw.println("  }");
				else
					pw.println("  },");
			}
			pw.println("};");
			ekpars = new ArrayList<Group>();
                        
                        pw.println();
			pw.println("keywordParameters = {");
			for(int knum = 0;knum < kpars.size();knum++) {
				Group kpar = kpars.get(knum);
				pw.println("  {");
				for(int i=1;i+1<kpar.groupCount();i+=2) {
					pw.print("    ");
					pw.print(kpar.group(i).substring());
					pw.print(" -> ");
					pw.print(kpar.group(i+1).substring());
					pw.println(",");
				}
				pw.print("    Name -> ");
				pw.println(kpar.group(0).substring());
				if(knum+1 == kpars.size())
					pw.println("  }");
				else
					pw.println("  },");
			}
			pw.println("};");
			kpars = new ArrayList<Group>();
			
                        pw.println();
			pw.println("realParameters = {");
			for(int rnum = 0;rnum < rpars.size();rnum++) {
				Group rpar = rpars.get(rnum);
				pw.println("  {");
				for(int i=1;i+1<rpar.groupCount();i+=2) {
					pw.print("    ");
					pw.print(rpar.group(i).substring());
					pw.print(" -> ");
					pw.print(rpar.group(i+1).substring());
					pw.println(",");
				}
				pw.print("    Name -> ");
				pw.println(rpar.group(0).substring());
				if(rnum+1 == rpars.size())
					pw.println("  }");
				else
					pw.println("  }");
			}
			pw.println("};");
			rpars = new ArrayList<Group>();
			
                        pw.println();
			pw.println("intParameters = {");
			for(int inum = 0;inum < ipars.size();inum++) {
				Group ipar = ipars.get(inum);
				pw.println("  {");
				for(int i=1;i<ipar.groupCount();i++) {
                                        String mm = ipar.group(i).getPatternName();
                                        if ("alloweds".equals(mm)) {
                                                formatOutput(pw, ipar.group(i));
                                        } else if ("calc_par".equals(mm)) {
                                                pw.print("    ");
                                                pw.print(ipar.group(i).group(0).substring());
                                                pw.print(" -> ");
                                                pw.print(ipar.group(i).group(1).substring());
                                                pw.println(",");
                                        } else {
                                                throw new Error("intParameters");
                                        }
				}
				pw.print("    Name -> ");
				pw.println(ipar.group(0).substring());
				if(inum+1 == ipars.size())
					pw.println("  }");
				else
					pw.println("  }");
			}
			pw.println("};");
			ipars = new ArrayList<Group>();

                        pw.println();
			pw.println("CreateKrancThornTT[allGroups, \".\", \""+thornName+"\",");
			pw.println("  Calculations -> calculations,");
			pw.println("  DeclaredGroups -> declaredGroupNames,");
			pw.println("  PartialDerivatives -> partialDerivatives,");
			pw.println("  EvolutionTimelevels -> evolutionTimelevels,");
			pw.println("  DefaultEvolutionTimelevels -> 3,"); 
			pw.println("  UseLoopControl -> True,");
			pw.println("  InheritedImplementations -> inheritedImplementations,");
			pw.println("  InheritedKeywordParameters -> inheritedKeywordParameters,");
			pw.println("  ExtendedKeywordParameters -> extendedKeywordParameters,");
			pw.println("  KeywordParameters -> keywordParameters,");
			pw.println("  IntParameters -> intParameters,");
			pw.println("  RealParameters -> realParameters");
			pw.println("];");
		} else {
			pw.flush();
			throw new Error(m+": "+g.near());
		}
	}
	private void declareTensor(PrintWriter pw, Group g, Set<String> tensors, String name) throws Error {
		if(name == null) throw new NullPointerException(g.near().toString());
		pw.println();
		pw.print("DefineTensor[");
		pw.print(name);
		pw.println("];");
		if(tensors.contains(name)) {
			throw new Error("multiply defined tensor "+name+" "+g.near());
		}
		tensors.add(name);
	}
	public static void main(String[] args) throws IOException {
		Kranc k = new Kranc();
		for(String inputfile : args) {
			String outputfile = inputfile.replaceFirst("\\.kranc$", ".m");
			System.out.println(inputfile);
			System.out.println("  --> "+outputfile);
			k.doFile(inputfile, outputfile);
		}
	}
	Group trim(Group g) {
		if(g.groupCount()==1) {
			return trim(g.group(0));
		} else if(g.groupCount()==0) {
			return new Group(g.getPatternName(),g.getBegin(),g.getEnd(),Group.emptyList,g.getText());
		} else {
			LinkedList<Group> groups = new LinkedList<Group>();
			for(int i=0;i<g.groupCount();i++) {
				groups.add(trim(g.group(i)));
			}
			return new Group(g.getPatternName(),g.getBegin(),g.getEnd(),groups,g.getText());
		}
	}
}
