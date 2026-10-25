package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import java.util.function.Function;

public class VehicleModel<T extends VehicleEntity> extends GeoModel<T> {

	private final ResourceLocation model;
	private final Function<T, ResourceLocation> texture;

	public VehicleModel(String path) {
		this(path, texture(path));
	}

	public VehicleModel(String path, ResourceLocation texture) {
		this(path, entity -> texture);
	}

	public VehicleModel(String path, Function<T, ResourceLocation> texture) {
		this.model = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/" + path + ".geo.json");
		this.texture = texture;
	}

	public static ResourceLocation texture(String path) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/" + path + ".png");
	}

	@Override
	public ResourceLocation getModelResource(T animatable) {
		return model;
	}

	@Override
	public ResourceLocation getTextureResource(T animatable) {
		return texture.apply(animatable);
	}

	@Override
	public ResourceLocation getAnimationResource(T animatable) {
		return null;
	}
}
