// SPDX-License-Identifier: GPL-3.0-only
// Clean-room adapter: preserves the Forge 1.20.1 PacketDistributor call shape using NeoForge 21.1.248 APIs.
package com.dragonminez.compat.network;

import java.util.Objects;
import java.util.function.Supplier;

/** Compatibility targets for the former Forge PacketDistributor API. */
public final class PacketDistributor {
    public static final Target PLAYER = new Target(Kind.PLAYER);
    public static final Target ALL = new Target(Kind.ALL);
    public static final Target TRACKING_ENTITY = new Target(Kind.TRACKING_ENTITY);
    public static final Target TRACKING_ENTITY_AND_SELF = new Target(Kind.TRACKING_ENTITY_AND_SELF);

    private PacketDistributor() {}

    public enum Kind {
        PLAYER,
        ALL,
        TRACKING_ENTITY,
        TRACKING_ENTITY_AND_SELF
    }

    public record PacketTarget(Kind kind, Supplier<?> target) {
        public PacketTarget {
            Objects.requireNonNull(kind, "kind");
        }
    }

    public static final class Target {
        private final Kind kind;

        private Target(Kind kind) {
            this.kind = kind;
        }

        public PacketTarget with(Supplier<?> target) {
            return new PacketTarget(kind, Objects.requireNonNull(target, "target"));
        }

        public PacketTarget noArg() {
            return new PacketTarget(kind, null);
        }
    }
}
