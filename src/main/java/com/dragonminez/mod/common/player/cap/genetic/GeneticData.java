package com.dragonminez.mod.common.player.cap.genetic;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.core.common.player.capability.CapDataHolder;
import com.dragonminez.mod.core.common.player.capability.CapDataType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public class GeneticData extends CapDataHolder {

  private String race = Reference.EMPTY;
  private String form = Reference.EMPTY;

  public GeneticData() {
    super(GeneticDataType.ID);
  }

  public GeneticData(String race, String form) {
    super(GeneticDataType.ID);
    this.race = race;
    this.form = form;
  }

  @Override
  public CompoundTag serializeNBT() {
    final CompoundTag nbt = new CompoundTag();
    nbt.putString(GeneticDataType.RACE.id(), this.race);
    nbt.putString(GeneticDataType.FORM.id(), this.form);
    return nbt;
  }

  @Override
  public void deserializeNBT(CompoundTag nbt) {
    this.race = nbt.getString(GeneticDataType.RACE.id());
    this.form = nbt.getString(GeneticDataType.FORM.id());
  }

  public String getRace() {
    return race;
  }

  public void setRace(String race) {
    this.race = race;
  }

  public String getForm() {
    return form;
  }

  public void setForm(String form) {
    this.form = form;
  }

  public static class GeneticDataType {

    public final static ResourceLocation ID = new ResourceLocation(Reference.MOD_ID, "genetic");
    public static final CapDataType RACE = CapDataType.of("race", true);
    public static final CapDataType FORM = CapDataType.of("form", true);

  }
}
