package com.codevibers.ecoquest.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.codevibers.ecoquest.model.LevelConfig.Mode;
import com.codevibers.ecoquest.model.TileMap.Tile;

/**
 * All game rules, on a tile grid. No JavaFX in here: the view reads state through the getters
 * and reacts to {@link SessionListener} events.
 */
public final class GameSession {
    private static final WasteType[] WASTE = WasteType.values();
    private static final int TILE_PX = 48;

    private final LevelConfig cfg;
    private final int levelIndex;
    private final Save save;
    private final Random rng;
    private final TileMap map;
    private final Player player = new Player(7, 5);
    private final List<Pickup> items = new ArrayList<>();
    private final List<Bin> bins = new ArrayList<>();
    private final List<PlantingSpot> spots = new ArrayList<>();
    private final List<String> mistakes = new ArrayList<>();
    private SessionListener listener = new SessionListener() { };

    // live-tuned copies (endless mode ramps these)
    private double spawnRate;
    private double pollutionRate;
    private double maxItems;

    private double time;
    private double timeLeft = 90;
    private double shuffleT;
    private double spawnT = 1;
    private double truckT = 20;
    private double truckDropT;
    private double droneT;
    private double dumpT;
    private double factoryT = 22;
    private double harmT = 8;
    private double boost;
    private double pollution = 15;
    private int score, money, sorted, planted, cleaned, combo, best, stars;
    private boolean missionDone, ended, won, offerPending;
    private boolean truckActive;
    private double truckCol;
    private boolean truckHit;
    private double truckWarnT;
    private double lowBridgeT;

    public GameSession(LevelConfig cfg, int levelIndex, Save save, Random rng) {
        this.cfg = cfg;
        this.levelIndex = levelIndex;
        this.save = save;
        this.rng = rng;
        this.map = new TileMap(cfg.hasRiver());
        this.spawnRate = cfg.spawn;
        this.pollutionRate = cfg.pp;
        this.maxItems = cfg.maxItems;
        this.shuffleT = cfg.shuffle;
        this.dumpT = cfg.dump;
        BinType[] types = BinType.values();
        for (int k = 0; k < 5; k++) bins.add(new Bin(types[k], TileMap.BIN_COLS[k], TileMap.BIN_ROW));
        int n = cfg.missionPlant;
        for (int k = 0; k < n; k++) {
            int col = n > 1 ? (int) Math.round(1 + k * 13.0 / (n - 1)) : 7;
            spots.add(new PlantingSpot(col, TileMap.SPOT_ROW));
        }
        for (int k = 0; k < 3; k++) spawnWaste(true, -1);
    }

    public void setListener(SessionListener l) { listener = l; }

    // ------------------------------------------------------------------ getters

    public LevelConfig config() { return cfg; }
    public TileMap map() { return map; }
    public Player player() { return player; }
    public List<Pickup> items() { return items; }
    public List<Bin> bins() { return bins; }
    public List<PlantingSpot> spots() { return spots; }
    public List<String> mistakes() { return mistakes; }
    public double pollution() { return pollution; }
    public int score() { return score; }
    public int money() { return money; }
    public int sorted() { return sorted; }
    public int planted() { return planted; }
    public int cleaned() { return cleaned; }
    public int combo() { return combo; }
    public int bestCombo() { return best; }
    public int stars() { return stars; }
    public double elapsed() { return time; }
    public double timeLeft() { return timeLeft; }
    public double boostSeconds() { return Math.max(0, boost); }
    public boolean missionDone() { return missionDone; }
    public boolean ended() { return ended; }
    public boolean won() { return won; }
    public int binCapacity() { return cfg.cap > 0 ? cfg.cap : 10; }
    public int carryCapacity() { return 1 + save.up[1]; }
    public boolean hasScanner() { return save.up[2] > 0; }
    public boolean truckActive() { return truckActive; }
    public double truckCol() { return truckCol; }
    /** Top row of the truck sprite; it is two tiles tall and covers both road rows. */
    public int truckRow() { return TileMap.ROAD_TOP; }
    /** True during the short warning before a truck enters its lane. */
    public boolean truckWarning() { return truckWarnT > 0; }

    /** Seconds one tile step takes right now (view uses it to glide the sprite). */
    public double moveSeconds() {
        return player.onBoat ? 0.30 / (1 + save.up[3] * .25) : 0.20 / (1 + save.up[0] * .15);
    }

    // ------------------------------------------------------------------ targets (the tile the Guardian faces)

