package com.dragonminez.mod.common.player.cap.progression;

import com.dragonminez.core.common.player.capability.CapData;
import com.dragonminez.core.common.player.capability.CapDataHolder;
import com.dragonminez.core.common.player.capability.ICap;
import com.dragonminez.core.common.util.JavaUtil.DataType;
import com.dragonminez.core.common.util.MathUtil;
import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.common.config.GeneralConfig;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public class ProgressData implements ICap {

  private int zPoints = 0;

  public ProgressData() {
    super();
  }

  @Override
  public void randomize() {
    final int max = GeneralConfig.attributes().maxAttributes;
    this.setZPoints(MathUtil.randomInt(0, max));
  }

  @Override
  public CapDataHolder holder() {
    return ProgressDataHolder.INSTANCE;
  }

  public void setZPoints(int points) {
    this.zPoints = points;
  }

  public int getZPoints() {
    return this.zPoints;
  }

  public static class ProgressDataHolder extends CapDataHolder {

    public static final ResourceLocation ID = new ResourceLocation(Reference.MOD_ID, "progress");
    public static final ProgressDataHolder INSTANCE = new ProgressDataHolder();

    public static final CapData<ProgressData, Integer> ZPOINTS = CapData.of("zpoints",
        DataType.INTEGER, ProgressData::setZPoints,
        ProgressData::getZPoints, false, false
    );

    public ProgressDataHolder() {
      super(ID);
    }

    @Override
    public List<CapData<?, ?>> acceptedData() {
      return List.of(ProgressDataHolder.ZPOINTS);
    }
  }
}
