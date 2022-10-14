import re
import sys
from .colored import colored
from .here import here
from .version import __version__

trace = False

def set_trace(t):
    global trace
    trace = t

indent = 0
max_int = 2147483647

def esc(s):
    s = re.sub(r'([\\"])',r'\\\1',s)
    s = re.sub(r'\n',r'\\n',s)
    s = re.sub(r'\t',r'\\t',s)
    s = re.sub(r'\r',r'\\r',s)
    return s

def fmtc(c):
  if len(c) != 1:
    raise Exception("bad strlen")
  if c == " ":
    return "{space}"
  if c == "\n":
    return "{newline}"
  if c == "\t":
    return "{tab}"
  if c == "\r":
    return "{return}"
  return c

def expand_char(k):
  if k == "\t":
    k = "TAB"
  elif k == "\n":
    k = "NEWLINE"
  elif k == "\r":
    k = "RETURN"
  elif k == "'":
    k = "SINGLE_QUOTE"
  elif k == "\"":
    k = "DOUBLE_QUOTE"
  elif k == "`":
    k = "BACK_QUOTE"
  elif k == "\b":
    k = "BACKSPACE"
  elif k == "\e":
    k = "ESC"
  return "'"+k+"'"

class Literal:
    def possibly_zero(self):
        return False
    def diag(self):
        return "Literal("+self.c+")"
    def match(self,m):
        if m.textPos >= len(m.text):
            return False
        c = m.text[m.textPos]
        if trace:
            here("trace:",c,self.c,m.stack)
        if c == self.c:
            m.inc_pos()
            return True
        else:
            m.fail(self.c)
            return False
    def __init__(self,c):
        self.c = c
        if len(c) != 1:
            raise Exception()

class ILiteral:
    def match(self,m):
        if m.textPos >= len(m.text):
            return False
        c = m.text[m.textPos]
        if c == self.lc or c == self.uc:
            m.inc_pos()
            return True
        m.fail(c)
        return False
    def diag(self):
        if self.uc == self.lc:
            return "ILiteral("+self.uc+")"
        else:
            return "ILiteral("+self.lc+","+self.uc+")"
    def __init__(self,c):
        if len(c) != 1:
            raise Exception()
        self.lc = c.lower()
        self.uc = c.upper()

class Seq:
    def possibly_zero(self):
        for pat in self.patternList:
            if pat.possibly_zero():
                return True
        return False

    def match(self,m):
        for pat in self.patternList:
            if not pat.match(m):
                return False
        return True

    def diag(self):
        out = "Seq{"
        tw = ""
        for p in self.patternList:
            if p is None:
                out += tw + "UNDEF"
            elif p == 0:
                out += tw + "ZERO"
            else:
                out += tw + p.diag()
            tw = ","
        out += "}"
        return out

    def __init__(self,*patterns):
        self.ignCase = False
        self.igcShow = False
        if type(patterns[0]) == list:
            self.patternList = patterns[0]
            self.ignCase = patterns[1]
            self.igcShow = patterns[2]
        else:
            self.patternList = patterns

class Bracket:
    def addRange(self,lo,hi,igcase=False):
        # We are expecting single characters here,
        # not, e.g. \n, or \x{34af}.
        if len(lo) != 1:
            raise Exception()
        if len(hi) != 1:
            raise Exception()
        # If ignorecase is on, call addRange()
        # twice. Once with the lower, once with
        # the upper, and don't set the igCase flag.
        if igcase:
          self.addRange(lo.lower(), hi.lower())
          self.addRange(lo.upper(), hi.upper())
          return
        a = self.ranges
        # Store the ascii value of the character,
        # so that ranges can be compared numerically.
        r = [ord(lo), ord(hi)]
        # The upper range should be greater than or
        # equal to the lower.
        if r[0] > r[1]:
            raise Exception("bad range: "+chr(r[0])+" to "+chr(r[1]))
        a += [r]
        return self

    def match(self,m):
        if m.textPos >= len(m.text):
          # Fail if we're passed the end of the string
          return False
        rc = m.text[m.textPos]
        # We shouldn't have an empty string here
        c = ord(rc)
        for r in self.ranges:
          if r[0] <= c and c <= r[1]:
            if not self.neg:
              # increment position in string
              # after a successful match
              m.inc_pos()
              return True
            else:
              m.fail(self.ranges)
              return False
        if not self.neg:
          m.fail(self.ranges)
          return False
        else:
          m.inc_pos()
          return True

    def diag(self):
        out = "Bracket("
        for r in self.ranges:
          if r[0] == r[1]:
            out += fmtc(chr(r[0]))
          else:
            out += fmtc(chr(r[0]))+"-"+fmtc(chr(r[1]))
        out += ")"
        return out

    def __init__(self,neg=False):
        assert type(neg)==bool
        self.neg = neg
        self.ranges = []