    public Pickup targetPickup() {
        boolean boat = player.onBoat;
        int reach = 1 + (save.up[2] > 0 ? 1 : 0);
        for (int d = 1; d <= reach; d++) {
            Pickup p = itemAt(player.col + player.facing.dc * d, player.row + player.facing.dr * d, boat);
            if (p != null) return p;
        }
        return itemAt(player.col, player.row, boat);
    }

    public Bin targetBin() { return binAt(player.col + player.facing.dc, player.row + player.facing.dr); }

    public PlantingSpot targetSpot() {
        int c = player.col + player.facing.dc, r = player.row + player.facing.dr;
        for (PlantingSpot s : spots) if (s.col() == c && s.row() == r) return s;
        return null;
    }

    private Bin binAt(int c, int r) {
        for (Bin b : bins) if (b.col() == c && b.row() == r) return b;
        return null;
    }

    private PlantingSpot spotAt(int c, int r) {
        for (PlantingSpot s : spots) if (s.col() == c && s.row() == r) return s;
        return null;
    }

    private Pickup itemAt(int c, int r, boolean water) {
        for (Pickup p : items) if (p.col == c && p.row == r && inWater(p) == water) return p;
        return null;
    }

    /** Open water only. Litter lying on a bridge deck is ordinary land litter (walkable pick-up). */
    private boolean inWater(Pickup p) { return map.isBoatWater(p.col, p.row); }

    private boolean anyItemAt(int c, int r) {
        for (Pickup p : items) if (p.col == c && p.row == r) return true;
        return false;
    }

    // ------------------------------------------------------------------ movement

    public void turn(Direction d) { player.facing = d; }

    public boolean tryMove(Direction d) {
        player.facing = d;
        if (ended) return false;
        int c = player.col + d.dc, r = player.row + d.dr;
        // walk into the parked boat to get in
        if (!player.onBoat && map.river() && !map.boatBoarded && c == map.boatCol && r == map.boatRow) {
            boardBoat();
            return true;
        }
        // row into a free bank tile to get out (bridges are not a dock: the boat can't go under them)
        if (player.onBoat && map.at(c, r) != Tile.BRIDGE && landPassable(c, r) && !map.isRiverTile(c, r)) {
            dockTo(c, r);
            return true;
        }
        if (!(player.onBoat ? boatPassable(c, r) : landPassable(c, r))) {
            if (player.onBoat && map.isRiverTile(c, r) && map.at(c, r) == Tile.BRIDGE && lowBridgeT <= 0) {
                lowBridgeT = 4;
                toast("Low bridge!", "The boat can't pass under it. Dock and walk across.", false, 2.5);
            }
            return false;
        }
        player.col = c;
        player.row = r;
        if (player.onBoat) {
            map.boatCol = c;
            map.boatRow = r;
            autoScoop();
        }
        return true;
    }

    /** Open water only: bridge decks block the boat (it used to row straight over them). */
    private void boardBoat() {
        player.onBoat = true;
        map.boatBoarded = true;
        player.col = map.boatCol;
        player.row = map.boatRow;
        listener.onSound(Sfx.BOAT);
        toast("All aboard!", "Row over litter to scoop it. Row into the bank to get off.", true, 3);
    }

    private void dockTo(int c, int r) {
        player.onBoat = false;
        map.boatBoarded = false;
        player.col = c;
        player.row = r;
        listener.onSound(Sfx.BOAT);
        toast("Docked", "Walk into the boat to board it again.", true, 2.5);
    }

    /** In the boat, litter on your own tile is scooped up automatically while there is room in the hold. */
    private void autoScoop() {
        if (player.carried.size() >= carryCapacity()) return;
        Pickup g = itemAt(player.col, player.row, true);
        if (g != null) pickUp(g);
    }

    private boolean boatPassable(int c, int r) {
        return map.isBoatWater(c, r);
    }

    private boolean landPassable(int c, int r) {
        Tile t = map.at(c, r);
        if (t != Tile.GRASS && t != Tile.ROAD && t != Tile.BRIDGE) return false;
        return binAt(c, r) == null && spotAt(c, r) == null;
    }

