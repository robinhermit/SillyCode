# SillyCode
live-coding interface for real-time text sonification
#### This version of SillyCode works with any Pyphen language: https://pypi.org/project/pyphen/ 

SillyCode is a live coding environment based on SuperCollider, working as a real-time sonification of plain text. It allows to sequence up to 10 parallel loops, where a user can write by typing basic literal text in Italian (`main` branch of this repository) or plenty other languages contained in Pyphen (in the `multi-lang` branch). In addition, with a minimalistic syntax consisting of punctuation, symbols and short commands it is possible to insert swings, fills and selectively execute the loops.

The basic structure and functionalities of SillyCode can be represented as follows:

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://github.com/robinhermit/ARCHIVE/blob/main/sillycode_diagram.png">
  <source media="(prefers-color-scheme: light)" srcset="https://github.com/robinhermit/ARCHIVE/blob/main/sillycode_diagram.png">
</picture>

## Requirements
For this version you'll need SuperCollider, Python and Pyphen installed.
If you haven't already, download and install SuperCollider: https://supercollider.github.io/
Then 
Then, install Pyphen by running `pip install pyphen` on your terminal.

