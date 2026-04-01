# goal
make a scelight extension mod that calculates and visualizes the time windows when a hatchery has 3+ larva.
the visualization is similar to the base control view that shows injected hatcheries. a red rectangle starts when a hatchery has more than 3 larva and ends when the hatchery has less than three larva. a different line for each hatchery created in the game, and separated by player.

# key assumptions
a larva unit is available in a replay. a larva belongs to a hatchery in the replay or a larva has coordinates and is close within one grid unit to an existing hatchery coordinates. 

# resources 
see folder knowhow. use java 7 syntax strictly.

# success criteria 
a new view choosable in the `chart` dropdown as "larva", and it shows a graph as described in the goal.

