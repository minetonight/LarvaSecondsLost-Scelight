# Scelight

## how to make a plugin/module
https://sites.google.com/site/scelight/external-modules

And in folder knowhow/making external modules.md

## source code
in folder scelight and see readme.md

## java api version
For the sake of compatibility and portability it is highly recommended to use only the Java 7.0 API (and not newer versions).

# scelight injects bug

Result:

sc2reader does see the missing early injects.
Current Scelight/module output still shows only 2 direct green injects in that window.
What sc2reader saw

Using the equivalent raw-time window of about 208-222 seconds for the displayed Faster-speed window 02:30-02:40, sc2reader found 5 SpawnLarva command rows:
Serral: 2 targets
Reynor: 3 rows, but only 2 unique targets
Collapsed to unique targets, that is exactly 4 inject targets:
Serral: 2 unique hatcheries
Reynor: 1 hatchery + 1 lair
The key detail is that some of them are UpdateTargetUnitCommandEvent, not just plain TargetUnitCommandEvent. sc2reader explicitly models that in game.py:361-371.

The actual sc2reader hits were:

Serral:
214.5625s TargetUnitCommandEvent
215.6875s UpdateTargetUnitCommandEvent
Reynor:
217.5625s TargetUnitCommandEvent
218.1875s UpdateTargetUnitCommandEvent
218.875s UpdateTargetUnitCommandEvent

The missing injects are the queued retarget/update commands.
sc2reader sees them as UpdateTargetUnitCommandEvent.
Scelight’s public game-event API only exposes ID_CMD in IGameEvents.java:23-29, and the factory only constructs CmdEvent for that path in GameEventFactory.java:61-66.
But the bundled protocol definitions clearly know about SCmdUpdateTargetUnitEvent, so this is very likely the missing path.