class Lookup:
    # Match a pattern by name. Thus, for the grammar
    # A = a
    # B = b
    # R = ({A}|{B})
    # The {A} and the {B} are both "Lookup" pattern
    # elements.

    def possibly_zero(self):
        return False

    def diag(self):
        return "Lookup("+self.name+")"

    def match(self,m):
      g = m.g # grammar
      pname = self.name
      pat = g.patterns[pname]
      # Save the child groups
      chSave = m.gr
      # Save the start position
      start = m.textPos
      # Lookup patterns that begin with a - do not capture, that
      # is they do not produce a node in the parse tree.
      cap = self.capture
      # Replace the current groups with a new group
      if cap:
        m.gr = Group(pname,chSave.text,start,-1)
      m.stack += [pname]
      try:
        b = pat.match(m)
      finally:
        m.stack = m.stack[:-1]
      if b:
        if cap:
          # Set the end of the current group
          m.gr.end = m.textPos
          # Append the current group the saved array of child groups
          chSave.children += [m.gr]
      if cap:
        # Restore the child groups to what they were before matching
        m.gr = chSave
      return b

    def __init__(self,name,g):
      g = re.match(r'^-(.*)',name)
      if g:
        self.capture = False
        self.name = g.group(1)
      else:
        self.capture = True
        self.name = name
      self.g = g

class Break:
    # Reprents a {brk} pattern element. This
    # pattern element triggers an exception to
    # be thrown, and allows the pattern matcher
    # to escape from processing a * pattern.
    # It's like a break from a for/while loop.

    def diag(self):
      return "Break()"

    def match(self,m):
      raise BreakOut()

    def __init__(self):
        pass

class BreakOut(Exception):
    pass

class Fail(Exception):
    pass

class NegLookAhead:
    # This pattern represents a negative
    # lookahead assertion. It is roughly
    # the same as it is in perl.
    # E.g. the pattern "cat(?!s)" will match
    # the word cat, but not if it's followed
    # by an s.

    def diag(self):
      return "NegLookAhead("+self.pat.diag()+")"

    def match(self,m):
      p  = m.textPos
      h  = m.hash
      mx,ms = m.maxTextPos,m.max_stack
      b = self.pat.match(m)
      m.textPos=p
      m.maxTextPos,m.max_stack=mx,ms
      m.hash = h
      return not b

    def __init__(self,pat,ignc=False,gram=None):
      self.pat = pat
      self.ignCase = ignc
      self.gram = gram

class LookAhead:
    # This pattern represents a 
    # lookahead assertion. It is roughly
    # the same as it is in perl.
    # E.g. the pattern "cat(?=s)" will match
    # the word cat, but only if it's followed
    # by an s.

    def diag(self):
      return "LookAhead("+self.pat.diag()+")"

    def match(self,m):
      p  = m.textPos
      h  = m.hash
      mx,ms = m.maxTextPos,m.max_stack
      b = self.pat.match(m)
      m.textPos=p
      m.maxTextPos,m.max_stack=mx,ms
      m.hash = h
      return b

    def __init__(self,pat,ignc=False,gram=None):
      self.pat = pat
      self.ignCase = ignc
      self.gram = gram

class Multi:
    # This pattern element is used to match the
    # pattern it contains multiple times. It is
    # used to implement the * and + pattern elements.

    def match(self,m):
      for i in range(0,self.mx+1):
        save = m.textPos
        nchildren = len(m.gr.children)
        rc = None
        try:
          if not self.pattern.match(m) or m.textPos <= save:
            raise Fail()
        except Fail as e:
            m.textPos = save
            m.gr.children = m.gr.children[0:nchildren]
            rc = i >= self.mn
            return rc
        except BreakOut as e:
            rc = i >= self.mn
            return rc
      return True

    def diag(self):
      if self.pattern is None:
        raise Exception("bad pat")
      return "Multi("+str(self.mn)+","+str(self.mx)+","+self.pattern.diag()+")"

    def __init__(self,pat,mn,mx=None):
      if type(pat)==int and type(mn)==int:
        mx = mn
        mn = pat
        pat = Nothing()
      elif mx is None:
        mx = mn
      self.pattern = pat
      self.mn = mn
      self.mx = mx
    
