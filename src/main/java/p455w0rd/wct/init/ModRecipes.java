/*
 * This file is part of Wireless Crafting Terminal. Copyright (c) 2016, p455w0rd
 * (aka TheRealp455w0rd), All rights reserved unless otherwise stated.
 *
 * Wireless Crafting Terminal is free software: you can redistribute it and/or
 * modify it under the terms of the MIT License.
 *
 * Wireless Crafting Terminal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the MIT License for
 * more details.
 *
 * You should have received a copy of the MIT License along with Wireless
 * Crafting Terminal. If not, see <https://opensource.org/licenses/MIT>.
 */
package p455w0rd.wct.init;

import appeng.core.Api;
import appeng.core.ApiDefinitions;
import appeng.core.api.definitions.ApiMaterials;
import appeng.core.api.definitions.ApiParts;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import p455w0rdslib.util.RecipeUtils;

/**
 * @author p455w0rd
 *
 */
public class ModRecipes {

	public static IRecipe WCT, MAGNET_CARD, BOOSTER_CARD;

	@SuppressWarnings("deprecation")
	public static void init() {
		if (ModConfig.WCT_MINETWEAKER_OVERRIDE) {
			return;
		}
		ApiDefinitions defs = Api.INSTANCE.definitions();
		ApiMaterials materials = defs.materials();
		ApiParts parts = defs.parts();
		ItemStack wap = materials.wirelessReceiver().maybeStack(1).get();
		ItemStack wt = defs.items().wirelessTerminal().maybeStack(1).get();
		ItemStack fluixPearl = materials.fluixPearl().maybeStack(1).get();
		ItemStack entangledSingularity = materials.qESingularity().maybeStack(1).get();
		ItemStack craftingTerminal = parts.craftingTerminal().maybeStack(1).get();
		ItemStack advancedCard = materials.advCard().maybeStack(1).get();
		ItemStack annihilationPlane = parts.annihilationPlane().maybeStack(1).get();

		ForgeRegistries.RECIPES.register(WCT = new ShapelessRecipes(ModGlobals.MODID + ":wct", new ItemStack(ModItems.WCT), RecipeUtils.createInput(new Object[] {
				wt,
				fluixPearl,
				craftingTerminal
		})).setRegistryName(new ResourceLocation(ModGlobals.MODID, "wct")));

		//RecipeUtils.addShapeless(new ItemStack(ModItems.WCT), wt, fluixPearl, craftingTerminal));

		ForgeRegistries.RECIPES.register(MAGNET_CARD = RecipeUtils.addOldShaped(new ItemStack(ModItems.MAGNET_CARD), new Object[] {
				"abc",
				"ded",
				"ddd",
				Character.valueOf('a'),
				"blockRedstone",
				Character.valueOf('b'),
				annihilationPlane,
				Character.valueOf('c'),
				"blockLapis",
				Character.valueOf('d'),
				"blockIron",
				Character.valueOf('e'),
				advancedCard
		}));

		if (!ModConfig.WCT_DISABLE_BOOSTER_RECIPE) {
			ForgeRegistries.RECIPES.register(BOOSTER_CARD = RecipeUtils.addOldShaped(new ItemStack(ModItems.BOOSTER_CARD), new Object[] {
					"a a",
					" b ",
					"   ",
					Character.valueOf('a'),
					entangledSingularity,
					Character.valueOf('b'),
					wap
			}));
		}
	}

}