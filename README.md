# Core Clash

Core Clash is a local two-player platform fighter built with Java and Swing. Duel a friend or team up against Foreman B.O.L.T. or the Cinder Queen.

## Build and run

Use JDK 17 or newer. Open PowerShell in the project folder and run:

```powershell
javac -d out (Get-ChildItem src -Filter *.java).FullName
java -cp out Main
```

The game opens in borderless fullscreen. Use `java -cp out Main --windowed` to start in a window, or add `--weapon-demo` to go straight to a duel with the Shock Baton and Gravity Hammer equipped.

## Choose a mode

- **Local Duel (1)**. Fight the other robot and compete for mystery chests around the arena.
- **Overtime Raid (2)**. Both robots fight Foreman B.O.L.T. and can continue as long as one teammate is still up. Attack when the HUD says **Core exposed**, and use his armored stretches to take out interns.
- **Ember Hive (3)**. Hunt the Cinder Queen across nine forest ledges. Both players start with Spark Wands. Tap jump again in midair to climb, and dash through incoming fire.

Click a mode, press its number, or use **Left / Right** to select and **Enter** to start.

### Fighting the Cinder Queen

Climb to her flight path and fire horizontally into her body. You can damage her throughout the fight. After three waves she overheats, glides low for a little over two seconds, and takes double damage. Watch for fire still crossing the clearing as she descends.

| Wave | Counterplay |
| --- | --- |
| Stinger Fan | Dotted sights show the direction of the next spread. Change height after she takes aim, or dash across it. |
| Firestorm | Golden columns mark incoming meteors. Move into a gap and watch where the next row is marked. At one-third health she adds aimed fans. |
| Pinwheel | Streams of cinders spiral out from the moving queen. Give yourself space to slip between the spokes. Later phases add spokes and shorten the interval between volleys. |

### Overtime Raid attack patterns

| Attack | How to dodge |
| --- | --- |
| Floor Surge | Watch for the shockwave to travel from the foreman and jump as it reaches you. |
| Fire Bolt | The dotted line locks onto the closest living robot. Step off the line or dash into the bolt to reflect it for 18 damage, even through armor. |
| Forked Flare | From phase two, a Fire Bolt is followed by a fresh lock and two angled shots. The gap between the dotted lines widens with distance. Each shot can be reflected. |
| Punch Clock | Leave the first pair of columns, then keep moving. The second pair locks onto the players' new positions. |
| Paper Jam | Watch both heights as stamped cards cross from opposite sides. Jump the low cards and stay below the cards skimming the side platforms. |
| Core Beam | Move above or below the horizontal warning band before it fires. |
| Grid Overload | After every two regular attack sequences, alternating blue columns fire from ceiling to floor. Step into an unmarked lane, then cross back after each flash. Expect four pulses in phase one, five in phase two, and six in phase three. The core opens after the last pulse. |
| Polarity Reversal | Jump over the floor strike, then land before the upper strike hits. The core opens after the second strike. |
| Intern charge | From phase two, the foreman calls in smaller bots. Jump over their marked charges or destroy the bots with your weapon. |

## Controls

| Player | Move | Jump | Attack | Dash |
| --- | --- | --- | --- | --- |
| Blue | A / D | W | F | Q |
| Red | Left / Right arrows | Up arrow | Enter | / |

Press **M** to return to the mode screen. After the match, press **R** for a rematch or raid retry. Press **F11** to toggle borderless fullscreen or **Esc** to return to windowed mode.

Hold toward an outer wall while falling to slide. Press jump while holding toward the wall to leap away.

**Cross Beam.** Start a dash, then tap attack as the white chest light flashes. The robot brakes, crosses its arms, and fires straight ahead for 26 damage. An early tap uses the equipped weapon. In the raid, line up with the exposed core or save your dash to reflect a bolt. In phase three, Forked Flare leads directly into Core Beam before the core opens.

All modes spawn mystery chests at random floor and platform locations. The first arrives after two seconds, and the next arrives six seconds after pickup at a different spot. A chest can equip a weapon or repair a damaged robot.

The first chest holds a **Spark Wand**. Tap your usual attack key while facing your target. Its fast, straight shot is good for a floor duel. The **Cinder Staff** throws a high arc that can catch someone on a ledge. The **Gyro Axe** throws a slower, heavier spinning blade with a shorter reach. Shots pass through the one-way platforms. In a raid they can hit an intern or the exposed core, so your teammate can cross your line of fire.

Two low steps flank the boss, and two high balconies sit above the side platforms. Jump through a ledge from below to land on it, or walk off its edge to drop to the lane underneath.

## Progress and credits

- [Build milestones and playtesting](PROGRESS.md)
- [Asset credits](assets/CREDITS.md)