class Or:
    # Match one of a sequence of alternatives
    # for a pattern, e.g. (a|b) matches either
    # the literal a or the literal b.

    def possibly_zero(self):
      for pat in self.patterns:
        if pat.possibly_zero():
          return True
      return False

    def diag(self):
      out = "Or("
      tw = ""
      for p in self.patterns:
        out += tw + p.diag()
        tw = ","
      out += ")"
      return out

    def match(self,m):
      save = m.textPos
      nchildren = len(m.gr.children)
      for pat in self.patterns:
        # The position, as well as the length of the child
        # nodes needs to be reset before every attempted
        # match to prevent leftovers from failed attempted
        # matches from lingering.
        m.textPos = save
        m.gr.children = m.gr.children[0:nchildren]
        if pat.match(m):
          # If any of the patterns works, we're done
          return True
      return False

    def __init__(self,*args):
      self.ignCase = False
      self.igcShow = False
      self.patterns = args
      if len(args)==2 and type(args[0])==bool and type(args[1])==bool:
        self.ignCase = args[0]
        self.igcShow = args[1]
        self.patterns = []

class Nothing:
    # This pattern element matches nothing.
    # It always succeeds.

    def match(self,m):
      return True

    def diag(self):
      return "Nothing()"

    def __init__(self):
        pass

class Start:
    # This pattern element matches the start
    # of a string.

    def match(self,m):
      return m.textPos==0

    def diag(self):
      return "Start()"

    def __init__(self):
        pass

class End:
    # This pattern element matches the end of
    # a string.

    def match(self,m):
      return m.textPos==len(m.text) or (m.textPos+1==len(m.text) and m.text[m.textPos]=='\n')

    def diag(self):
      return "End()"

    def __init__(self):
        pass

class Dot:
    # Matches any character except \n

    def diag(self):
      return "Dot()"

    def match(self,m):
      if m.textPos >= len(m.text):
        return False
      c = m.text[m.textPos]
      if re.match(r'.',c):
        m.inc_pos()
        return True
      else:
        return False

    def __init__(self):
        pass

class Empty:
    def Has(self,a,b=None):
        return self
    def StrEq(self,a,b=None):
        return self
    def eval(self):
        return False

empty = Empty()

class Group:
    # This class represents a node in the
    # parse tree.
    def eval(self):
        return True

    def __repr__(self):
        return self.dump()

    def group(self,n,nm=None):
      if n < 0:
        n += self.groupCount()
      ref = self.children[n]
      if nm is not None:
        m = ref.name
        if m != nm:
            raise Exception("wrong group '$nm' != '$m'")
      return ref

    def has(self,n,nm=None):
      if n < 0:
        n += self.groupCount()
      if n < 0 or n >= len(self.children):
        return None
      ref = self.children[n]
      if nm is not None:
        m = ref.name
        if m != nm:
            return None 
      return ref

    def Has(self,n,nm=None):
      if n < 0:
        n += self.groupCount()
      if n < 0 or n >= len(self.children):
        return empty
      ref = self.children[n]
      if nm is not None:
        m = ref.name
        if m != nm:
            return empty
      return self

    def StrEq(self,n,nm=None):
      if n < 0:
        n += self.groupCount()
      if n < 0 or n >= len(self.children):
        return empty
      ref = self.children[n]
      if nm is not None:
        m = ref.substring()
        if m != nm:
            return empty
      return self

    def is_(self,nm):
      return self.name == nm

    def groupCount(self):
      return len(self.children)

    def substring(self):
      return self.text[self.start:self.end]

    def linenum(self):
      n = 1
      t = self.text[0:self.start]
      for c in t:
        if c == '\n':
            n += 1
      return n

    def mkstring(self,tween=" "):
      if len(self.children) == 0:
        return self.substring()
      else:
        buf = ""
        for child in self.children:
          if buf != '':
            buf += tween
          buf += child.mkstring(tween)
        return buf

    def dump(self,post=''):
      global indent
      pre = "\n"+("  " * indent)
      indent += 1
      end = "\n"+("  " * indent)
      if len(self.children) == 0:
        indent -= 1
        return self.name+"(\""+esc(self.substring())+"\")"
      else:
        out = self.name+pre+"("+end
        tween = ""
        ln = len(self.children)
        for i in range(0,ln):
          child = self.children[i]
          out += tween
          out += child.dump(i<ln)
          tween=","+end
        if not post:
          out += pre
        indent -= 1
        if post:
            out += "\n"+("  " * indent) 
        out += ")"
        return out

    def getPatternName(self):
      return self.name

    def __init__(self,name,text,start,end):
      self.name = name
      self.text = text
      self.start = start
      self.end = end
      self.children = []

class Boundary:
    # This pattern element matches a "boundary"
    # either the start of a string, the end of
    # a string, or a transition between a c-identifier
    # character and a non c-identifier character.

    def match(self,m):
      if m.textPos==len(m.text) or m.textPos==0:
        return True
      bf = m.text[m.textPos-1]
      af = m.text[m.textPos]
      if re.match(r'\w',bf) and re.match(r'\w',af):
        return False
      return True

    def diag(self):
      return "Boundary()"

    def __init__(self):
        pass
 
