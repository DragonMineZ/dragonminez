package com.dragonminez.common.combat.weapon;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class AttributesContainer {

    @Nullable
    private final String parent;
    @Nullable
    private final WeaponAttributes attributes;

    public AttributesContainer(@Nullable String parent, @Nullable WeaponAttributes attributes) {
        this.parent = parent;
        this.attributes = attributes;
    }

    public String parent() {
        return parent;
    }

    public WeaponAttributes attributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AttributesContainer) obj;
        return Objects.equals(this.parent, that.parent) && Objects.equals(this.attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, attributes);
    }

    @Override
    public String toString() {
        return "AttributesContainer[" + "parent=" + parent + ", " + "attributes=" + attributes + ']';
    }
}
