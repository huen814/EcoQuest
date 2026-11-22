# EcoQuest: Guardians of the Green City

A 2D waste-sorting game built with Java, JavaFX and FXGL. Play as Lena or Adam, a sibling whose messy twin was kidnapped by the evil Queen Muck. Walk around tile-based levels, pick up litter, sort it to the right bins, row a boat to clean the river, plant saplings, and fight your way through all three bosses to rescue your sibling in Queen Muck's palace.

> **Status:** Finals group project by group *codevibers* for Object Oriented Programming. Work in progress.

## Requirements
- JDK 21
- Maven

## Run
```bash
cd ecoquest-fxgl
mvn javafx:run
```
Optional: `python3 tools/gen_sprites.py` regenerates all the art (needs Pillow: pip install pillow) Run it from ecoquest-fxgl. You only need this if you want to change the textures.

## Story and Game Flow
```
Intro cutscene -> Campaign levels -> Boss fights -> Mother Nature -> Palace -> Queen Muck
                       |
                       +-> XP -> level-up -> attribute choices
                       +-> coins -> Cosmetics shop
```
1. **Story:** pick a sibling (Lena or Adam). The other one is kidnapped. Sprout guides you, and Mother Nature names you a Guardian of the Green City. Cutscenes play between stages.
2. **Campaign (10 levels):** Residential Area, Public Park, River, Industrial Zone, Market Rush, Sunny Beach, Toxic Landfill, Arctic Outpost, Jungle Delta, Mega City.
3. **Bosses:** triggered by campaign progress (see Bosses below).
4. **XP and attributes:** sorting, planting, fishing and missions give XP. Each level-up (max level 30) offers 3 attribute choices.
5. **Coins and shop:** correct sorts earn coins, which you spend on cosmetics.

## Features
- **Campaign:** 10 levels with rising difficulty (swapping bins, currents, rain, factory bribes).
- **Modes:** Time Attack, Endless Survival, Chaos Mode, Speed Sort, Zen Garden.
- **XP and attributes (11):** Swift Boots, Strong Gloves, Eco Scanner, Sturdy Boat, Helper Drone, Lucky Clover (primary) and Coin Purse, Fresh Air, Steady Hands, Green Thumb, Quick Learner (secondary). Replaying a cleared level gives reduced XP.
- **Cosmetics shop:** outfits (unique designs per sibling), hair colors and boat paint.
- **Cutscenes:** intro, grove, kidnapping, transformation, palace.

### Bosses
| Boss | When | Type | Attacks |
|---|---|---|---|
| Muck Mound | After level 3 | Fight | Gunk Shot, Sludge Bomb, slime rain, puddles, sludge ring |
| The Experiment | After level 6 | Escape chase (cannot be beaten) | Junk cubes to shake it off, stink clouds slow you down, ghosts |
| Queen Muck | After the last level | Fight (final boss, rage phase) | Bottle Caps (ricochet), Rusted Rebar & Pipes, Rolling Tires, Leaky Car Batteries, Tied Trash Bags |

## Controls
| Key | Action |
|---|---|
| Arrows / WASD | Walk (also moves you in boss fights and the chase) |
| E / Z / Enter | A button (pick up / drop / sort / plant). In boss fights: shoots the Spirit Bottle. After dying: retry |
| Space | Empty bin |
| B | Boat |
| P / Esc | Pause (also the PAUSE button, top right) |
| X | Back in menus. In boss fights: quit to menu |
| M | Mute |

## Project Layout
- `model/`: pure Java game rules on a 15x10 tile grid (no JavaFX). `GameSession` holds the whole game state. Also `Levels` (campaign and modes), `Progression` (XP and level-ups), `Attribute`, `Cosmetics` and `Save`.
- `story/`: `StoryState`, the story wording and art shared by the cutscenes and boss fights.
- `view/`: FXGL/JavaFX screens.
  - Gameplay: `PlayScreen`, `MapView`, `Hud`, `DialogBox`, `MenuBox`.
  - Menus: `MainMenuScreen`, `LevelSelectScreen`, `ModesScreen`, `ShopScreen`, `Wardrobe`, `AttributesScreen`, `AttributeChoiceScreen`, `HowToScreen`, `CharacterSelectScreen`, `ConfirmNewGameScreen`.
  - Story and bosses: `IntroScreen`, `GroveCutsceneScreen`, `KidnapCutsceneScreen`, `PalaceCutsceneScreen`, `TransformationScreen`, `BossStoryScreen`, `BossDef`, `BossFightScreen`, `ChaseScreen`.
- `EcoQuestApp` + `InputState`: window setup, screen switching, keyboard input.
- `tools/`: art and music generators.
  - `gen_sprites.py`: generates the textures (clean and dirty sprite sheets for both siblings, outfits, items, bins, tiles, backdrops, bosses). You can swap in hand-made PNGs with the same name and size.
  - `gen_palace_hall.py`: the palace hall backdrop.
  - `mother_nature_gen.py`: the Mother Nature sprite.
  - `gen_chase_music.py`: the chase music (`assets/music/chase.wav`).
  - `ProgressionCheck.java`: checks the XP and level-up rules.

## Optional: Pixel Font
Drop `PressStart2P.ttf` into `src/main/resources/assets/ui/fonts/` for a more retro look. Without it the game uses bold monospace.

## Testing and Development Status
- **Model layer:** compiled and fuzz-tested headlessly on all 15 levels/modes (10 campaign levels + 5 modes). A greedy bot clears level 1 with 3 stars. XP and level-up rules are covered by `tools/ProgressionCheck.java`.
- **View layer:** runs with FXGL 21.1 via `mvn javafx:run` / IntelliJ and has been playtested by the author.
- **Not done:** gamepad/mouse support, balance tuning (movement is tile-based, so speeds and reach differ from the earlier Swing prototype), animated clouds/factory smoke, background music (only the chase theme exists).

## Disclaimer
- Parts of the code were AI-assisted.
- This project is **not finished**. More features, levels and polish will be added over time.
- It currently runs from the terminal with `mvn javafx:run` (or from an IDE). A standalone version that can be played outside an IDE is planned.
- All art and the chase music are generated by the scripts in `tools/`, so the textures are placeholders that may be replaced with hand-made art later.
- FXGL and JavaFX are third-party libraries and belong to their respective authors and licenses.