    public void toggleBoat() {
        if (ended || !map.river()) return;
        if (player.onBoat) {
            int[][] tries = {{player.col + player.facing.dc, player.row + player.facing.dr},
                    {player.col, player.row == TileMap.RIVER_TOP ? TileMap.ROAD_BOTTOM : TileMap.BIN_ROW},
                    {player.col - 1, player.row == TileMap.RIVER_TOP ? TileMap.ROAD_BOTTOM : TileMap.BIN_ROW},
                    {player.col + 1, player.row == TileMap.RIVER_TOP ? TileMap.ROAD_BOTTOM : TileMap.BIN_ROW}};
            for (int[] t : tries) {
                if (landPassable(t[0], t[1])) {
                    player.onBoat = false;
                    map.boatBoarded = false;
                    player.col = t[0];
                    player.row = t[1];
                    listener.onSound(Sfx.BOAT);
                    toast("Docked", "Back on land. Cross the river on the bridges.", true, 2.5);
                    return;
                }
            }
            toast("Can't dock here", "Row next to a free bank tile and press B.", false, 2);
        } else if (Math.abs(map.boatCol - player.col) <= 1 && Math.abs(map.boatRow - player.row) <= 1) {
            boardBoat();
        } else {
            toast("The boat is over there", "Walk next to the boat and press B.", false, 2);
        }
    }

    // ------------------------------------------------------------------ actions

    public void interact() {
        if (ended) return;
        Pickup top = player.carried.isEmpty() ? null : player.carried.get(player.carried.size() - 1);
        Bin bin = targetBin();
        PlantingSpot spot = targetSpot();
        PlantingSpot freeSpot = spot != null && !spot.planted() ? spot : null;
        if (top != null && (top instanceof SeedItem ? (freeSpot != null || bin != null) : bin != null)) {
            useCarried(top, bin, freeSpot);
            return;
        }
        Pickup g = targetPickup();
        if (g != null && player.carried.size() < carryCapacity()) {
            pickUp(g);
            return;
        }
        if (top != null) drop(top);
    }

    private void pickUp(Pickup g) {
        items.remove(g);
        listener.onSound(Sfx.PICKUP);
        if (g instanceof BoostItem) {
            boost = 12 + 3 * save.up[5];
            toast("Eco Boost! x2 score for " + (int) boost + "s", "Pollution also grows slower.", true, 3.5);
            listener.onBurst(g.col, g.row, 0xffd23c, 18);
            listener.onSound(Sfx.BOOST);
            return;
        }
        player.carried.add(g);
        if (g.river) {
            cleaned++;
            score += 25;
            pollution = Math.max(0, pollution - 10);
            g.river = false;
            listener.onFloatText(g.col, g.row, "+25 river", 0x4fc3f7);
            toast("River cleaned! +25 Score", "Pollution -10. Now sort it into the right bin.", true, 3.5);
            listener.onBurst(g.col, g.row, 0x6ec6ff, 12);
        }
        checkMission();
    }

    private void drop(Pickup top) {
        int c = player.col, r = player.row;
        if (anyItemAt(c, r)) {
            c += player.facing.dc;
            r += player.facing.dr;
            boolean ok = player.onBoat ? boatPassable(c, r) : landPassable(c, r) && !map.isRiverTile(c, r);
            if (!ok || anyItemAt(c, r)) {
                toast("No room here", "Move to an empty tile to drop it.", false, 2);
                return;
            }
        }
        player.carried.remove(top);
        top.col = c;
        top.row = r;
        top.drift = 0;
        items.add(top);
    }

    private void useCarried(Pickup item, Bin bin, PlantingSpot spot) {
        if (item instanceof SeedItem) {
            if (spot != null) {
                player.carried.remove(item);
                spot.plant();
                planted++;
                score += 20 * (boost > 0 ? 2 : 1);
                pollution = Math.max(0, pollution - 3);
                toast("Tree planted! +20 Score", "Pollution -3. Trees clean the air.", true, 3.5);
                listener.onBurst(spot.col(), spot.row(), 0x5cd16a, 16);
                listener.onFloatText(spot.col(), spot.row() - 1, "+20", 0x9be37a);
                listener.onSound(Sfx.PLANT);
                checkMission();
            } else {
                toast("Seeds go in planting spots", "Find an empty spot to plant it.", false, 3.5);
            }
            return;
        }
        WasteItem w = (WasteItem) item;
        int cap = binCapacity();
        if (cfg.cap == 0 && bin.fill() >= cap) bin.setFill(0);
        if (bin.fill() >= cap) {
            toast("Bin is full!", "Press SPACE beside the bin to empty it.", false, 3.5);
            listener.onSound(Sfx.FULL);
            return;
        }
        player.carried.remove(item);
        if (w.bin() == bin.type()) {
            bin.setFill(bin.fill() + 1);
            sorted++;
            combo++;
            best = Math.max(best, combo);
            int mult = Math.min(4, 1 + combo / 3) * (boost > 0 ? 2 : 1);
            int pts = 10 * mult;
            score += pts;
            money += 3;
            pollution = Math.max(0, pollution - 5);
            listener.onBinLid(bin);
            toast("Correct! +" + pts + " Score   Pollution -5", w.bin().explanation, true, 3.5);
            listener.onFloatText(bin.col(), bin.row() - 1, "+" + pts + (mult > 1 ? " x" + mult : ""), 0xfff48a);
            listener.onBurst(bin.col(), bin.row(), bin.type().rgb, 14);
            listener.onSound(Sfx.CORRECT);
        } else {
            score -= 5;
            combo = 0;
            listener.onShake(.3);
            mistakes.add(w.type().label + " is not " + bin.type().label.toLowerCase() + " waste - it belongs in " + w.bin().label.toUpperCase());
            toast("Wrong bin! -5 Score", w.bin().explanation, false, 3.5);
            listener.onFloatText(bin.col(), bin.row() - 1, "-5", 0xff6a5e);
            listener.onBurst(bin.col(), bin.row(), 0x666666, 10);
            listener.onSound(Sfx.WRONG);
        }
        checkMission();
    }

