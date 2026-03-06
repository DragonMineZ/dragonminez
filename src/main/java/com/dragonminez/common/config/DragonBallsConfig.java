package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class DragonBallsConfig {
    public static final int CURRENT_VERSION = 1;
    private int configVersion;

    private List<DragonBallData> dragonBalls = new ArrayList<>();

    @Setter
    @Getter
    @NoArgsConstructor
    public static class DragonBallData {
        private DragonData dragon;
        private BallData balls;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class DragonData {
        private String name;
        private Float width;
        private Float height;
        private Integer wishAmount;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class BallData {
        private String name;
        private Integer amount;
        private Integer range;
        private String dimensionKey;
        private Double[] baseShape;
    }
}
