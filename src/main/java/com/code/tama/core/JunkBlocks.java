package com.code.tama.core;

import static com.code.tama.JunkDrawer.RL;
import static com.simibubi.create.api.behaviour.display.DisplaySource.displaySource;
import static com.simibubi.create.api.behaviour.movement.MovementBehaviour.movementBehaviour;
import static com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType.mountedFluidStorage;
import static com.simibubi.create.api.contraption.storage.item.MountedItemStorageType.mountedItemStorage;
import static com.simibubi.create.foundation.data.BlockStateGen.axisBlock;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;
import static com.simibubi.create.foundation.data.TagGen.tagBlockAndItem;

import com.code.tama.JunkDrawer;
import com.code.tama.core.blocks.TertiaryLeverBlock;
import com.code.tama.core.registries.JunkRegistrate;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.clipboard.ClipboardBlock;
import com.simibubi.create.content.equipment.clipboard.ClipboardBlockItem;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankGenerator;
import com.simibubi.create.content.fluids.tank.FluidTankItem;
import com.simibubi.create.content.fluids.tank.FluidTankModel;
import com.simibubi.create.content.fluids.tank.FluidTankMovementBehavior;
import com.simibubi.create.content.logistics.funnel.BrassFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelGenerator;
import com.simibubi.create.content.logistics.funnel.FunnelItem;
import com.simibubi.create.content.logistics.funnel.FunnelMovementBehaviour;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlock;
import com.simibubi.create.foundation.block.DyedBlockList;
import com.simibubi.create.foundation.block.render.ReducedDestroyEffects;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.UncontainableBlockItem;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTable.Builder;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

@SuppressWarnings("removal")
public class JunkBlocks {
	private static final JunkRegistrate REGISTRATE = JunkDrawer.registrate();

	static {
		REGISTRATE.setCreativeTab(JunkDrawer.MAIN_TAB);
	}

	public static final BlockEntry<TertiaryLeverBlock> TERTIARY_LEVER = REGISTRATE.block("tertiary_lever", TertiaryLeverBlock::new)
			.defaultLoot()
			.initialProperties(() -> Blocks.LEVER)
			.defaultBlockstate()
			.item().recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
					.define('R', Items.REDSTONE)
					.define('S', Items.STICK)
					.define('C', Items.COBBLESTONE)
					.pattern(" S ")
					.pattern("RCR")
					.unlockedBy("has_redstone", RegistrateRecipeProvider.has(Items.REDSTONE))
					.save(p, RL(c.getName())))
			.build()
			.register();



	// Leaving these here for examples
