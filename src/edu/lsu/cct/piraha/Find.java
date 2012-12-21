package edu.lsu.cct.piraha;

import java.util.ArrayList;
import java.util.List;

import edu.lsu.cct.piraha.examples.Calc;

public class Find {
	static Grammar g = new Grammar();
	static {
		g.compile("index","[0-9]+");
		g.compile("quoted", "([^'\\\\]|\\\\[^\n])+");
		g.compile("value","[a-zA-Z0-9_]+|'{quoted}'");
		g.compile("op","![=~]|=~?|[<>]=?");
		g.compile("name","[a-zA-Z0-9_*]+");
		g.compile("elem","{name}(\\[{index}\\]|)({op}{value}|)");
		g.compile("elemseq","\\.{elem}(\\|{elem})*");
		g.compile("quant","\\({elemseq}\\)\\*");
		g.compile("pat", "({quant}|{elemseq})({pat}|{quant}|)");
		g.compile("full", "{pat}$");
	}
	Group patc;
	public Find(String pattern) {
		Matcher m = g.matcher(pattern);
		if(!m.matches())
			throw new RuntimeException(m.near().toString());
		patc = m.group().group(0);
	}
	public List<Group> find(Group g) {
		List<Group> li = new ArrayList<Group>();
		find(patc,g,li);
		return li;
	}
	private void find(Group p,Group g,List<Group> li) {
		String pn = p.getPatternName();
		if("pat".equals(pn)) {
			List<Group> li2 = p.groupCount()==1 ? li :new ArrayList<Group>();
			find(p.group(0),g,li2);
			if(p.groupCount()==2) {
				for(Group gg : li2) {
					for(int i=0;i<gg.groupCount();i++) {
						find(p.group(1),gg.group(i),li);
					}
				}
			}
		} else if("elemseq".equals(pn)) {
			for(int i=0;i<p.groupCount();i++) {
				find(p.group(i),g,li);
			}
		} else if("elem".equals(pn)) {
//			p.dumpMatches();
			boolean add = true;
			int m = wildmatch(p.group(0).substring(),g.getPatternName());
			if(m != g.getPatternName().length()) {
				//li.add(g);
				add = false;
			}
			if(add && p.groupCount()==3) {
				String op = p.group(1).substring();
				String val = p.group(2).groupCount()>0 ? p.group(2).group(0).substring() : p.group(2).substring();
				if("=".equals(op)) {
					if(!g.substring().equals(val)) {
						add = false;
					}
				} else if("!=".equals(op)) {
					if(g.substring().equals(val)) {
						add = false;
					}
				} else if("=~".equals(op)) {
					if(wildmatch(val,g.substring()) != g.substring().length()) {
						add = false;
					}
				} else if("!~".equals(op)) {
					if(wildmatch(val,g.substring()) == g.substring().length()) {
						add = false;
					}
				} else {
					if("<".equals(op)) {
						int intstr = Integer.parseInt(g.substring());
						if(intstr >= Integer.parseInt(val)) {
							add = false;
						}
					} else if(">".equals(op)) {
						int intstr = Integer.parseInt(g.substring());
						if(intstr <= Integer.parseInt(val)) {
							add = false;
						}
					} else if("<=".equals(op)) {
						int intstr = Integer.parseInt(g.substring());
						if(intstr > Integer.parseInt(val)) {
							add = false;
						}
					} else if(">=".equals(op)) {
						int intstr = Integer.parseInt(g.substring());
						if(intstr < Integer.parseInt(val)) {
							add = false;
						}
					}
				}
			}
			if(add)
				li.add(g);
		} else if("quant".equals(pn)) {
			List<Group> lg = new ArrayList<Group>();
			find(p.group(0),g,lg);
			if(lg.size()>0) {
				if(p.groupCount()==1) {
					for(Group g2 : lg) {
						li.add(g2);
					}
				}
				for(int i=0;i<g.groupCount();i++) {
					find(p,g.group(i),li);
				}
			}
			if(p.groupCount()==2) {
				find(p.group(1),g,li);
			}
		}
	}
	
	static int wildmatch(String pattern,String text) {
		return wildmatch(pattern,0,text,0);
	}
    static int wildmatch(String pattern,int i,String text,int j) {
        while(i < pattern.length() && j < text.length()) {
            if(pattern.charAt(i) == '*') {
                if(pattern.length()-1==i)
                    return text.length();
                int n = wildmatch(pattern,i+1,text,j);
                if(n >= 0) return n;
                j++;
            } else if(pattern.charAt(i) == text.charAt(j)) {
                i++; j++;
            } else {
                return -1; 
            }   
        }   
        if(i==pattern.length())
            return j;
        else 
            return -1; 
    }   

	
    public static void check(boolean b) {
    	if(!b) throw new Error();
    }
	public static void main(String[] args) {
		Grammar tg = Calc.makeMath();
		Matcher mg = tg.matcher("3+9*(1+9)-4");
		mg.matches();
		Group res = mg.group();
		res.dumpMatches();
		
		Find f = new Find("(.*)*.num<=3");
		List<Group> fgl = f.find(res);
		check(fgl.size()==2);
		
		f = new Find("(.*)*.num");
		fgl = f.find(res);
		check(fgl.size()==5);
		
		f = new Find("(.*)*.addop|mulop");
		fgl = f.find(res);
		check(fgl.size()==4);
		
		System.out.println("test complete");
	}
}

