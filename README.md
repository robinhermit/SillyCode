# SillyCode Matrix
## live-coding interface for real-time text sonification
version of SillyCode running alongside an Arduino-based, 13 rows, 15 columns, velostat-based pressure sensors matrix.

## Instructions
With Python3, SuperCollider, PyPhen library installed;
with Arduino Mega and a velostat-based, conductively-threaded sweater with 15 columns and 14 rows (distributed as you feel like);

1) Plug Arduino Mega and flash 'matrix_sweater.ino' on the board.
2) open 'SILLYCODE_MTX_MULTI.sc' and 'ctrl_SILLYCODE_MTX_MULTI.scd' in SuperCollider.
3) execute the former by clicking anywhere in the code and hitting cmd+enter; 'a Function' should post on the SC console.
4) then run in order the first and then the second line of 'ctrl_SILLYCODE_MTX_MULTI.scd' making sure to see "Questo, come esempio" in the post window.
5) run line 3 '~cc.();' and give some time to the program to boot the server. BlackHole is selected by default, but go to block ~cc in the 'SILLYCODE_MTX_MULTI.sc' file to change it.
6) lastly run line 4 '~dd.();'; you should see the SillyCode text terminal appear. At this point an error might usually spawn, but no worries, follow the standard troubleshooting sequence below.

Visualise on the SillyCode terminal the pads on the sweater and the standard SillyCode text GUI.

## classic reboot / troubleshooting sequence
to make sure SillyCode doesn't crash, if you make any edit to the code and/or the code crashes, do as follows:
- cmd+period to stop SuperCollider.
- make edits or re-execute 'SILLYCODE_MTX_MULTI.sc'
- execute '~cc.();' again in 'ctrl_SILLYCODE_MTX_MULTI.scd' to reboot SC server
- execute '~dd.();' again in 'ctrl_SILLYCODE_MTX_MULTI.scd' to reboot SillyCode

This should prevent other weird errors to spawn.
