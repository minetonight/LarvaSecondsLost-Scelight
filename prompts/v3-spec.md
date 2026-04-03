1) injection indication
 - a purple line below the hatchery rail for the time a hatch is injected
  * Q: how do we know if a hatch is injected in a replay, do we see the tag/property of the building that it is producing extra larva?
 - see base control tab in the original scelight code, injects, for implementation inspiration
 - idle inject queens indication - red lines below the rail for the periods when there are queen/s with 25+ energy, in the near vicinity of the hatchery and the hatchery is not injected.
 - count potential larva missed by missed injects via accumulation windows. the inject duration is 29s and the larva for each such window is 3 per hatchery. keep that in a separate variable.
 - show those missed inject larva in separate lines with purple color, after the potential larva missed. 

2) stats per game phase
 - larva lost per phase
 - injection uptime %
  * 
 - larva generated from injects per phase 

3) player stats
 - use main id from scelight settings if set
 - table with N replays, 6 columns for the phases
 - chart over time

4) opening a replay opens it in a new window in the tree, like the matches in the main program
