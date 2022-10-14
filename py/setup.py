from setuptools import setup, find_packages
from piraha.version import __version__

setup(
  name='Piraha',
  version=__version__,
  description='Parsing Expression Grammar for Python',
  long_description='Parsing Expression Grammar for Python',
  url='https://github.com/stevenrbrandt/piraha-peg.git',
  author='Steven R. Brandt',
  author_email='steven@stevenrbrandt.com',
  license='LGPL',
  packages=['piraha']
)