class Grammar:
    def __init__(self):
        self.patterns = {}

class Matcher:
    # The matcher holds data relevant to the
    # current match, i.e. the position in the
    # text, etc. In principle, two threads
    # could use the same pattern at the same
    # time, but not the same matcher.

    def show(self):
      print("SHOW")

    def showError(self,fd=sys.stdout):
      print("max_stack:",self.max_stack,file=fd)
      pos = self.maxTextPos
      txt = self.text
      pre = txt[0:pos]
      line = 1
      for c in pre:
        if c == '\n':
            line += 1
      print(colored("ERROR ON LINE ","red"),line,":",sep='',file=fd)
      g = re.match(r'.*\n.*\n.*\n*$',pre)
      if g:
        pre = g.group(0)+'$'
      post = txt[pos+1:]
      g = re.search(r'.*',post)
      if g:
        post = g.group(0)
      post = post.strip()
      hash = self.hash

      # Don't worry about comment characters
      for key in [" ","#","\t","\n","\b","\r"]:
        if key in hash:
          del hash[key]

      if len(txt) == 0:
        c = ""
      elif pos >= len(txt):
        c = txt[-1]
      else:
        c = txt[pos]
      if c == "\n":
        post = ""
      print(colored(pre,"green"),colored(c,"yellow"),colored(post,"green"),sep='',file=fd)
      g = re.search(r'.*$',pre)
      print(" " * len(g.group(0)),"^",sep='',file=fd)
      print(" " * len(g.group(0)),"| here",sep='',file=fd)
      out = []
      count = []
      ks = sorted(hash.keys()) #sort keys %hash
      for k in ks:
        if len(out)>0 and re.match(r'[b-zB-Z1-9]',k) and ord(k) == ord(out[-1]) + count[-1]:
          count[-1]+=1
        else:
          out += [k]
          count += [1]
      out2 = []
      for i in range(0,len(out)):
        if count[i]==1:
          k = out[i]
          out2 += [expand_char(k)]
        elif count[i]==2:
          k = out[i]
          out2 += ["'"+k+"'"]
          out2 += ["'"+chr(ord(k)+1)+"'"]
        else:
          k = out[i]
          out2 += ["'"+k+"' to '"+chr(ord(k)+count[i]-1)+"'"]
      print("FOUND CHARACTER: ",expand_char(c),"\n",end='',sep='',file=fd)
      print("EXPECTED CHARACTER(S): ",",".join(out2),"\n",end='',sep='',file=fd)

    def upos(self,pos):
      self.textPos = pos
      if pos > self.maxTextPos:
        self.maxTextPos = pos
        self.max_stack = [k for k in self.stack]
        self.hash = {}

    def inc_pos(self):
      self.textPos += 1
      pos = self.textPos
      if pos > self.maxTextPos:
        self.maxTextPos = pos
        self.max_stack = [k for k in self.stack]
        self.hash = {}

    def matches(self):
      ret = self.pat.match(self)
      self.gr.end = self.textPos
      return ret

    def groupCount(self):
      return len(self.gr.children)

    def group(self,i):
      return self.gr.children[i]

    def fail(self,c):
      if self.textPos > self.maxTextPos:
        self.maxTextPos = self.textPos
        self.max_stack = [k for k in self.stack]
        self.hash = {}
      elif self.textPos == self.maxTextPos:
        pass
      else:
        return
      if type(c) == list:
        for r in c:
          for n in range(r[0],r[1]+1):
            self.hash[chr(n)]=1
      elif type(c) == str and len(c)==1:
        self.hash[c] = 1
      else:
        raise Exception(str(c))

    def __init__(self,grammar,pname,text):
        self.stack = [pname]
        self.max_stack = []
        self.text = text
        self.textPos = 0
        self.maxTextPos = 0
        self.max_stack = []
        self.pat = grammar.patterns[pname]
        self.g = grammar
        self.gr = Group(pname,text,0,len(text))
        self.hash = {}