    public void emptyBin() {
        if (ended) return;
        Bin b = targetBin();
        if (b == null || b.fill() == 0) {
            b = null;
            for (Direction d : Direction.values()) {
                Bin n = binAt(player.col + d.dc, player.row + d.dr);
                if (n != null && n.fill() > 0) { b = n; break; }
            }
        }
        if (b != null && b.fill() > 0) {
            b.setFill(0);
            listener.onBinLid(b);
            toast("Bin emptied", "Ready to accept more waste.", true, 3.5);
            listener.onSound(Sfx.EMPTY);
        }
    }

    /** Zen mode only: ends the run as a success. Returns false for every other mode. */
    public boolean finishZen() {
        if (cfg.mode != Mode.ZEN || ended) return false;
        endRun(true);
        return true;
    }

    public void answerFactoryOffer(boolean accept) {
        offerPending = false;
        if (accept) {
            money += 100;
            pollution += 15;
            score -= 10;
            toast("Accepted the factory deal", "+PHP 100, but Pollution +15 and Score -10.", false, 3.5);
        } else {
            score += 10;
            toast("Rejected the factory deal! +10 Score", "Integrity keeps the city clean.", true, 3.5);
        }
    }

    private void toast(String a, String b, boolean ok, double secs) { listener.onToast(a, b, ok, secs); }

    // ------------------------------------------------------------------ per-frame rules

    public void tick(double dt) {
        if (ended || offerPending) return;
        for (PlantingSpot s : spots) s.grow(dt);
        if (boost > 0) boost -= dt;
        if (lowBridgeT > 0) lowBridgeT -= dt;
        if (player.onBoat) autoScoop();
        time += dt;
        driftItems(dt);

        if (cfg.shuffle > 0 && (shuffleT -= dt) < 0) {
            shuffleT = cfg.shuffle;
            int a = rng.nextInt(5), b = rng.nextInt(5);
            if (a == b) b = (a + 1 + rng.nextInt(4)) % 5;
            BinType tmp = bins.get(a).type();
            bins.get(a).setType(bins.get(b).type());
            bins.get(b).setType(tmp);
            toast("Bins swapped!", "Check the labels before you sort.", false, 3);
            listener.onSound(Sfx.SWAP);
        }
        if (cfg.mode == Mode.TIME && (timeLeft -= dt) <= 0) { timeLeft = 0; endRun(true); return; }
        if (cfg.mode == Mode.SPRINT && sorted >= 25) { endRun(true); return; }

        updateTruck(dt);
        updateDrone(dt);
        if (cfg.mode == Mode.ENDLESS) {
            spawnRate = Math.max(.6, spawnRate - dt * .012);
            pollutionRate += dt * .0012;
            maxItems = Math.min(16, maxItems + dt * .02);
        }
        if ((spawnT -= dt) < 0) {
            spawnT = spawnRate * (.7 + rng.nextDouble() * .6);
            spawnItem(false);
        }
        long garbage = items.stream().filter(Pickup::isGarbage).count();
        pollution = Math.min(100, pollution + garbage * pollutionRate * dt * .35 * (boost > 0 ? .3 : 1));

        if (cfg.dump > 0 && (dumpT -= dt) < 0) {
            dumpT = cfg.dump * (.8 + rng.nextDouble() * .4);
            for (int k = 0; k < 3; k++) spawnWaste(true, -1);
            pollution += 10;
            score -= 15;
            combo = 0;
            listener.onShake(.4);
            toast("Illegal dumping! -15 Score", "Pollution +10. Clean it up fast!", false, 3.5);
            listener.onSound(Sfx.DUMP);
        }
        if (cfg.factory > 0 && (factoryT -= dt) < 0) {
            factoryT = 24 + rng.nextDouble() * 10;
            offerPending = true;
            listener.onFactoryOffer();
            return;
        }
        if (pollution >= 75 && (harmT -= dt) < 0) {
            harmT = 8;
            score -= 10;
            toast("Pollution too high! -10 Score", "Sort waste and plant trees to bring it down.", false, 3.5);
        }
        if (pollution >= 100) endRun(false);
    }

