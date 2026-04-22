1) injection indication
 - a green line below the hatchery rail for the time a hatch is injected
  * Q: how do we know if a hatch is injected in a replay, do we see the tag/property of the building that it is producing extra larva?
 - see base control tab in the original scelight code, injects, for implementation inspiration
 - idle inject queens indication - red lines below the rail for the periods when there are queen/s with 25+ energy, in the near vicinity of the hatchery and the hatchery is not injected.
 - count potential larva missed by missed injects via accumulation windows. the inject duration is 29s and the larva for each such window is 3 per hatchery. keep that in a separate variable.
 - show those missed inject larva in separate lines with green color, after the potential larva missed. 

## Why my injects are missed from the analysis
see (knowhow.md)[knowhow/knowhow.md]

### workaround 
 - dont track inject events at all
 - track moments where 3 larva spawn to a hatchery within 8 game loops
 - then retroactively create a 29 second window in the injection charts.
 - the current implementation for red missed inject windows are correct, keep them.

bugs:
 - queens missed in exactly one replay Gloria vs Sleepless.
 - fixing in epic11c-queensMissed.md

2) epic 12 - stats per game phase
game phase definitions:
    + early game up to 36 simultaneously alive drones for more than 30 seconds
    + mid game up to 66 simultaneously alive drones for more than 30 seconds
    + late game 67+ simultaneously alive drones for more than 30 seconds
    + end game 90+ simultaneously alive drones for more than 30 seconds

Measurements:
 - larva missed per phase, measured in larva per hatch per minute
 - injects missed per phase, measured in larva per hatch per minute
 - total larva spawned per phase (both natural spawn and injects spawns) , measured in larva per hatch per minute
 - injection uptime % - start of the global timer is 36 seconds after the finish of the spawining pool building that unlocks queens. then from that time point or the completions of a hatchery until the destruction of hatch, measure what cummulative time the hatch is being injected vs not, and calculate %.

 UI - use the row just under the player name and use the full len of the screen, 
 there make a table with 4 rows + header and four columns

3) epic 13 define benchmarks - analyse many replays and find what is a good measurement for a player. 
 - research how spending quotient metric was created and see if any of that can be used for our case. 
Statistical analysis phase, ai agent must find and download:
 - bunch of replays of Serral and Raynor (7000 mmr) (100 ZvT, 100 ZvP, 100 ZvZ)
 - replays on 5000mmr (100 ZvT, 100 ZvP, 100 ZvZ)
 - replays on 4000mmr (100 ZvT, 100 ZvP, 100 ZvZ)
 - replays on 3000mmr (100 ZvT, 100 ZvP, 100 ZvZ)
 - replays on 2000mmr (100 ZvT, 100 ZvP, 100 ZvZ)
Batch process the replays and find averages per phase. Look for correlations between metrics and skill levels. Define ranges per skill for the metric/s.

3.5) fix the stats ranges, they are wrong
 - off by 50%
 - add total larva generated per player in a ZvZ matchup explicitly 


4) epic 14 - player stats window
 - use "toon of favored player" from scelight settings if set
 - table with N replays, 6 columns for the phases
 - chart over time

5) opening a replay opens it in a new window in the tree, like the matches in the main program