def fileparserGenerator():
  g = Grammar()
  g.patterns["boundary"]=(Seq(
    Literal("\\"),
    Literal('b')))
  g.patterns["backref"]=(Seq(
    Literal("\\"),
    (Bracket(False))
      .addRange('1','9')
      ))
  g.patterns["named"]=(Seq(
    Literal('{'),
    Lookup("name",g),
    Literal('}')))
  g.patterns["echar"]=(Literal('-'))
  g.patterns["num"]=(Multi((Bracket(False))
    .addRange('0','9')
    ,1,2147483647))
  g.patterns["dot"]=(Literal('.'))
  g.patterns["pattern"]=(Or(
    Lookup("group_top",g),
    Nothing()))
  g.patterns["range"]=(Seq(
    Lookup("cchar",g),
    Literal('-'),
    Lookup("cchar",g)))
  g.patterns["rule"]=(Seq(
    Lookup("name",g),
    Lookup("-w",g),
    Literal('='),
    Lookup("-w",g),
    Lookup("pattern",g)))
  g.patterns["pelems_next"]=(Seq(
    Multi(Lookup("s",g),0,1),
    Literal('|'),
    Multi(Lookup("s",g),0,1),
    Lookup("pelem",g),
    Multi(Seq(
      Multi(Lookup("s0",g),0,1),
      Lookup("pelem",g)),0,2147483647)))
  g.patterns["literal"]=(Or(
    Seq(
      Literal("\\"),
      Literal('u'),
      Lookup("hex",g)),
    Seq(
      Literal("\\"),
      (Bracket(True))
        .addRange('1','9')
        .addRange('b','b')
        ),
    (Bracket(True))
      .addRange("\n","\n")
      .addRange("\r","\r")
      .addRange('$','$')
      .addRange('(','+')
      .addRange('.','.')
      .addRange('?','?')
      .addRange('[','^')
      .addRange('{','}')
      ))
  g.patterns["neg"]=(Literal('^'))
  g.patterns["file"]=(Seq(
    Start(),
    Multi(Lookup("-s",g),0,1),
    Lookup("rule",g),
    Multi(Seq(
      Multi(Lookup("-s",g),0,1),
      Lookup("rule",g)),0,2147483647),
    Multi(Lookup("-s",g),0,1),
    End()))
  g.patterns["cchar"]=(Or(
    Seq(
      Literal("\\"),
      Literal('u'),
      Lookup("hex",g)),
    Seq(
      Literal("\\"),
      (Bracket(True))
        ),
    (Bracket(True))
      .addRange('-','-')
      .addRange("\\",']')
      ))
  g.patterns["hex"]=(Multi((Bracket(False))
    .addRange('0','9')
    .addRange('A','F')
    .addRange('a','f')
    ,4,4))
  g.patterns["pipe"]=(Nothing())
  g.patterns["end"]=(Literal('$'))
  g.patterns["s0"]=(Multi((Bracket(False))
    .addRange("\t","\t")
    .addRange(' ',' ')
    ,1,2147483647))
  g.patterns["pelems_top"]=(Seq(
    Lookup("pelem",g),
    Multi(Seq(
      Multi(Lookup("s0",g),0,1),
      Lookup("pelem",g)),0,2147483647)))
  g.patterns["group"]=(Seq(
    Literal('('),
    Or(
      Lookup("ign_on",g),
      Lookup("ign_off",g),
      Lookup("lookahead",g),
      Lookup("neglookahead",g),
      Nothing()),
    Or(
      Lookup("group_inside",g),
      Nothing()),
    Literal(')')))
  g.patterns["ign_on"]=(Seq(
    Literal('?'),
    Literal('i'),
    Literal(':')))
  g.patterns["pelem"]=(Or(
    Seq(
      Or(
        Lookup("named",g),
        Lookup("dot",g),
        Lookup("backref",g),
        Lookup("literal",g),
        Lookup("charclass",g),
        Lookup("group",g)),
      Or(
        Lookup("quant",g),
        Nothing())),
    Or(
      Lookup("start",g),
      Lookup("end",g),
      Lookup("boundary",g))))
  g.patterns["nothing"]=(Nothing())
  g.patterns["group_inside"]=(Seq(
    Lookup("pelems",g),
    Multi(Seq(
      Literal('|'),
      Lookup("pelems",g)),0,2147483647),
    Or(
      Seq(
        Multi(Lookup("s0",g),0,1),
        Lookup("nothing",g),
        Literal('|')),
      Nothing()),
    Multi(Lookup("s",g),0,1)))
  g.patterns["start"]=(Literal('^'))
  g.patterns["quantmax"]=(Seq(
    Literal(','),
    Multi(Lookup("num",g),0,1)))
  g.patterns["ign_off"]=(Seq(
    Literal('?'),
    Literal('-'),
    Literal('i'),
    Literal(':')))
  g.patterns["quant"]=(Or(
    Literal('+'),
    Literal('*'),
    Literal('?'),
    Seq(
      Literal('{'),
      Lookup("num",g),
      Multi(Lookup("quantmax",g),0,1),
      Literal('}'))))
  g.patterns["lookahead"]=(Seq(
    Literal('?'),
    Literal('=')))
  g.patterns["s"]=(Multi(Or(
    (Bracket(False))
      .addRange("\t","\n")
      .addRange("\r","\r")
      .addRange(' ',' ')
      ,
    Seq(
      Literal('#'),
      Multi(Dot(),0,2147483647))),1,2147483647))
  g.patterns["pelems"]=(Seq(
    Multi(Seq(
      Multi(Lookup("s",g),0,1),
      Lookup("pelem",g)),1,2147483647),
    Multi(Lookup("s",g),0,1)))
  g.patterns["group_top"]=(Seq(
    Lookup("pelems_top",g),
    Multi(Lookup("pelems_next",g),0,2147483647),
    Or(
      Seq(
        Multi(Lookup("s",g),0,1),
        Lookup("nothing",g),
        Literal('|')),
      Nothing())))
  g.patterns["w"]=(Multi((Bracket(False))
    .addRange("\t","\t")
    .addRange(' ',' ')
    ,0,2147483647))
  g.patterns["name"]=(Seq(
    Multi(Literal('-'),0,1),
    (Bracket(False))
      .addRange(':',':')
      .addRange('A','Z')
      .addRange('_','_')
      .addRange('a','z')
      ,
    Multi((Bracket(False))
      .addRange('0',':')
      .addRange('A','Z')
      .addRange('_','_')
      .addRange('a','z')
      ,0,2147483647)))
  g.patterns["charclass"]=(Seq(
    Literal('['),
    Multi(Lookup("neg",g),0,1),
    Multi(Or(
      Lookup("range",g),
      Lookup("echar",g)),0,1),
    Multi(Or(
      Lookup("range",g),
      Lookup("cchar",g)),0,2147483647),
    Multi(Lookup("echar",g),0,1),
    Literal(']')))
  g.patterns["neglookahead"]=(Seq(
    Literal('?'),
    Literal('!')))
  return g

