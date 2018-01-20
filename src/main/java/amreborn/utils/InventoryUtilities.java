package amreborn.utils;

import amreborn.entity.EntityBroomInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.oredict.OreDictionary;

public class InventoryUtilities{
	public static int decrementStackQuantity(IInventory inventory, int slotIndex, int quantity){
		int deducted = 0;
		ItemStack stack = inventory.getStackInSlot(slotIndex);
		if (stack != ItemStack.EMPTY){
			if (stack.getCount() < quantity)
				quantity = stack.getCount();
			stack.shrink(quantity);
			deducted = quantity;
			if (stack.getCount() <= 0){
				inventory.setInventorySlotContents(slotIndex, ItemStack.EMPTY);
			} else {
				inventory.setInventorySlotContents(slotIndex, stack);
			}
		}
		return deducted;
	}

	public static boolean mergeIntoInventory(IInventory inventory, ItemStack toMerge){
		return mergeIntoInventory(inventory, toMerge, toMerge.getCount());
	}

	public static boolean mergeIntoInventory(IInventory inventory, ItemStack toMerge, int quantity){

		if (quantity > toMerge.getCount())
			quantity = toMerge.getCount();

		int qty = quantity;
		int emptySlot = -1;
		for (int i = 0; i < inventory.getSizeInventory(); ++i){
			if (!inventory.isItemValidForSlot(i, toMerge))
				continue;
			ItemStack inventoryStack = inventory.getStackInSlot(i);
			if (inventoryStack == ItemStack.EMPTY){
				if (emptySlot == -1)
					emptySlot = i;
				continue;
			}
			if (compareItemStacks(inventoryStack, toMerge, true, false, true, true)){
				if (inventoryStack.getCount() == inventoryStack.getMaxStackSize())
					continue;
				if (inventoryStack.getMaxStackSize() - inventoryStack.getCount()>= qty){
					inventoryStack.grow(qty);
					toMerge.shrink(qty);
					return true;
				}else{
					qty -= (inventoryStack.getMaxStackSize() - inventoryStack.getCount());
					inventoryStack.setCount(inventoryStack.getMaxStackSize());
				}
			}
		}

		if (qty > 0 && emptySlot > -1){
			ItemStack temp = toMerge.copy();
			temp.setCount(qty);
			inventory.setInventorySlotContents(emptySlot, temp);
			toMerge.shrink(qty);
			return true;
		}

		toMerge.setCount(qty);

		return false;
	}

	public static boolean mergeIntoInventory(IInventory inventory, ItemStack toMerge, int quantity, EnumFacing side){
		if (inventory instanceof ISidedInventory){
			ItemStack stack = toMerge.splitStack(Math.min(toMerge.getCount(), quantity));
			ISidedInventory sidedInventory = (ISidedInventory)inventory;
			int[] slots = sidedInventory.getSlotsForFace(side);
			boolean flag = false;

			for (int i = 0; i < slots.length && stack != ItemStack.EMPTY && stack.getCount() > 0; ++i){
				//For each slot that can be accessed from this side
				ItemStack prvStack = sidedInventory.getStackInSlot(slots[i]);
				if (InventoryUtilities.canInsertItemToInventory(sidedInventory, stack, slots[i], side)){
					//if the items can be inserted into the current slot
					if (prvStack == ItemStack.EMPTY){
						//if the stack in the slot is null then get the max value that can be moved and transfer the stack to the inventory
						int max = Math.min(stack.getMaxStackSize(), sidedInventory.getInventoryStackLimit());
						if (max >= stack.getCount()){
							sidedInventory.setInventorySlotContents(slots[i], stack.copy());
							stack.setCount(0);
							flag = true;
						}else{
							sidedInventory.setInventorySlotContents(slots[i], stack.splitStack(max));
							flag = true;
						}
					}else if (InventoryUtilities.canStacksMerge(prvStack, stack)){
						//if the stack in the slot can be merged with the stack we are trying to move get the max items that can exist in the slot
						//and insert as many as will fit from the stack we are trying to move
						int max = Math.min(stack.getMaxStackSize(), sidedInventory.getInventoryStackLimit());
						if (max > prvStack.getCount()){
							int qty = Math.min(stack.getCount(), max - prvStack.getCount());
							prvStack.grow(qty);
							stack.shrink(qty);
							flag = qty > 0;
						}
					}
				}
			}

			toMerge.setCount(toMerge.getCount() + stack.getCount());
			return flag;
		}else{
			return mergeIntoInventory(inventory, toMerge, quantity);
		}
	}

