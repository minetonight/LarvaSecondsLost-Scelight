# goal
Select multiple replays, find the most common zerg player(s), up to two players, in the replays and display table(s) with the averaged stats for the games. 

## implementation stages
In order to add the new subpages we need major rework of the UI: 
 
### The current menu item "Larva" 
This is the central hub with buttons only. The single replay analysis and the multiple replays are opened in new sub-menu items and windows.

There are only buttons, centered in the page and one below another:
 - open replay
 - analyze latest replay
 - Multy analysis (a question mark icon with tooltip explains that one must select multiple replays and will receive aggregated stats over them); next to the button there is a checkbox checked by default to "ladder games only".

### the old larva replay overview
It is kept as it is, even the three buttons are kept there. the only change is that the two buttons that open replays open them in new sub-menu items, as if the user has navigated to the central hub Larva menu item.
The sub-menu item is called with the name of the replay file.
That window is permanent and stays until closed manually by the user, there is an X button in top right hand corner, probably default for the scelight platform, that is enabled. That is a new feature that must be added.

### The new multy-analysis page
The multy-analysis window is rendered as a new window in the left scelight tree view, as a sub-item to the 'larva' main item
 - the window is permanent and stays until closed manually by the user, there is an X button in top right hand corner, probably default for the scelight platform, that is enabled.
 - a new request for aggregated analysis (button click in the larva hub) creates and opens new multy analysis page.
 - that all is similar behavior to the scelight current behavior for opening replays of the Replay Analyzers menu item.


#### The header of the new window is 
"Averaged larva stats for {players names listed with commas} for {n} replays from {start date} to {end date}"

The dates are parsed from within the replays, not from the files modification time.

#### the name of the sub-menu item 
It is "from dd.mm.yy to dd.mm.yy" short dates format in the user locale. that there is the BG locale and for US could be mm/dd/yy.   

#### stats needed

Filter out with the checkbox for ladder games only: 
ladder games only (automm) or all games; 
The type of the game is kept in the replay itself. for Ladder games it is autoMM, and the others are Private and some other strings.
Remove games with length less than 4 minutes;

after the user has selected multiple replays, find the most common zerg player(s), up to two players, in the replays and display table(s) with the averaged stats for the games. 

Make three tables for each matchup: ZvP, ZvT, ZvZ 
Table contents is the same as the current table: 
 - columns: early game, midgame, lategame, endgame
 - rows: inject uptime %; larva gen [lrv/htch/min]; larva gen ranking; larva missed [lrv/htch/min]; larva missed ranking; inject-missed larva [lrv/htch/min];
 - omit phase span row, show average phase length

Above these tables show a common aggregated table for all games analyzed with the same structure;

If the players are two, use second column, for one player, use only the left half of the window.


#### New stats to add:
in the multy analysis page only

Average time of: 
 - 100 supply used. When the population got to 100, not unlocked with overlords.
 - 66 drones made, all of them alive, there is already a method for counting these in the game phases functionality.
 - 80 drones made;
 - 200 supply, used. again, reached, not unlocked with overlords.


## assets and icons
Two new icons exist in the LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/icon folder for the replay that must receive new icon:
![alt text](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/icon/larva-replay-icon.png)

and the multy analysis page:
![alt text](../LarvaSecondsLostExtMod/src/hu/aleks/larvasecondslostextmod/icon/larva-multy-analysis-icon.png) 

fixes in the epic:
 - OQ-10: add the function that auto opens a replay upon new replay detected.