package com.dragonminez.mod.common.player.cap.genetic;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.core.common.player.capability.CapDataHolder;
import com.dragonminez.core.common.player.capability.CapData;
import com.dragonminez.core.common.player.capability.ICap;
import com.dragonminez.core.common.util.JavaUtil.DataType;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public class GeneticData implements ICap {

  private String race = Reference.EMPTY;
  private String form = Reference.EMPTY;

  public GeneticData() {
    super();
  }

  @Override
  public CapDataHolder holder() {
    return GeneticDataHolder.INSTANCE;
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

  public static class GeneticDataHolder extends CapDataHolder {

    public static final ResourceLocation ID = new ResourceLocation(Reference.MOD_ID, "genetic");
    public static final GeneticDataHolder INSTANCE = new GeneticDataHolder();

    public static final CapData<GeneticData, String> RACE = CapData.of("race", DataType.STRING,
        GeneticData::setRace, GeneticData::getRace, true, true);

    public static final CapData<GeneticData, String> FORM = CapData.of("form", DataType.STRING,
        GeneticData::setForm, GeneticData::getForm, true, true);

    public GeneticDataHolder() {
      super(ID);
    }

    @Override
    public List<CapData<?, ?>> acceptedData() {
      return List.of(RACE, FORM);
    }
  }
}
