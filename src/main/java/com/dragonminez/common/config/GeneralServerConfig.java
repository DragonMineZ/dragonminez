package com.dragonminez.common.config;

public class GeneralServerConfig {

    private WorldGenConfig worldGen = new WorldGenConfig();
    private GameplayConfig gameplay = new GameplayConfig();

    public WorldGenConfig getWorldGen() {
        return worldGen;
    }

    public void setWorldGen(WorldGenConfig worldGen) {
        this.worldGen = worldGen;
    }

    public GameplayConfig getGameplay() {
        return gameplay;
    }

    public void setGameplay(GameplayConfig gameplay) {
        this.gameplay = gameplay;
    }

    public static class WorldGenConfig {
        private boolean generateCustomStructures = true;
        private boolean generateDragonBalls = true;

        public boolean isGenerateCustomStructures() { return generateCustomStructures; }
        public void setGenerateCustomStructures(boolean generate) { this.generateCustomStructures = generate; }
        public boolean isGenerateDragonBalls() { return generateDragonBalls; }
        public void setGenerateDragonBalls(boolean generate) { this.generateDragonBalls = generate; }

    }

    public static class GameplayConfig {
        private double tpsMultiplier = 1.0;
        private boolean respectAttackCooldown = true;
        private int maxStatValue = 10000;

        public double getTpsMultiplier() { return tpsMultiplier; }
        public void setTpsMultiplier(double multiplier) { this.tpsMultiplier = multiplier; }

        public boolean isRespectAttackCooldown() { return respectAttackCooldown; }
        public void setRespectAttackCooldown(boolean respectAttackCooldown) { this.respectAttackCooldown = respectAttackCooldown; }

        public int getMaxStatValue() { return maxStatValue; }
        public void setMaxStatValue(int maxStatValue) { this.maxStatValue = maxStatValue; }
    }
}

