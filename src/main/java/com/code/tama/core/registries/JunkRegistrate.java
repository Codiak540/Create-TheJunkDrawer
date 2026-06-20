package com.code.tama.core.registries;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.VirtualFluidBuilder;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateProvider;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.CreateClient;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.api.registry.registrate.SimpleBuilder;
import com.simibubi.create.content.decoration.encasing.CasingConnectivity;
import com.simibubi.create.content.fluids.VirtualFluid;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder.BlockEntityFactory;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class JunkRegistrate extends AbstractRegistrate<JunkRegistrate> {
	private static final Map<RegistryEntry<?, ?>, DeferredHolder<CreativeModeTab, CreativeModeTab>> TAB_LOOKUP = Collections.synchronizedMap(new IdentityHashMap<>());

	@Nullable
	protected Function<Item, TooltipModifier> currentTooltipModifierFactory;
	protected DeferredHolder<CreativeModeTab, CreativeModeTab> currentTab;

	protected JunkRegistrate(String modid) {
		super(modid);
	}

	public static JunkRegistrate create(String modid) {
		JunkRegistrate registrate = new JunkRegistrate(modid);
		JunkRegistrateRegistrationCallback.provideRegistrate(registrate);
		return registrate;
	}

	public static boolean isInCreativeTab(RegistryEntry<?, ?> entry, DeferredHolder<CreativeModeTab, CreativeModeTab> tab) {
		return TAB_LOOKUP.get(entry) == tab;
	}

	public JunkRegistrate setTooltipModifierFactory(@Nullable Function<Item, TooltipModifier> factory) {
		currentTooltipModifierFactory = factory;
		return self();
	}

	@Nullable
	public Function<Item, TooltipModifier> getTooltipModifierFactory() {
		return currentTooltipModifierFactory;
	}

	@Nullable
	public JunkRegistrate setCreativeTab(DeferredHolder<CreativeModeTab, CreativeModeTab> tab) {
		currentTab = tab;
		return self();
	}

	public DeferredHolder<CreativeModeTab, CreativeModeTab> getCreativeTab() {
		return currentTab;
	}

	@Override
	public JunkRegistrate registerEventListeners(IEventBus bus) {
		return super.registerEventListeners(bus);
	}

	@Override
	protected <R, T extends R> RegistryEntry<R, T> accept(String name, ResourceKey<? extends Registry<R>> type, Builder<R, T, ?, ?> builder, NonNullSupplier<? extends T> creator, NonNullFunction<DeferredHolder<R, T>, ? extends RegistryEntry<R, T>> entryFactory) {
		RegistryEntry<R, T> entry = super.accept(name, type, builder, creator, entryFactory);
		if (type.equals(Registries.ITEM) && currentTooltipModifierFactory != null) {
			// grab the factory here for the lambda, it can change between now and registration
			Function<Item, TooltipModifier> factory = currentTooltipModifierFactory;
			this.addRegisterCallback(name, Registries.ITEM, item -> {
				TooltipModifier modifier = factory.apply(item);
				TooltipModifier.REGISTRY.register(item, modifier);
			});
		}
		if (currentTab != null)
			TAB_LOOKUP.put(entry, currentTab);

		return entry;
	}

	@Override
	public <T extends BlockEntity> JunkBlockEntityBuilder<T, JunkRegistrate> blockEntity(String name,
	                                                                                       BlockEntityFactory<T> factory) {
		return blockEntity(self(), name, factory);
	}

	@Override
	public <T extends BlockEntity, P> JunkBlockEntityBuilder<T, P> blockEntity(P parent, String name,
																				 BlockEntityFactory<T> factory) {
		return (JunkBlockEntityBuilder<T, P>) entry(name,
			(callback) -> JunkBlockEntityBuilder.create(this, parent, name, callback, factory));
	}

	@Override
	public <T extends Entity> JunkEntityBuilder<T, JunkRegistrate> entity(String name,
	                                                                      EntityType.EntityFactory<T> factory, MobCategory classification) {
		return this.entity(self(), name, factory, classification);
	}

	@Override
	public <T extends Entity, P> JunkEntityBuilder<T, P> entity(P parent, String name,
	                                                            EntityType.EntityFactory<T> factory, MobCategory classification) {
		return (JunkEntityBuilder<T, P>) this.entry(name, (callback) -> {
			return JunkEntityBuilder.create(this, parent, name, callback, factory, classification);
		});
	}

	@Override
	public <T extends Block> JunkBlockBuilder<T, JunkRegistrate> block(NonNullFunction<BlockBehaviour.Properties, T> factory) {
		return block(self(), factory);
	}

	@Override
	public <T extends Block> JunkBlockBuilder<T, JunkRegistrate> block(String name, NonNullFunction<BlockBehaviour.Properties, T> factory) {
		return block(self(), name, factory);
	}

	@Override
	public <T extends Block, P> JunkBlockBuilder<T, P> block(P parent, NonNullFunction<BlockBehaviour.Properties, T> factory) {
		return block(parent, currentName(), factory);
	}

	@Override
	public <T extends RegistrateProvider> void genData(ProviderType<? extends T> type, T gen) {
		try {
			super.genData(type, gen);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	@Override
	public <T extends Block, P> JunkBlockBuilder<T, P> block(P parent, String name, NonNullFunction<BlockBehaviour.Properties, T> factory) {
		return (JunkBlockBuilder<T, P>) entry(name, callback -> JunkBlockBuilder.create(this, parent, name, callback, factory));
	}

	/* Palettes */

	public <T extends Block> JunkBlockBuilder<T, JunkRegistrate> paletteStoneBlock(String name,
	                                                                               NonNullFunction<Properties, T> factory, NonNullSupplier<Block> propertiesFrom, boolean worldGenStone,
	                                                                               boolean hasNaturalVariants) {
		com.tterrag.registrate.builders.BlockBuilder<T, JunkRegistrate> builder = super.block(name, factory).initialProperties(propertiesFrom)
			.transform(pickaxeOnly())
			.blockstate(hasNaturalVariants ? BlockStateGen.naturalStoneTypeBlock(name) : (c, p) -> {
				final String location = "block/palettes/stone_types/" + c.getName();
				p.simpleBlock(c.get(), p.models()
					.cubeAll(c.getName(), p.modLoc(location)));
			})
			.tag(BlockTags.DRIPSTONE_REPLACEABLE)
			.tag(BlockTags.AZALEA_ROOT_REPLACEABLE)
			.tag(BlockTags.MOSS_REPLACEABLE)
			.tag(BlockTags.LUSH_GROUND_REPLACEABLE)
			.item()
			.model((c, p) -> p.cubeAll(c.getName(),
				p.modLoc(hasNaturalVariants ? "block/palettes/stone_types/natural/" + name + "_1"
					: "block/palettes/stone_types/" + c.getName())))
			.build();
		return (JunkBlockBuilder<T, JunkRegistrate>) (Object) builder;
	}

	public JunkBlockBuilder<Block, JunkRegistrate> paletteStoneBlock(String name, NonNullSupplier<Block> propertiesFrom,
	                                                                 boolean worldGenStone, boolean hasNaturalVariants) {
		return paletteStoneBlock(name, Block::new, propertiesFrom, worldGenStone, hasNaturalVariants);
	}

	/* Fluids */

	public <T extends BaseFlowingFluid> FluidBuilder<T, JunkRegistrate> virtualFluid(String name,
	                                                                                 FluidBuilder.FluidTypeFactory typeFactory, NonNullFunction<BaseFlowingFluid.Properties, T> sourceFactory,
	                                                                                 NonNullFunction<BaseFlowingFluid.Properties, T> flowingFactory) {
		return entry(name,
			c -> new VirtualFluidBuilder<>(self(), self(), name, c, ResourceLocation.fromNamespaceAndPath(getModid(), "fluid/" + name + "_still"),
				ResourceLocation.fromNamespaceAndPath(getModid(), "fluid/" + name + "_flow"), typeFactory, sourceFactory, flowingFactory));
	}

	public <T extends BaseFlowingFluid> FluidBuilder<T, JunkRegistrate> virtualFluid(String name,
	                                                                                 ResourceLocation still, ResourceLocation flow, FluidBuilder.FluidTypeFactory typeFactory,
	                                                                                 NonNullFunction<BaseFlowingFluid.Properties, T> sourceFactory, NonNullFunction<BaseFlowingFluid.Properties, T> flowingFactory) {
		return entry(name, c -> new VirtualFluidBuilder<>(self(), self(), name, c, still, flow, typeFactory, sourceFactory, flowingFactory));
	}

	public FluidBuilder<VirtualFluid, JunkRegistrate> virtualFluid(String name) {
		return entry(name,
			c -> new VirtualFluidBuilder<>(self(), self(), name, c,
				ResourceLocation.fromNamespaceAndPath(getModid(), "fluid/" + name + "_still"), ResourceLocation.fromNamespaceAndPath(getModid(), "fluid/" + name + "_flow"),
				JunkRegistrate::defaultFluidType, VirtualFluid::createSource, VirtualFluid::createFlowing));
	}

	public FluidBuilder<VirtualFluid, JunkRegistrate> virtualFluid(String name, ResourceLocation still,
	                                                               ResourceLocation flow) {
		return entry(name, c -> new VirtualFluidBuilder<>(self(), self(), name, c, still, flow,
			JunkRegistrate::defaultFluidType, VirtualFluid::createSource, VirtualFluid::createFlowing));
	}

	public FluidBuilder<BaseFlowingFluid.Flowing, JunkRegistrate> standardFluid(String name) {
		return fluid(name, ResourceLocation.fromNamespaceAndPath(getModid(), "fluid/" + name + "_still"), ResourceLocation.fromNamespaceAndPath(getModid(), "fluid/" + name + "_flow"));
	}

	public FluidBuilder<BaseFlowingFluid.Flowing, JunkRegistrate> standardFluid(String name,
	                                                                            FluidBuilder.FluidTypeFactory typeFactory) {
		return fluid(name, ResourceLocation.fromNamespaceAndPath(getModid(), "fluid/" + name + "_still"), ResourceLocation.fromNamespaceAndPath(getModid(), "fluid/" + name + "_flow"),
			typeFactory);
	}

	@SuppressWarnings("removal")
	public static FluidType defaultFluidType(FluidType.Properties properties, ResourceLocation stillTexture,
											 ResourceLocation flowingTexture) {
		return new FluidType(properties) {
			@Override
			public void initializeClient(@NotNull Consumer<IClientFluidTypeExtensions> consumer) {
				consumer.accept(new IClientFluidTypeExtensions() {
					@Override
					public ResourceLocation getStillTexture() {
						return stillTexture;
					}

					@Override
					public ResourceLocation getFlowingTexture() {
						return flowingTexture;
					}
				});
			}
		};
	}

	/* Util */

	public static <T extends Block> NonNullConsumer<? super T> casingConnectivity(
		BiConsumer<T, CasingConnectivity> consumer) {
		return entry -> CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> registerCasingConnectivity(entry, consumer));
	}

	public static <T extends Block> NonNullConsumer<? super T> blockModel(
		Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
		return entry -> CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> registerBlockModel(entry, func));
	}

	public static <T extends Item> NonNullConsumer<? super T> itemModel(
		Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
		return entry -> CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> registerItemModel(entry, func));
	}

	public static NonNullConsumer<? super Block> connectedTextures(
		Supplier<ConnectedTextureBehaviour> behavior) {
		return entry -> CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> registerCTBehviour(entry, behavior));
	}

	@OnlyIn(Dist.CLIENT)
	private static <T extends Block> void registerCasingConnectivity(T entry,
																	 BiConsumer<T, CasingConnectivity> consumer) {
		consumer.accept(entry, CreateClient.CASING_CONNECTIVITY);
	}

	@OnlyIn(Dist.CLIENT)
	private static void registerBlockModel(Block entry,
										   Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
		CreateClient.MODEL_SWAPPER.getCustomBlockModels()
			.register(RegisteredObjectsHelper.getKeyOrThrow(entry), func.get());
	}

	@OnlyIn(Dist.CLIENT)
	private static void registerItemModel(Item entry,
										  Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
		CreateClient.MODEL_SWAPPER.getCustomItemModels()
			.register(RegisteredObjectsHelper.getKeyOrThrow(entry), func.get());
	}

	@OnlyIn(Dist.CLIENT)
	private static void registerCTBehviour(Block entry, Supplier<ConnectedTextureBehaviour> behaviorSupplier) {
		ConnectedTextureBehaviour behavior = behaviorSupplier.get();
		CreateClient.MODEL_SWAPPER.getCustomBlockModels()
			.register(RegisteredObjectsHelper.getKeyOrThrow(entry), model -> new CTModel(model, behavior));
	}
}
