package com.code.tama.core.registries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.*;
import com.tterrag.registrate.builders.BlockEntityBuilder.BlockEntityFactory;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.providers.loot.RegistrateLootTableProvider.LootType;
import com.tterrag.registrate.util.OneTimeEventReceiver;
import com.tterrag.registrate.util.RegistrateDistExecutor;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A builder for blocks, allows for customization of the {@link Block.Properties}, creation of block items, and configuration of data associated with blocks (loot tables, recipes, etc.).
 * 
 * @param <T>
 *            The type of block being built
 * @param <P>
 *            Parent object type
 */
public class JunkBlockBuilder<T extends Block, P> extends BlockBuilder<T, P> {

    /**
     * Create a new {@link JunkBlockBuilder} and configure data. Used in lieu of adding side-effects to constructor, so that alternate initialization strategies can be done in subclasses.
     * <p>
     * The block will be assigned the following data:
     * <ul>
     * <li>A default blockstate file mapping all states to one model (via {@link #defaultBlockstate()})</li>
     * <li>A simple cube_all model (used in the blockstate) with one texture (via {@link #defaultBlockstate()})</li>
     * <li>A self-dropping loot table (via {@link #defaultLoot()})</li>
     * <li>The default translation (via {@link #defaultLang()})</li>
     * </ul>
     * 
     * @param <T>
     *            The type of the builder
     * @param <P>
     *            Parent object type
     * @param owner
     *            The owning {@link AbstractRegistrate} object
     * @param parent
     *            The parent object
     * @param name
     *            Name of the entry being built
     * @param callback
     *            A callback used to actually register the built entry
     * @param factory
     *            Factory to create the block
     * @return A new {@link JunkBlockBuilder} with reasonable default data generators.
     */
    public static <T extends Block, P> JunkBlockBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<BlockBehaviour.Properties, T> factory) {
        return new JunkBlockBuilder<>(owner, parent, name, callback, factory, BlockBehaviour.Properties::of)
                .defaultBlockstate().defaultLoot().defaultLang();
    }

    protected JunkBlockBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<BlockBehaviour.Properties, T> factory, NonNullSupplier<BlockBehaviour.Properties> initialProperties) {
        super(owner, parent, name, callback, factory, initialProperties);
    }


    /**
     * Create a standard {@link BlockItem} for this block, building it immediately, and not allowing for further configuration.
     * <p>
     * The item will have no lang entry (since it would duplicate the block's) and a simple block item model (via {@link RegistrateItemModelProvider#blockItem(NonNullSupplier)}).
     *
     * @return this {@link JunkBlockBuilder}
     * @see #item()
     */
    public JunkBlockBuilder<T, P> simpleItem() {
        return (JunkBlockBuilder<T, P>) item().build();
    }

    /**
     * Create a standard {@link BlockItem} for this block, and return the builder for it so that further customization can be done.
     * <p>
     * The item will have no lang entry (since it would duplicate the block's) and a simple block item model (via {@link RegistrateItemModelProvider#blockItem(NonNullSupplier)}).
     * 
     * @return the {@link ItemBuilder} for the {@link BlockItem}
     */
    public ItemBuilder<BlockItem, BlockBuilder<T, P>> item() {
        return item(BlockItem::new);
    }

    /**
     * Create a {@link BlockItem} for this block, which is created by the given factory, and return the builder for it so that further customization can be done.
     * <p>
     * By default, the item will have no lang entry (since it would duplicate the block's) and a simple block item model (via {@link RegistrateItemModelProvider#blockItem(NonNullSupplier)}).
     * 
     * @param <I>
     *            The type of the item
     * @param factory
     *            A factory for the item, which accepts the block object and properties and returns a new item
     * @return the {@link ItemBuilder} for the {@link BlockItem}
     */
    public <I extends Item> ItemBuilder<I, BlockBuilder<T, P>> item(NonNullBiFunction<? super T, Item.Properties, ? extends I> factory) {
        final var sup = asSupplier();
        return ((ItemBuilder<I, BlockBuilder<T,P>>) (Object) getOwner().<I, JunkBlockBuilder<T, P>> item(this, getName(), p -> factory.apply(getEntry(), p))
                .setData(ProviderType.LANG, NonNullBiConsumer.noop())
                .model((ctx, prov) -> {
                    try {
                        Optional<String> model = getOwner().getDataProvider(ProviderType.BLOCKSTATE)
                                .flatMap(p -> p.getExistingVariantBuilder(getEntry()))
                                .map(b -> b.getModels().get(b.partialState()))
                                .map(BlockStateProvider.ConfiguredModelList::toJSON)
                                .filter(JsonElement::isJsonObject)
                                .map(j -> j.getAsJsonObject().get("model"))
                                .map(JsonElement::getAsString);
                        if (model.isPresent()) {
                            prov.withExistingParent(ctx.getName(), model.get());
                        } else {
                            prov.blockItem(sup);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }));
    }

    /**
     * Assign the default blockstate, which maps all states to a single model file (via {@link RegistrateBlockstateProvider#simpleBlock(Block)}). This is the default, so it is generally not necessary
     * to call, unless for undoing previous changes.
     * 
     * @return this {@link JunkBlockBuilder}
     */
    public JunkBlockBuilder<T, P> defaultBlockstate() {
        return blockstate((ctx, prov) -> {
            try {
                prov.simpleBlock(ctx.getEntry());
            }
            catch (Exception e) { e.printStackTrace(); }
        });
    }

    /**
     * Configure the blockstate/models for this block.
     * 
     * @param cons
     *            The callback which will be invoked during data generation.
     * @return this {@link JunkBlockBuilder}
     * @see #setData(ProviderType, NonNullBiConsumer)
     */
    public JunkBlockBuilder<T, P> blockstate(NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> cons) {
        return (JunkBlockBuilder<T, P>) setData(ProviderType.BLOCKSTATE, cons);
    }

    /**
     * Assign the default translation, as specified by {@link RegistrateLangProvider#getAutomaticName(NonNullSupplier, net.minecraft.resources.ResourceKey)}. This is the default, so it is generally
     * not necessary to call, unless for undoing previous changes.
     * 
     * @return this {@link JunkBlockBuilder}
     */
    public JunkBlockBuilder<T, P> defaultLang() {
        return (JunkBlockBuilder<T, P>) lang(Block::getDescriptionId);
    }

    /**
     * Set the translation for this block.
     * 
     * @param name
     *            A localized English name
     * @return this {@link JunkBlockBuilder}
     */
    public JunkBlockBuilder<T, P> lang(String name) {
        return (JunkBlockBuilder<T, P>) lang(Block::getDescriptionId, name);
    }

    /**
     * Assign the default loot table, as specified by {@link RegistrateBlockLootTables#dropSelf(Block)}. This is the default, so it is generally not necessary to call, unless for
     * undoing previous changes.
     * 
     * @return this {@link JunkBlockBuilder}
     */
    public JunkBlockBuilder<T, P> defaultLoot() {
        return loot(RegistrateBlockLootTables::dropSelf);
    }

    /**
     * Configure the loot table for this block. This is different than most data gen callbacks as the callback does not accept a {@link DataGenContext}, but instead a
     * {@link RegistrateBlockLootTables}, for creating specifically block loot tables.
     * <p>
     * If the block does not have a loot table (i.e. {@link Block.Properties#noLootTable()} is called) this action will be <em>skipped</em>.
     * 
     * @param cons
     *            The callback which will be invoked during block loot table creation.
     * @return this {@link JunkBlockBuilder}
     */
    public JunkBlockBuilder<T, P> loot(NonNullBiConsumer<RegistrateBlockLootTables, T> cons) {
        return (JunkBlockBuilder<T, P>) setData(ProviderType.LOOT, (ctx, prov) -> prov.addLootAction(LootType.BLOCK, tb -> {
            if (!ctx.getEntry().getLootTable().equals(BuiltInLootTables.EMPTY)) {
                cons.accept(tb, ctx.getEntry());
            }
        }));
    }

    /**
     * Configure the recipe(s) for this block.
     * 
     * @param cons
     *            The callback which will be invoked during data generation.
     * @return this {@link JunkBlockBuilder}
     * @see #setData(ProviderType, NonNullBiConsumer)
     */
    public JunkBlockBuilder<T, P> recipe(NonNullBiConsumer<DataGenContext<Block, T>, RegistrateRecipeProvider> cons) {
        return (JunkBlockBuilder<T, P>) setData(ProviderType.RECIPE, cons);
    }

    @Nullable
    private Function<T, NonNullSupplier<Supplier<IClientBlockExtensions>>> clientExtensionFunc;

    /**
     * Register a client extension for this block.
     * The {@link IClientBlockExtensions} instance can be shared across many items.
     *
     * @param clientExtension
     *            The client extension to register for this block
     * @return this {@link JunkBlockBuilder}
     */
    public JunkBlockBuilder<T, P> clientExtension(NonNullSupplier<Supplier<IClientBlockExtensions>> clientExtension) {
        if (this.clientExtensionFunc == null) {
            RegistrateDistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::registerClientExtension);
        }
        this.clientExtensionFunc = block -> clientExtension;
        return this;
    }

    protected void registerClientExtension() {
        OneTimeEventReceiver.addModListener(getOwner(), RegisterClientExtensionsEvent.class, e -> {
            if (this.clientExtensionFunc != null) {
                NonNullSupplier<Supplier<IClientBlockExtensions>> clientExtension = this.clientExtensionFunc.apply(getEntry());
                e.registerBlock(clientExtension.get().get(), getEntry());
            }
        });
    }

    @Override
    protected RegistryEntry<Block, T> createEntryWrapper(DeferredHolder<Block, T> delegate) {
        return new BlockEntry<>(getOwner(), delegate);
    }

    @Override
    public BlockEntry<T> register() {
        return super.register();
    }
}