    /** Floating waste drifts east through the boat's stretch of river and re-enters at its west end (it passes under the bridge). */
    private void driftItems(double dt) {
        double speed = cfg.current > 0 ? cfg.current : 10;
        for (Pickup p : items) {
            if (!inWater(p)) continue;
            p.drift += speed * dt;
            while (p.drift >= TILE_PX) {
                p.drift -= TILE_PX;
                p.col = p.col + 1 > map.laneMax() ? map.laneMin() : p.col + 1;
            }
        }
    }

    private static final int TRUCK_PENALTY = 10;
    private static final double TRUCK_WARNING_SECONDS = 1.5;
    /** Truck sprite is 3 tiles wide and 2 tall (whole road); the hitbox is trimmed a little so near misses feel fair. */
    private static final double TRUCK_WIDTH = 3, TRUCK_HIT_TRIM = .25;

    private void updateTruck(double dt) {
        if (cfg.spawn >= 2.5) return;
        if (!truckActive) {
            if (truckWarnT > 0) {
                if ((truckWarnT -= dt) <= 0) {
                    truckWarnT = 0;
                    truckActive = true;
                    truckCol = -TRUCK_WIDTH;
                    truckDropT = 0;
                    truckHit = false;
                }
                return;
            }
            if ((truckT -= dt) < 0) {
                truckT = 18 + rng.nextDouble() * 10;
                truckWarnT = TRUCK_WARNING_SECONDS;
                toast("Truck coming!", "Stay off the road until it passes.", false, 2.5);
                listener.onSound(Sfx.HONK);
            }
            return;
        }
        truckCol += 140.0 / TILE_PX * dt;
        checkTruckHit();
        if ((truckDropT -= dt) < 0) {
            truckDropT = .5;
            int c = (int) Math.floor(truckCol + TRUCK_WIDTH / 2);
            if (c >= 0 && c < TileMap.COLS) spawnWaste(true, c);
        }
        if (truckCol > TileMap.COLS + 1) truckActive = false;
    }

    /** The truck hits the Guardian at most once per pass. */
    private void checkTruckHit() {
        if (truckHit || player.onBoat || (player.row != TileMap.ROAD_TOP && player.row != TileMap.ROAD_BOTTOM)) return;
        boolean overlap = player.col + 1 > truckCol + TRUCK_HIT_TRIM
                && player.col < truckCol + TRUCK_WIDTH - TRUCK_HIT_TRIM;
        if (!overlap) return;
        truckHit = true;
        score -= TRUCK_PENALTY;
        combo = 0;
        listener.onShake(.4);
        listener.onFloatText(player.col, player.row, "-" + TRUCK_PENALTY, 0xff6b6b);
        toast("Hit by the truck! -" + TRUCK_PENALTY + " Score", "Watch the road before you cross.", false, 3.5);
        listener.onSound(Sfx.DUMP);
    }

    private void updateDrone(double dt) {
        if (save.up[4] <= 0) return;
        droneT += dt;
        if (droneT < 10 - save.up[4] * 2) return;
        droneT = 0;
        Pickup best = null;
        double bd = Double.MAX_VALUE;
        for (Pickup p : items) {
            if (!p.isGarbage() || inWater(p)) continue;
            double d = Math.hypot(p.col - player.col, p.row - player.row);
            if (d < bd) { bd = d; best = p; }
        }
        if (best != null) {
            items.remove(best);
            score += 5;
            pollution = Math.max(0, pollution - 2);
            listener.onBurst(best.col, best.row, 0x88ffff, 10);
            listener.onFloatText(best.col, best.row, "drone +5", 0x99ffff);
        }
    }

    // ------------------------------------------------------------------ spawning

