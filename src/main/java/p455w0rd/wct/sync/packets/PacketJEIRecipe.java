/*
 * This file is part of Wireless Crafting Terminal. Copyright (c) 2017, p455w0rd
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
package p455w0rd.wct.sync.packets;

import java.io.*;

import javax.annotation.Nonnull;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.IContainerCraftingPacket;
import appeng.items.storage.ItemViewCell;
import appeng.util.Platform;
import appeng.util.helpers.ItemHandlerUtil;
import appeng.util.inv.AdaptorItemHandler;
import appeng.util.inv.WrapperInvItemHandler;
import appeng.util.item.AEItemStack;
import appeng.util.prioritylist.IPartitionList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import p455w0rd.ae2wtlib.api.ICustomWirelessTermHandler;
import p455w0rd.ae2wtlib.helpers.WTGuiObject;
import p455w0rd.wct.api.IWirelessCraftingTerminalItem;
import p455w0rd.wct.container.ContainerWCT;
import p455w0rd.wct.sync.WCTPacket;
import p455w0rd.wct.sync.network.INetworkInfo;

public class PacketJEIRecipe extends WCTPacket {

	private ItemStack[][] recipe;

	// automatic.
	public PacketJEIRecipe(final ByteBuf stream) throws IOException {
		final ByteArrayInputStream bytes = getPacketByteArray(stream);
		bytes.skip(stream.readerIndex());
		final NBTTagCompound comp = CompressedStreamTools.readCompressed(bytes);
		if (comp != null) {
			recipe = new ItemStack[9][];
			for (int x = 0; x < recipe.length; x++) {
				final NBTTagList list = comp.getTagList("#" + x, 10);
				if (list.tagCount() > 0) {
					recipe[x] = new ItemStack[list.tagCount()];
					for (int y = 0; y < list.tagCount(); y++) {
						recipe[x][y] = new ItemStack(list.getCompoundTagAt(y));
					}
				}
			}
		}
	}

	// api
	public PacketJEIRecipe(final NBTTagCompound recipe) throws IOException {
		final ByteBuf data = Unpooled.buffer();

		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		final DataOutputStream outputStream = new DataOutputStream(bytes);

		data.writeInt(getPacketID());

		CompressedStreamTools.writeCompressed(recipe, outputStream);
		data.writeBytes(bytes.toByteArray());

		configureWrite(data);
	}

	private WTGuiObject<IAEItemStack, IItemStorageChannel> getGuiObject(final ItemStack it, final EntityPlayer player, final World w, final int x, final int y, final int z) {
		if (it != null) {
			final ICustomWirelessTermHandler wh = (ICustomWirelessTermHandler) AEApi.instance().registries().wireless().getWirelessTerminalHandler(it);
			if (wh != null) {
				return new WTGuiObject<IAEItemStack, IItemStorageChannel>(wh, it, player, w, x, y, z);
			}
		}

		return null;
	}

	@Override
	public void serverPacketData(final INetworkInfo manager, final WCTPacket packet, final EntityPlayer player) {
		final EntityPlayerMP pmp = (EntityPlayerMP) player;
		final Container con = pmp.openContainer;

		if (con instanceof IContainerCraftingPacket && con instanceof ContainerWCT) {
			final IContainerCraftingPacket cct = (IContainerCraftingPacket) con;
			IGridNode node = cct.getNetworkNode();
			if (node == null) {
				ItemStack wct = ((ContainerWCT) con).getWirelessTerminal();
				if (wct.isEmpty() || !(wct.getItem() instanceof IWirelessCraftingTerminalItem)) {
					return;
				}
				WTGuiObject<IAEItemStack, IItemStorageChannel> obj = getGuiObject(wct, player, player.getEntityWorld(), (int) player.posX, (int) player.posY, (int) player.posZ);
				if (obj != null) {
					node = obj.getActionableNode(((IWirelessCraftingTerminalItem) wct.getItem()).checkForBooster(wct));
				}
			}

			if (node != null) {
				final IGrid grid = node.getGrid();
				if (grid == null) {
					return;
				}

				final IStorageGrid inv = grid.getCache(IStorageGrid.class);
				final IEnergyGrid energy = grid.getCache(IEnergyGrid.class);
				final ISecurityGrid security = grid.getCache(ISecurityGrid.class);
				final ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
				final IItemHandler craftMatrix = cct.getInventoryByName("crafting");
				final IItemHandler playerInventory = cct.getInventoryByName("player");

				if (inv != null && recipe != null && security != null) {

					final IMEMonitor<IAEItemStack> storage = inv.getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
					final IPartitionList<IAEItemStack> filter = ItemViewCell.createFilter(cct.getViewCells());

					for (int x = 0; x < craftMatrix.getSlots(); x++) {
						ItemStack currentItem = craftMatrix.getStackInSlot(x);

						// prepare slots
						if (!currentItem.isEmpty()) {
							// already the correct item?
							ItemStack newItem = canUseInSlot(x, currentItem);

							// put away old item
							if (newItem != currentItem && security.hasPermission(player, SecurityPermissions.INJECT)) {
								final IAEItemStack in = AEItemStack.fromItemStack(currentItem);
								final IAEItemStack out = cct.useRealItems() ? Platform.poweredInsert(energy, storage, in, cct.getActionSource()) : null;
								if (out != null) {
									currentItem = out.createItemStack();
								}
								else {
									currentItem = ItemStack.EMPTY;
								}
							}
						}

						if (currentItem.isEmpty() && recipe[x] != null) {
							// for each variant
							for (int y = 0; y < recipe[x].length && currentItem.isEmpty(); y++) {
								final IAEItemStack request = AEItemStack.fromItemStack(recipe[x][y]);
								if (request != null) {
									// try ae
									if ((filter == null || filter.isListed(request)) && security.hasPermission(player, SecurityPermissions.EXTRACT)) {
										request.setStackSize(1);
										IAEItemStack out;
										if (cct.useRealItems()) {
											out = Platform.poweredExtraction(energy, storage, request, cct.getActionSource());
										}
										else {
											// Query the crafting grid if there is a pattern providing the item
											if (!crafting.getCraftingFor(request, null, 0, null).isEmpty()) {
												out = request;
											}
											else {
												// Fall back using an existing item
												out = storage.extractItems(request, Actionable.SIMULATE, cct.getActionSource());
											}
										}

										if (out != null) {
											currentItem = out.createItemStack();
										}
									}

									// try inventory
									if (currentItem.isEmpty()) {
										AdaptorItemHandler ad = new AdaptorItemHandler(playerInventory);

										if (cct.useRealItems()) {
											currentItem = ad.removeItems(1, recipe[x][y], null);
										}
										else {
											currentItem = ad.simulateRemove(1, recipe[x][y], null);
										}
									}
								}
							}
						}
						ItemHandlerUtil.setStackInSlot(craftMatrix, x, currentItem);
					}
					con.onCraftMatrixChanged(new WrapperInvItemHandler(craftMatrix));
				}
			}
		}
	}

	@Nonnull
	private ItemStack canUseInSlot(int slot, ItemStack is) {
		if (recipe[slot] != null) {
			for (ItemStack option : recipe[slot]) {
				if (is.isItemEqual(option)) {
					return is;
				}
			}
		}
		return ItemStack.EMPTY;
	}

}
