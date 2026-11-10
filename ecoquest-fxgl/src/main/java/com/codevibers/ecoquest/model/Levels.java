package com.codevibers.ecoquest.model;

import java.util.List;

import com.codevibers.ecoquest.model.LevelConfig.Mode;

public final class Levels {
    private static final int INF = 1_000_000_000;

    public static final List<LevelConfig> CAMPAIGN = List.of(
            new LevelConfig("Residential Area", "Easy", "Sprout: Maple Street is buried in litter!", "Sort it all before the smog settles in.")
                    .rate(3.4, 6, .12, 150).m(10, 0, 0).hz(0, 0, 0, 0, 0).scene("house").tip("Pick up garbage (A), drop it in the matching bin."),
            new LevelConfig("Public Park", "Medium", "Sprout: The park lost its trees.", "Plant seeds and keep the grounds litter-free.")
                    .rate(2.8, 8, .2, 350).m(0, 10, 0).hz(0, 0, 0, 1, 0).scene("park").tip("Carry seeds to empty spots. Leftover garbage pollutes faster!"),
            new LevelConfig("River", "Hard", "Sprout: Dumpers are poisoning the river!", "Fish out waste, empty full bins, refuse factory bribes.")
                    .rate(2.3, 9, .28, 600).m(0, 0, 15).hz(5, 16, 1, 0, 1).scene("river").tint(255, 130, 50, .2).tip("Bins hold 5: press SPACE beside one to empty it."),
            new LevelConfig("Industrial Zone", "Expert", "Sprout: The final test. Smog, rain and dumping.", "Use every skill you learned, Guardian.")
                    .rate(1.5, 11, .4, 900).m(20, 5, 6).hz(4, 9, 1, 1, 1).scene("ind").tint(15, 25, 85, .4).tip("Everything at once, and faster. Chain combos!"),
            new LevelConfig("Market Rush", "Hard+", "Sprout: Market day, and the bins keep swapping!", "Stay sharp and read every label.")
                    .rate(1.4, 11, .34, 1100).m(30, 0, 0).hz(4, 10, 1, 0, 0).shuf(25).scene("market").tip("Bins swap places every 25 seconds."),
            new LevelConfig("Sunny Beach", "Expert+", "Sprout: The tide brought in tons of waste.", "Clean the shore and replant the dunes.")
                    .rate(1.3, 12, .38, 1300).m(0, 6, 20).hz(4, 9, 1, 1, 1).cur(32).scene("beach").tip("Strong currents carry waste along fast."),
            new LevelConfig("Toxic Landfill", "Insane", "Sprout: The last great cleanup.", "Swaps, currents, rain, bribes. Good luck, Guardian!")
                    .rate(1, 14, .5, 1700).m(40, 8, 10).hz(3, 7, 1, 1, 1).cur(24).shuf(20).rain().scene("dump").tint(20, 30, 70, .35).tip("Everything is against you. Stay calm."),
            new LevelConfig("Arctic Outpost", "Brutal", "Sprout: Trash even reached the ice!", "Melting floes carry waste in every direction.")
                    .rate(.95, 14, .55, 1900).m(35, 6, 12).hz(3, 7, 1, 1, 1).cur(20).shuf(18).scene("arctic").tint(150, 200, 255, .15).tip("Cold, fast and swap-happy. Keep your combo."),
            new LevelConfig("Jungle Delta", "Nightmare", "Sprout: The delta is choking on plastic.", "Save the rainforest before the rains return.")
                    .rate(.9, 15, .6, 2200).m(40, 8, 15).hz(3, 6, 1, 1, 1).cur(28).shuf(15).rain().scene("jungle").tip("Dense, wet and relentless."),
            new LevelConfig("Mega City", "Legend", "Sprout: One last place: the Mega City.", "Millions of people. Show them how it is done!")
                    .rate(.8, 16, .7, 2800).m(50, 10, 15).hz(3, 5, 1, 1, 1).cur(30).shuf(12).rain().scene("city").tint(20, 10, 60, .3).tip("The final exam. Everything, all at once."));

    public static final List<LevelConfig> MODES = List.of(
            new LevelConfig("Time Attack", "90 seconds on the clock", "Sprout: Beat the clock!", "Sort as much as you can in 90 seconds.")
                    .mode(Mode.TIME, 0).rate(.9, 12, .04, INF).hz(6, 0, 0, 0, 0).scene("market").tip("Chain combos for big points!"),
            new LevelConfig("Endless Survival", "Survive as long as you can", "Sprout: The waste never stops.", "Everything speeds up. Survive!")
                    .mode(Mode.ENDLESS, 1).rate(3, 7, .15, INF).hz(5, 20, 1, 0, 1).scene("river").rain().tint(255, 130, 50, .2).tip("Spawns and pollution ramp up over time."),
            new LevelConfig("Chaos Mode", "Bins swap constantly", "Sprout: Nothing stays where it should!", "Read the bin labels every time.")
                    .mode(Mode.ENDLESS, 2).rate(2, 9, .28, INF).hz(3, 9, 1, 0, 1).shuf(10).scene("dump").rain().tint(15, 25, 85, .4).tip("Bins shuffle every 10 seconds."),
            new LevelConfig("Speed Sort", "Sort 25 items, fastest time wins", "Sprout: How fast can you sort?", "Sort 25 items as quickly as you can.")
                    .mode(Mode.SPRINT, 3).rate(1, 10, .03, INF).hz(6, 0, 0, 0, 0).scene("market").tip("Combos make you faster!"),
            new LevelConfig("Zen Garden", "No pressure, just recycling", "Sprout: Breathe. Sort. Enjoy the view.", "Pollution never rises. Pause (P) to finish.")
                    .mode(Mode.ZEN, 4).rate(2.2, 7, 0, INF).hz(0, 0, 0, 0, 1).scene("park").tint(255, 170, 90, .12).tip("Row the boat (B) and enjoy the sunset."));

    public static final String[][] UPGRADES = {
            {"Speed Boots", "Walk faster"}, {"Big Gloves", "Carry 1 more item per level"}, {"Eco Scanner", "Longer reach + bin hints"},
            {"Turbo Boat", "Row faster on the river"}, {"Eco Drone", "Auto-cleans litter every few seconds"}, {"Lucky Clover", "More and longer Eco Boosts"}};

    private Levels() { }
}
