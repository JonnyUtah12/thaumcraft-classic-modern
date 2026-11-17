package com.jonnyutah12.thaumcraftclassic.loot;

import com.jonnyutah12.thaumcraftclassic.registration.ModLootNumberProviders;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

/**
 * SPINE-1 loot rolls for the {@code blockcustomore} drop tables.
 *
 * <p>Reproduces the original 1.7.10 curve verbatim, from
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/blocks/BlockCustomOre.java}
 * getDrops:</p>
 * <pre>{@code int q = 1 + world.rand.nextInt(2 + fortune); }</pre>
 * <p>i.e. a uniform draw over {@code [base, base + span - 1 + fortune]} — the
 * fortune level is added INSIDE the {@code nextInt} bound, not multiplied
 * after the fact (vanilla's {@code ore_drops} bonus formula does that, and it
 * is NOT the original's shape). The JSON form:</p>
 * <pre>{@code "rolls": { "type": "thaumcraft:fortuned_uniform", "base": 1, "span": 2 } }</pre>
 * <p>gives exactly {@code 1 + rand.nextInt(2 + fortune)}. The enchantment is
 * hardcoded to {@code minecraft:fortune} because the original hardcoded its
 * {@code fortune} parameter (the level the pickaxe carried); the tool is read
 * from {@link LootContextParams#TOOL} exactly like {@code ApplyBonusCount}
 * does.</p>
 *
 * <p>{@code minecraft:fortune} is a DATA-PACK registry in 1.21.1 (it is not
 * in BuiltInRegistries), so its holder resolves lazily at roll time from the
 * context level — the house rule for datapack-registry holders.</p>
 */
public record FortunedUniformProvider(int base, int span) implements NumberProvider {

    public static final MapCodec<FortunedUniformProvider> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.intRange(1, 64).optionalFieldOf("base", 1).forGetter(FortunedUniformProvider::base),
                    Codec.intRange(1, 64).optionalFieldOf("span", 2).forGetter(FortunedUniformProvider::span))
                    .apply(instance, FortunedUniformProvider::new));

    @Override
    public LootNumberProviderType getType() {
        return ModLootNumberProviders.FORTUNED_UNIFORM.get();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.TOOL);
    }

    @Override
    public float getFloat(final LootContext context) {
        final ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        int fortune = 0;
        if (tool != null) {
            final Holder<Enchantment> fortuneEnchantment = context.getLevel().registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FORTUNE);
            fortune = EnchantmentHelper.getItemEnchantmentLevel(fortuneEnchantment, tool);
        }
        return this.base + context.getRandom().nextInt(this.span + fortune);
    }
}