	public static boolean deductFromInventory(IInventory inventory, ItemStack search, int quantity){
		for (int i = 0; i < inventory.getSizeInventory(); ++i){
			ItemStack inventoryStack = inventory.getStackInSlot(i);
			if (inventoryStack == ItemStack.EMPTY) continue;
			if (compareItemStacks(inventoryStack, search, true, false, true, true)){
				if (search.getCount() <= 0){
					inventory.setInventorySlotContents(i, ItemStack.EMPTY);
					return true;
				}else{
					quantity -= decrementStackQuantity(inventory, i, quantity);
					if (quantity <= 0){
						if (inventory.getStackInSlot(i) != ItemStack.EMPTY && inventory.getStackInSlot(i).getCount() <= 0){
							inventory.setInventorySlotContents(i, ItemStack.EMPTY);
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean inventoryHasItem(IInventory inventory, ItemStack search, int quantity){
		int qtyFound = 0;
		for (int i = 0; i < inventory.getSizeInventory(); ++i){
			ItemStack inventoryStack = inventory.getStackInSlot(i);
			if (inventoryStack == ItemStack.EMPTY) continue;
			if (compareItemStacks(inventoryStack, search, true, false, true, true)){
				qtyFound += inventoryStack.getCount();
				if (qtyFound >= quantity)
					return true;
			}
		}
		return false;
	}

	public static boolean inventoryHasItem(IInventory inventory, ItemStack search, int quantity, EnumFacing side){
		if (inventory instanceof ISidedInventory){
			ISidedInventory sidedInventory = (ISidedInventory)inventory;
			int qtyFound = 0;
			int[] slots = sidedInventory.getSlotsForFace(side);

			for (int i = 0; i < slots.length; i++){
				ItemStack inventoryStack = inventory.getStackInSlot(slots[i]);
				if (inventoryStack == ItemStack.EMPTY)
					continue;
				else if (compareItemStacks(inventoryStack, search, true, false, true, true)){
					qtyFound += inventoryStack.getCount();
					if (qtyFound >= quantity)
						return true;
				}
			}

			return false;
		}else{
			return inventoryHasItem(inventory, search, quantity);
		}
	}

	public static int getFirstBlockInInventory(IInventory inventory){
		for (int i = 0; i < inventory.getSizeInventory(); ++i){
			ItemStack inventoryStack = inventory.getStackInSlot(i);
			if (inventoryStack == ItemStack.EMPTY) continue;
			if (inventoryStack.getItem() instanceof ItemBlock)
				return i;
		}
		return -1;
	}

	public static boolean isInventoryFull(IInventory inventory){
		for (int i = 0; i < inventory.getSizeInventory(); ++i){
			ItemStack inventoryStack = inventory.getStackInSlot(i);
			if (inventoryStack == ItemStack.EMPTY) return false;
		}
		return true;
	}

	/**
	 * Returns true if any items from source can be merged into dest.
	 */
	public static boolean canMergeHappen(IInventory source, IInventory dest){
		for (int i = 0; i < source.getSizeInventory(); ++i){
			if (source.getStackInSlot(i) == ItemStack.EMPTY) continue;
			if (inventoryHasRoomFor(dest, source.getStackInSlot(i))){
				return true;
			}
		}
		return false;
	}

	public static boolean isInventoryEmpty(IInventory inventory){
		for (int i = 0; i < inventory.getSizeInventory(); ++i){
			ItemStack inventoryStack = inventory.getStackInSlot(i);
			if (inventoryStack != ItemStack.EMPTY) return false;
		}
		return true;
	}

	public static boolean inventoryHasRoomFor(IInventory inventory, ItemStack stack){
		return inventoryHasRoomFor(inventory, stack, stack.getCount());
	}

	public static boolean inventoryHasRoomFor(IInventory inventory, ItemStack stack, int qty){
		for (int i = 0; i < inventory.getSizeInventory(); ++i){
			ItemStack invStack = inventory.getStackInSlot(i);
			if (invStack == ItemStack.EMPTY)
				return true;
			if (compareItemStacks(invStack, stack, true, false, true, true) && invStack.getMaxStackSize() - invStack.getCount() >= qty)
				return true;
		}
		return false;
	}

	public static boolean inventoryHasRoomFor(IInventory inventory, ItemStack stack, int qty, EnumFacing side){
		if (inventory instanceof ISidedInventory){
			ISidedInventory sidedInventory = (ISidedInventory)inventory;
			int[] slots = sidedInventory.getSlotsForFace(side);

			for (int i = 0; i < slots.length; i++){
				ItemStack invStack = inventory.getStackInSlot(slots[i]);
				if (invStack == ItemStack.EMPTY)
					return true;
				if (compareItemStacks(invStack, stack, true, false, true, true) && invStack.getMaxStackSize() - invStack.getCount() >= qty)
					return true;
			}

			return false;
		}else{
			return inventoryHasRoomFor(inventory, stack, qty);
		}
	}

	public static ItemStack getFirstStackInInventory(EntityBroomInventory inventory){
		for (int i = 0; i < inventory.getSizeInventory(); ++i){
			ItemStack invStack = inventory.getStackInSlot(i);
			if (invStack != ItemStack.EMPTY){
				return invStack;
			}
		}
		return ItemStack.EMPTY;
	}

	public static int getInventorySlotIndexFor(IInventory inventory, Item item){
		return getInventorySlotIndexFor(inventory, item, Short.MAX_VALUE);
	}

	public static int getInventorySlotIndexFor(IInventory inventory, Item item, int metadata){
		for (int i = 0; i < inventory.getSizeInventory(); ++i){
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack != ItemStack.EMPTY && stack.getItem() == item && (stack.getItemDamage() == metadata || metadata == Short.MAX_VALUE))
				return i;
		}
		return -1;
	}

	public static int getInventorySlotIndexFor(IInventory inventory, ItemStack search){
		for (int i = 0; i < inventory.getSizeInventory(); ++i){
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack != ItemStack.EMPTY && compareItemStacks(stack, search, false, false, true, true))
				return i;
		}
		return -1;
	}

	public static boolean canStacksMerge(ItemStack stack1, ItemStack stack2){
		if (stack1 == ItemStack.EMPTY || stack2 == ItemStack.EMPTY)
			return false;

		if (!compareItemStacks(stack1, stack2, true, false, true, true))
			return false;

		return true;
	}

	public static int getLikeItemCount(IInventory inventory, ItemStack stack){
		int totalCount = 0;
		for (int i = 0; i < inventory.getSizeInventory(); ++i){
			ItemStack invStack = inventory.getStackInSlot(i);
			if (invStack != ItemStack.EMPTY && compareItemStacks(invStack, stack, true, false, true, true))
				totalCount += invStack.getCount();
		}

		return totalCount;
	}

	public static int getLikeItemCount(IInventory inventory, ItemStack stack, EnumFacing side){
		if (inventory instanceof ISidedInventory){
			int totalCount = 0;
			ISidedInventory sidedInventory = (ISidedInventory)inventory;
			int[] slots = sidedInventory.getSlotsForFace(side);

			for (int i = 0; i < slots.length; i++){
				ItemStack invStack = inventory.getStackInSlot(slots[i]);
				if (invStack != ItemStack.EMPTY && compareItemStacks(invStack, stack, true, false, true, true))
					totalCount += invStack.getCount();
			}

			return totalCount;
		}else{
			return getLikeItemCount(inventory, stack);
		}
	}

	public static boolean canInsertItemToInventory(IInventory inventory, ItemStack itemStack, int slot, EnumFacing side){
		return !inventory.isItemValidForSlot(slot, itemStack) ? false : !(inventory instanceof ISidedInventory) || ((ISidedInventory)inventory).canInsertItem(slot, itemStack, side);
	}

	private static boolean canExtractItemFromInventory(IInventory inventory, ItemStack itemStack, int slot, EnumFacing side){
		return !(inventory instanceof ISidedInventory) || ((ISidedInventory)inventory).canExtractItem(slot, itemStack, side);
	}

	public static GetFirstStackStartingFromSlotResult getFirstStackStartingFromSlot(IInventory inventory, ItemStack itemStack, int slot){
		for (int i = slot; i < inventory.getSizeInventory(); i++){
			itemStack = inventory.getStackInSlot(i);
			if (itemStack != ItemStack.EMPTY){
				return new GetFirstStackStartingFromSlotResult(i, itemStack);
			}
		}

		return new GetFirstStackStartingFromSlotResult(-1, ItemStack.EMPTY);
	}

	public static GetFirstStackStartingFromSlotResult getFirstStackStartingFromSlot(IInventory inventory, ItemStack itemStack, int slot, EnumFacing side){
		if (inventory instanceof ISidedInventory){
			ISidedInventory sidededInventory = (ISidedInventory)inventory;
			int[] slots = sidededInventory.getSlotsForFace(side);

			for (int i = slot; i < slots.length; i++){
				itemStack = inventory.getStackInSlot(slots[i]);
				if (itemStack != ItemStack.EMPTY && canExtractItemFromInventory(sidededInventory, itemStack, slots[i], side)){
					return new GetFirstStackStartingFromSlotResult(i, itemStack);
				}
			}
		}else{
			return getFirstStackStartingFromSlot(inventory, itemStack, slot);
		}

		return new GetFirstStackStartingFromSlotResult(-1, ItemStack.EMPTY);
	}

	public static TileEntityChest getAdjacentChest(TileEntityChest chest){
		TileEntityChest adjacent = null;

		if (chest.adjacentChestXNeg != null)
			adjacent = chest.adjacentChestXNeg;
		else if (chest.adjacentChestXPos != null)
			adjacent = chest.adjacentChestXPos;
		else if (chest.adjacentChestZPos != null)
			adjacent = chest.adjacentChestZPos;
		else if (chest.adjacentChestZNeg != null)
			adjacent = chest.adjacentChestZNeg;

		return adjacent;
	}

	public static ItemStack replaceItem(ItemStack originalStack, Item newItem){
		ItemStack stack = new ItemStack(newItem, originalStack.getCount(), originalStack.getItemDamage());
		if (originalStack.hasTagCompound())
			stack.setTagCompound(originalStack.getTagCompound());
		return stack;
	}

	public static boolean compareItemStacks(ItemStack a, ItemStack b, boolean matchMeta, boolean matchStackSize, boolean matchNBT, boolean allowAnyMeta){
		if (a == ItemStack.EMPTY || b == ItemStack.EMPTY)
			return false;

		if (a.getItem() != b.getItem())
			return false;

		if (allowAnyMeta && !(a.getItemDamage() == b.getItemDamage() || a.getItemDamage() == OreDictionary.WILDCARD_VALUE || b.getItemDamage() == OreDictionary.WILDCARD_VALUE)){
			return false;
		}else if (matchMeta && a.getItemDamage() != b.getItemDamage()){
			return false;
		}

		if (matchStackSize && a.getCount() != b.getCount())
			return false;

		if (matchNBT && !ItemStack.areItemStackTagsEqual(a, b))
			return false;

		return true;
	}
}