    private void spawnItem(boolean ground) {
        spawnWaste(ground, -1);
    }

    /** forcedCol >= 0: drop on that column of the road (used by the truck). */
    private void spawnWaste(boolean ground, int forcedCol) {
        if (items.size() >= maxItems) return;
        if (!ground && forcedCol < 0 && rng.nextDouble() < .05 + .03 * save.up[5] && items.stream().noneMatch(i -> i instanceof BoostItem)) {
            int[] t = freeTile(false);
            if (t != null) { items.add(new BoostItem(t[0], t[1])); return; }
        }
        long carriedSeeds = player.carried.stream().filter(i -> i instanceof SeedItem).count();
        long seedsOnMap = items.stream().filter(i -> i instanceof SeedItem).count();
        long need = cfg.missionPlant > 0 ? cfg.missionPlant - planted - carriedSeeds : 0;
        if (cfg.seeds > 0 && !ground && seedsOnMap < need && rng.nextDouble() < .3) {
            int[] t = freeTile(false);
            if (t != null) { items.add(new SeedItem(t[0], t[1])); return; }
        }
        boolean river = forcedCol < 0 && cfg.hasRiver() && rng.nextDouble() < .55;
        int[] t;
        if (forcedCol >= 0) {
            int dropRow = TileMap.ROAD_TOP + rng.nextInt(2);
            t = anyItemAt(forcedCol, dropRow) ? null : new int[]{forcedCol, dropRow};
        }
        else t = freeTile(river);
        if (t == null) return;
        WasteItem w = new WasteItem(WASTE[rng.nextInt(WASTE.length)], t[0], t[1]);
        w.river = river;
        items.add(w);
    }

    private int[] freeTile(boolean water) {
        for (int tries = 0; tries < 24; tries++) {
            int c = water ? map.laneMin() + rng.nextInt(map.laneMax() - map.laneMin() + 1) : rng.nextInt(TileMap.COLS);
            int r = water ? TileMap.RIVER_TOP + rng.nextInt(2) : TileMap.ROAD_TOP + rng.nextInt(2);
            if (water && map.at(c, r) != Tile.WATER) continue;
            if (!anyItemAt(c, r)) return new int[]{c, r};
        }
        return null;
    }

    // ------------------------------------------------------------------ mission and end of run

    private void checkMission() {
        if (cfg.isCampaign() && !missionDone && sorted >= cfg.missionSort && planted >= cfg.missionPlant && cleaned >= cfg.missionClean) {
            missionDone = true;
            score += 100;
            money += 50;
            toast("Mission complete! +100 Score, PHP 50", score >= cfg.target ? "Target reached!" : "Now reach the target score of " + cfg.target + ".", true, 4);
            listener.onSound(Sfx.MISSION);
        }
        if (missionDone && score >= cfg.target) endRun(true);
    }

    private void endRun(boolean win) {
        if (ended) return;
        ended = true;
        save.wallet += money;
        if (!cfg.isCampaign()) {
            int v = cfg.mode == Mode.TIME || cfg.mode == Mode.ZEN ? score : (int) time;
            boolean sprint = cfg.mode == Mode.SPRINT;
            won = !(sprint && sorted < 25);
            if (won) {
                int k = cfg.highSlot;
                if (sprint ? (save.high[k] == 0 || v < save.high[k]) : v > save.high[k]) save.high[k] = v;
                stars = sprint ? (v <= 40 ? 3 : v <= 60 ? 2 : 1)
                        : cfg.mode == Mode.ENDLESS ? (v >= 240 ? 3 : v >= 120 ? 2 : 1)
                        : (v >= 1000 ? 3 : v >= 500 ? 2 : 1);
                listener.onSound(Sfx.WIN);
            }
            save.store();
            return;
        }
        won = win;
        if (win) {
            stars = pollution <= 25 && mistakes.size() <= 2 ? 3 : pollution <= 50 ? 2 : 1;
            save.stars[levelIndex] = Math.max(save.stars[levelIndex], stars);
            if (levelIndex < Levels.CAMPAIGN.size() - 1 && levelIndex + 1 > save.unlocked) save.unlocked = levelIndex + 1;
            save.completed = Math.max(save.completed, levelIndex);
            save.started = true;
            listener.onSound(Sfx.WIN);
        }
        save.store();
    }

    /** Unique mistakes, newest last (for the result screen). */
    public List<String> uniqueMistakes() {
        List<String> out = new ArrayList<>(new java.util.LinkedHashSet<>(mistakes));
        return Collections.unmodifiableList(out);
    }
}