//	public static final BlockEntry<SchematicannonBlock> SCHEMATICANNON =
//		REGISTRATE.block("schematicannon", SchematicannonBlock::new)
//			.initialProperties(() -> Blocks.DISPENSER)
//			.properties(p -> p.mapColor(MapColor.COLOR_GRAY))
//			.transform(pickaxeOnly())
//			.blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), AssetLookup.partialBaseModel(ctx, prov)))
//			.loot((lt, block) -> {
//				Builder builder = LootTable.lootTable();
//				LootItemCondition.Builder survivesExplosion = ExplosionCondition.survivesExplosion();
//				lt.add(block, builder.withPool(LootPool.lootPool()
//					.when(survivesExplosion)
//					.setRolls(ConstantValue.exactly(1))
//					.add(LootItem.lootTableItem(JunkBlocks.SCHEMATICANNON.get()
//							.asItem())
//						.apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
//							.include(AllDataComponents.SCHEMATICANNON_OPTIONS)))));
//			})
//			.item()
//			.transform(customItemModel())
//			.register();
//
//
//	public static final BlockEntry<FluidTankBlock> FLUID_TANK = REGISTRATE.block("fluid_tank", FluidTankBlock::regular)
//		.initialProperties(SharedProperties::copperMetal)
//		.properties(p -> p.noOcclusion()
//			.isRedstoneConductor((p1, p2, p3) -> true))
//		.transform(pickaxeOnly())
//		.blockstate(new FluidTankGenerator()::generate)
//		.onRegister(CreateRegistrate.blockModel(() -> FluidTankModel::standard))
//		.transform(displaySource(AllDisplaySources.BOILER))
//		.transform(mountedFluidStorage(AllMountedStorageTypes.FLUID_TANK))
//		.onRegister(movementBehaviour(new FluidTankMovementBehavior()))
//		.addLayer(() -> RenderType::cutoutMipped)
//		.item(FluidTankItem::new)
//		.model(AssetLookup.customBlockItemModel("_", "block_single_window"))
//		.build()
//		.register();
//
//	public static final BlockEntry<BrassFunnelBlock> BRASS_FUNNEL =
//		REGISTRATE.block("brass_funnel", BrassFunnelBlock::new)
//			.addLayer(() -> RenderType::cutoutMipped)
//			.initialProperties(SharedProperties::softMetal)
//			.properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW))
//			.transform(pickaxeOnly())
//			.tag(AllBlockTags.SAFE_NBT.tag)
//			.clientExtension(() -> () -> new ReducedDestroyEffects())
//			.onRegister(movementBehaviour(FunnelMovementBehaviour.brass()))
//			.blockstate(new FunnelGenerator("brass", true)::generate)
//			.item(FunnelItem::new)
//			.tag(AllItemTags.CONTRAPTION_CONTROLLED.tag)
//			.model(FunnelGenerator.itemModel("brass"))
//			.build()
//			.register();
//
//	public static final DyedBlockList<ToolboxBlock> TOOLBOXES = new DyedBlockList<>(colour -> {
//		String colourName = colour.getSerializedName();
//		return REGISTRATE.block(colourName + "_toolbox", p -> new ToolboxBlock(p, colour))
//			.initialProperties(SharedProperties::wooden)
//			.properties(p -> p.sound(SoundType.WOOD)
//				.mapColor(colour)
//				.forceSolidOn())
//			.addLayer(() -> RenderType::cutoutMipped)
//			.loot((lt, block) -> {
//				lt.add(block, LootTable.lootTable().withPool(LootPool.lootPool()
//						.when(ExplosionCondition.survivesExplosion())
//						.setRolls(ConstantValue.exactly(1))
//						.add(LootItem.lootTableItem(block)
//								.apply(CopyNameFunction.copyName(CopyNameFunction.NameSource.BLOCK_ENTITY))
//								.apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
//										.include(AllDataComponents.TOOLBOX_UUID)
//										.include(AllDataComponents.TOOLBOX_INVENTORY)
//								)
//						)
//				));
//			})
//			.blockstate((c, p) -> {
//				p.horizontalBlock(c.get(), p.models()
//					.withExistingParent(colourName + "_toolbox", p.modLoc("block/toolbox/block"))
//					.texture("0", p.modLoc("block/toolbox/" + colourName)));
//			})
//			.onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block.create.toolbox"))
//			.transform(mountedItemStorage(AllMountedStorageTypes.TOOLBOX))
//			.tag(AllBlockTags.TOOLBOXES.tag)
//			.item(UncontainableBlockItem::new)
//			.model((c, p) -> p.withExistingParent(colourName + "_toolbox", p.modLoc("block/toolbox/item"))
//				.texture("0", p.modLoc("block/toolbox/" + colourName)))
//			.tag(AllItemTags.TOOLBOXES.tag)
//			.build()
//			.register();
//	});
//
//	public static final BlockEntry<ClipboardBlock> CLIPBOARD = REGISTRATE.block("clipboard", ClipboardBlock::new)
//		.initialProperties(SharedProperties::wooden)
//		.properties(p -> p.forceSolidOn())
//		.transform(axeOrPickaxe())
//		.tag(AllBlockTags.SAFE_NBT.tag)
//		.blockstate((c, p) -> p.horizontalFaceBlock(c.get(),
//			s -> AssetLookup.partialBaseModel(c, p, s.getValue(ClipboardBlock.WRITTEN) ? "written" : "empty")))
//		.loot((lt, b) -> lt.add(b, BlockLootSubProvider.noDrop()))
//		.item(ClipboardBlockItem::new)
//		.onRegister(ClipboardBlockItem::registerModelOverrides)
//		.model((c, p) -> ClipboardOverrides.addOverrideModels(c, p))
//		.build()
//		.register();


	public static void register() {
	}

}
