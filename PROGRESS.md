# Core Clash progress

- [x] **Grid Overload.** Add recurring full-height raid volleys with alternating safe lanes and a faster cadence in later phases.

Core Clash is a local two-player Java Swing platform fighter with head-to-head and co-op modes. See [README.md](README.md) for launch instructions, controls, and mode rules.

## Build checklist

- [x] **Movement.** Draw one arena and two robots. Give each player separate controls and keep both robots inside the walls.
- [x] **Basic combat.** Draw directional attacks, show health bars, enforce attack cooldowns, and stop the round at zero health.
- [x] **Interchangeable weapons.** Add the Pulse Blade, Shock Baton, and Gravity Hammer. Give each weapon its own damage, reach, arc, and cooldown. Show the equipped weapon and provide a way to test both new weapons.
- [x] **Robot and weapon art.** Show blue and red mechs with a visible facing direction, display each equipped weapon beside its health bar, and record the asset sources in [the credits](assets/CREDITS.md).
- [x] **Round result and rematch.** Show the winner or double knockout over the arena. Press R after the round to restore both robots' health, spawn positions, and starting weapons.
- [x] **Arena and HUD polish.** Add team-colored robot outlines, readable health and weapon cards, keyboard hints, and a marked arena floor.
- [x] **Combat animation.** Animate each weapon's swing in its facing direction. Show sparks or a shockwave when it hits and let the final impact finish before showing the round result.
- [x] **First supply chest.** Show a two-second countdown, equip the chest's weapon on contact, and reset the drop on rematch.
- [x] **Recurring supply chests.** Spawn the next chest six seconds after pickup. Cycle through Shock Baton, Gravity Hammer, and Repair Kit.
- [x] **Dash.** Give each player a short directional burst on a 1.8-second cooldown. Show the dash trail, readiness, and remaining cooldown.
- [x] **Repair kits.** Restore up to 25 HP while keeping the current weapon. Leave the kit available for a damaged robot.
- [x] **Scripted playtesting.** Check an opening double knockout, a full supply rotation, dashes, range differences, repair, a winner, and the rematch through the game controls. Move the first drop earlier so it appears during a fast opening duel.
- [x] **Borderless fullscreen.** Fill the display by default, preserve the arena's aspect ratio, and restore the previous window size when switching back.
- [x] **Side-view physics.** Add gravity, jumping, air dashes, and platforms that robots pass through upward and land on while falling. Verify both routes to the upper chest and a complete round through the regular controls.
- [x] **Mode selection.** Open a menu with Local Duel and Sentinel Raid. Support mouse selection, keyboard selection, and returning to the menu with M.
- [x] **Sentinel Raid.** Add a cooperative boss with its own health bar, team victory or defeat, and retry.
- [x] **Sentinel overhaul.** Add the licensed mech sprites, three attack phases, Twin Lock columns, the two-part Polarity Reversal, and exposed-core openings. Verify that dodging and attacking the exposed core clears the raid while standing in the attacks loses.
- [x] **Overtime Raid.** Replace the Sentinel with the pixel-art Foreman B.O.L.T. and add Punch Clock's re-locking second volley and destroyable Intern Bots. Verify all phases, minion charges, victory, and retry.
- [x] **Animated raid patterns.** Add windups, recoil, lightning discharges, traveling shockwaves, and Paper Jam cards in two lanes. Verify that moving hazards deal damage on contact and that the co-op raid remains beatable.
- [x] **Roomier arena and compact HUD.** Shrink the mechs, move dash meters into the player cards, and extend the arena through the former key guide. Hide chest contents and randomize drops among seven floor and platform locations.
- [x] **Clean raid interface.** Remove attack-name popups and rasterize the HUD, menu, and results at display resolution.
- [x] **Platform combat update.** Add wall sliding and wall jumps, a wider platform layout, a city backdrop, and the Foreman's horizontal Core Beam.
- [x] **Sharper graphics.** Render the HUD and arena at higher resolution, cache the static arena, and adjust the resolution budget on very large displays. Add armor detail, clearer hazard markings, and a reactor backdrop.
- [x] **Running animation.** Use distance traveled to time each stride. Keep the planted foot fixed on the ground while the other swings forward.
- [ ] **Two-player playtesting.** Play both sides on the demo keyboard and compare how the weapons feel over several matches.
- [x] **Ranged combat and raid counters.** Add three ranged chest weapons, four one-way ledges, aimed Fire Bolts, a forked follow-up, and dash reflection.
- [x] **Dash techniques.** Add the timed Cross Beam brake, increase run and dash speeds, and chain the final-phase flare into Core Beam.