def reparserGenerator():
  g = Grammar()
  g.patterns["boundary"]=(Seq(
    Literal("\\"),
    Literal('b')))
  g.patterns["backref"]=(Seq(
    Literal("\\"),
    (Bracket(False))
      .addRange('1','9')
      ))
  g.patterns["named"]=(Seq(
    Literal('{'),
    Lookup("name",g),
    Literal('}')))
  g.patterns["echar"]=(Literal('-'))
  g.patterns["num"]=(Multi((Bracket(False))
    .addRange('0','9')
    ,1,2147483647))
  g.patterns["dot"]=(Literal('.'))
  g.patterns["pattern"]=(Seq(
    Start(),
    Or(
      Lookup("group_inside",g),
      Nothing()),
    End()))
  g.patterns["range"]=(Seq(
    Lookup("cchar",g),
    Literal('-'),
    Lookup("cchar",g)))
  g.patterns["literal"]=(Or(
    Seq(
      Literal("\\"),
      Literal('u'),
      Lookup("hex",g)),
    Seq(
      Literal("\\"),
      (Bracket(True))
        .addRange('b','b')
        ),
    (Bracket(True))
      .addRange('$','$')
      .addRange('(','+')
      .addRange('.','.')
      .addRange('?','?')
      .addRange('[','^')
      .addRange('{','}')
      ))
  g.patterns["neg"]=(Literal('^'))
  g.patterns["cchar"]=(Or(
    Seq(
      Literal("\\"),
      Literal('u'),
      Lookup("hex",g)),
    Seq(
      Literal("\\"),
      (Bracket(True))
        ),
    (Bracket(True))
      .addRange('-','-')
      .addRange("\\",']')
      ))
  g.patterns["hex"]=(Multi((Bracket(False))
    .addRange('0','9')
    .addRange('A','F')
    .addRange('a','f')
    ,4,4))
  g.patterns["pipe"]=(Nothing())
  g.patterns["end"]=(Literal('$'))
  g.patterns["group"]=(Seq(
    Literal('('),
    Or(
      Lookup("ign_on",g),
      Lookup("ign_off",g),
      Lookup("lookahead",g),
      Lookup("neglookahead",g),
      Nothing()),
    Or(
      Lookup("group_inside",g),
      Nothing()),
    Literal(')')))
  g.patterns["ign_on"]=(Seq(
    Literal('?'),
    Literal('i'),
    Literal(':')))
  g.patterns["pelem"]=(Or(
    Seq(
      Or(
        Lookup("named",g),
        Lookup("dot",g),
        Lookup("backref",g),
        Lookup("literal",g),
        Lookup("charclass",g),
        Lookup("group",g)),
      Or(
        Lookup("quant",g),
        Nothing())),
    Or(
      Lookup("start",g),
      Lookup("end",g),
      Lookup("boundary",g))))
  g.patterns["nothing"]=(Nothing())
  g.patterns["group_inside"]=(Seq(
    Lookup("pelems",g),
    Multi(Seq(
      Literal('|'),
      Lookup("pelems",g)),0,2147483647),
    Or(
      Seq(
        Lookup("nothing",g),
        Literal('|')),
      Nothing())))
  g.patterns["start"]=(Literal('^'))
  g.patterns["quantmax"]=(Seq(
    Literal(','),
    Multi(Lookup("num",g),0,1)))
  g.patterns["ign_off"]=(Seq(
    Literal('?'),
    Literal('-'),
    Literal('i'),
    Literal(':')))
  g.patterns["quant"]=(Or(
    Literal('+'),
    Literal('*'),
    Literal('?'),
    Seq(
      Literal('{'),
      Lookup("num",g),
      Multi(Lookup("quantmax",g),0,1),
      Literal('}'))))
  g.patterns["lookahead"]=(Seq(
    Literal('?'),
    Literal('=')))
  g.patterns["pelems"]=(Seq(
    Lookup("pelem",g),
    Multi(Lookup("pelem",g),0,2147483647)))
  g.patterns["name"]=(Seq(
    Multi(Literal('-'),0,1),
    (Bracket(False))
      .addRange(':',':')
      .addRange('A','Z')
      .addRange('_','_')
      .addRange('a','z')
      ,
    Multi((Bracket(False))
      .addRange('0',':')
      .addRange('A','Z')
      .addRange('_','_')
      .addRange('a','z')
      ,0,2147483647)))
  g.patterns["charclass"]=(Seq(
    Literal('['),
    Multi(Lookup("neg",g),0,1),
    Multi(Or(
      Lookup("range",g),
      Lookup("echar",g)),0,1),
    Multi(Or(
      Lookup("range",g),
      Lookup("cchar",g)),0,2147483647),
    Multi(Lookup("echar",g),0,1),
    Literal(']')))
  g.patterns["neglookahead"]=(Seq(
    Literal('?'),
    Literal('!')))
  return g

