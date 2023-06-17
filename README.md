# SillyCode
live-coding interface for real-time text sonification
#### This version of SillyCode works with any Pyphen language: https://pypi.org/project/pyphen/ 

SillyCode is a live coding environment based on SuperCollider, working as a real-time sonification of plain text. It allows to sequence up to 10 parallel loops, where a user can write by typing basic literal text in Italian (`main` branch of this repository) or any other language contained in Pyphen (in the `multi-lang` branch). In addition, with a minimalistic syntax consisting of punctuation, symbols and short commands it is possible to insert swings, fills and selectively execute the loops.

The basic structures of SillyCode can be represented as follows:

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://github.com/robinhermit/ARCHIVE/blob/main/sillycode_diagram.png">
  <source media="(prefers-color-scheme: light)" srcset="https://github.com/robinhermit/ARCHIVE/blob/main/sillycode_diagram.png">
  <img alt="Shows an illustrated sun in light mode and a moon with stars in dark mode." src="https://user-images.githubusercontent.com/25423296/163456779-a8556205-d0a5-45e2-ac17-42d089e3c3f8.png">
</picture>

## Requirements
For this version you'll need SuperCollider, Python and Pyphen installed.
Then, install Pyphen by running `pip install pyphen` on your terminal.

