0) bug:
"on windows my charts end 20-ish % before the game end, what could be the reason?"

proposal:
the bug is:

the chart end loop is being computed from real-time milliseconds as if they were raw game-time milliseconds.
Practical conclusion:

the chart is truncated because resolveVisibleEndLoop() is using the wrong time basis
the replay data itself is likely still there
the cutoff point in your dump strongly matches Faster speed scaling, not random rendering loss
Best fix:

stop deriving end loop from replayLengthMs
use replay end loop directly from the replay/header, or pass Scelight-consistent loop/time conversion both ways


1) revisit: 

bug reported:
"the window start is 0:48. the message is near future 0:36. what is off? debug the times"

proposed solution:
Added Scelight-consistent loop-to-time conversion to LarvaAnalysisReport.java
Passed replay time conversion settings from LarvaReplayAnalyzer.java
Rebuilt window/marker display times in LarvaTimelineModelBuilder.java using the same time basis as Scelight


2) related bug:
currently the larva missed lines are demonstrated every 8 seconds, why? the correct time is the equivalent of 11 seconds but in loops.


3) new feature:

finer time measurements:
use the full available resolution in loop and sum loops with 3+ larva on hatch. currently there might be rounding up errors that punish the player stats. show the times on the tooltip with 10th of a second resolution.

4) see #file:LarvaSecondsLost-dev-dump.txt . i read that i got a line for missed larva only 5 seconds into the accumulation. is that so:
looking at first hatch
.3 sec on start, 
1.4 sec on 14/14
3.17.9 - 3.11.7 = 6.2sec
first larva moment is 3:17.9 and total time 3+ larva is 7.9 sec.
so we still have the issue with bad times. 
analyse and fix.