rp = reparserGenerator()
fp = fileparserGenerator()

def mkMulti(g):
    if g.groupCount()==0:
        s = g.substring()
        if "*" == s:
            return Multi(0,max_int)
        elif "+" == s:
            return Multi(1,max_int)
        elif "?" == s:
            return Multi(0,1)
    elif g.groupCount()==1:
        mn = (g.group(0).substring())
        return Multi(mn,mn)
    elif g.groupCount()==2:
        mn = 1*(g.group(0).substring())
        if g.group(1).groupCount()>0:
            mx = (g.group(1).group(0).substring())
            return Multi(mn,mx)
        else:
            return Multi(mn,max_int)

def getChar(gr):
    if gr.groupCount()==1:
        sub = gr.group(0).substring()
        n = 0
        # Parse a hexadecimal coded character
        for i in range(len(sub)):
            c = sub[i]
            if ord(c) >= ord('0') and ord(c) <= ord('9'):
                n = n*16+ord(c)-'0'
            elif ord(c) >= ord('a') and ord(c) <= ord('f'):
                n = n*16+ord(c)-ord('a')+10
            elif ord(c) >= ord('A') and ord(c) <= ord('F'):
                n = n*16+ord(c)-ord('A')+10
    gs = gr.substring()
    if len(gs)==2:
        # Parse an escaped character
        c = gs[1]
        if c == 'n':
            return "\n"
        elif c == 'r':
            return "\r"
        elif c == 't':
            return "\t"
        elif c == 'b':
            return "\b"
        else:
            return c
    else:
        return gs[0]

