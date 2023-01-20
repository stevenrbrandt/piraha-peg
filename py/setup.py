from setuptools import setup, find_packages
import re

vfile="piraha/version.py"
verstrline = open(vfile, "rt").read()
VSRE = r"^__version__\s*=\s*(['\"])(.*)\1"
g = re.search(VSRE, verstrline, re.M)
if g:
    __version__ = g.group(2)
else:
    raise RuntimeError(f"Unable to find version in file '{vfile}")

setup(
  name='Piraha',
  version=__version__,
  description='Parsing Expression Grammar for Python',
  long_description='Parsing Expression Grammar for Python',
  url='https://github.com/stevenrbrandt/piraha-peg.git',
  author='Steven R. Brandt',
  author_email='steven@stevenrbrandt.com',
  license='LGPL',
  packages=['piraha'],
  package_data = {
    'piraha': ['py.typed'],
  }
)