# Convert a piraha expression into the
# data structures needed to parse piraha
# code. Consider, for example, the pattern
# "a". It would be passed in as a group
# with name "literal" and a substring, "a".
# This would then be converted to a
# Literal() object, with $self->{c} = "a".
def compile(g,ignCase,gram):
    pn = g.getPatternName()
    if "literal" == pn:
        c = getChar(g)
        if ignCase:
            return ILiteral(c)
        else:
            return Literal(c)
    elif "pattern" == pn:
        if g.groupCount()==0:
            return Nothing()
        return compile(g.group(0),ignCase,gram)
    elif "pelem" == pn:
        if g.groupCount()==2:
            pm = mkMulti(g.group(1))
            m = pm # Not sure
            m.pattern = compile(g.group(0),ignCase,gram)
            return pm
        return compile(g.group(0),ignCase,gram)
    elif "pelems" == pn or "pelems_top" == pn or "pelems_next" == pn:
        li = []
        for i in range(g.groupCount()):
            pat = compile(g.group(i),ignCase,gram)
            li += [pat]
        if len(li)==1:
            return li[0]
        if len(li)==0:
          print(g.dump(),"\n")
          raise Exception("empty")
        return Seq(li,0,0)
    elif "group_inside" == pn or "group_top" == pn:
        if g.groupCount()==1:
            return compile(g.group(0),ignCase,gram)
        li = []
        for i in range(g.groupCount()):
            li += [compile(g.group(i),ignCase,gram)]
        or_ = Or(False,False)
        or_.patterns = li
        orp = or_
        return orp
    elif "group" == pn:
        or_ = Or(False,False)
        orp_ = or_
        ignC = ignCase
        inside = None
        if g.groupCount()==2:
            ignC = or_.igcShow = True
            ps = g.group(0).getPatternName()
            if ps == "ign_on":
                ignC = or_.ignCase = True 
            elif ps == "ign_off":
                ignC = or_.ignCase = False
            elif ps == "neglookahead":
                return NegLookAhead(compile(g.group(1),ignCase,gram))
            elif ps == "lookahead":
                return LookAhead(compile(g.group(1),ignCase,gram))
            inside = g.group(1)
        else:
            inside = g.group(0)
        for i in range(inside.groupCount()):
            or_.patterns += [compile(inside.group(i),ignC,gram)]
        if or_.igcShow == False and 1+len(or_.patterns)==1:
            return or_.patterns[0]
        if len(orp_.patterns)==0:
            raise Exception()
        return orp_
    elif "start" == pn:
        return Start()
    elif "end" == pn:
        return End()
    elif "boundary" == pn:
        return Boundary()
    elif "charclass" == pn:
        br = Bracket()
        brp = br
        i=0
        if g.groupCount()>0 and g.group(0).getPatternName() == "neg":
            i += 1
            br.neg = True
        i0 = i
        for i in range(i0,g.groupCount()):
            gn = g.group(i).getPatternName()
            if "range" == gn:
                c0 = getChar(g.group(i).group(0))
                c1 = getChar(g.group(i).group(1))
                br.addRange(c0, c1, ignCase)
            else:
                c = getChar(g.group(i))
                br.addRange(c,c, ignCase)
        return brp
    elif "named" == pn:
        lookup = g.group(0).substring()
        if "brk" == lookup:
            return Break()
        return Lookup(lookup, gram)
    elif "nothing" == pn:
        return Nothing()
    elif "s" == pn or "s0" == pn:
        return Lookup("-skipper", gram)
    elif "dot" == pn:
        return Dot()
    elif "backref" == pn:
        return BackRef(ord(g.substring()[1:2])-ord('0'), ignCase)
    return None

# Compile an individual Piaraha pattern.
def compilePattern(pattern):
  # The rules to compile a Piraha pattern
  # expression stored as a Piraha parse tree.
  grammar = reparserGenerator()
  m = Matcher(grammar,"pattern",pattern)
  if m.matches():
    # Convert a parse tree for a Piraha expression
    # into the Piraha data structures used to parse
    # code.
    return compile(m.gr,0,grammar)
  else:
    m.showError()
    raise Exception()

def parse(peg,src):
  g,rule = parse_peg_src(peg)
  return parse_src(g,rule,src)

# Open, read and parse a peg rule file.
def parse_peg_file(peg):
  with open(peg) as fd:
    peg_contents = fd.read()
  return parse_peg_src(peg_contents)

# Compile a file containing Piraha rules
def compileFile(g,fname):
  with open(fname,"r") as fd:
    buffer = fd.read()
  return compileSrc(g,buffer)

def compileSrc(g,buffer):
  # The rules to compile a Piraha rule file
  # stored as a Piraha parse tree.
  grammar = fileparserGenerator()
  m = Matcher(grammar,"file",buffer)
  b = m.matches()
  if not b:
    m.showError()
    raise Exception("match failed")

  for i in range(m.groupCount()):
    rule = m.group(i)
    # Convert the parse tree for each Piraha rule
    # into the Piraha data structures used to parse
    # code.
    ptmp = compile(rule.group(1), 0, grammar)
    nm = rule.group(0).substring()
    g.patterns[nm] = ptmp
    # Set the default rule.
    g.default_rule = nm
  return g.default_rule

# Parse a peg rule file, return
# a grammar and the default file
def parse_peg_src(peg_contents):
  g = Grammar()
  rule = compileSrc(g,peg_contents)
  return (g,rule)

# Given a grammar and a rule, parse
# a source string which should match the rule.
def parse_src(g,rule,src):
  with open(src,"r") as fd:
    src_contents = fd.read()
  return Matcher(g,rule,src_contents)
