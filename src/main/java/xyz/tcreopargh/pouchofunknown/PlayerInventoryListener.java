package xyz.tcreopargh.pouchofunknown;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import java.util.HashMap;
import java.util.Map;

public class PlayerInventoryListener implements IContainerListener {
    private final EntityPlayerMP player;

    private final Map<Integer, Object> lastSlotStates = new HashMap<>();

    private boolean lock = false;

    public PlayerInventoryListener(EntityPlayerMP player) {
        this.player = player;
    }

    @Override
    public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {
        if(lock) {
            return;
        }
        lock = true;
        PouchOfUnknownEvents.detect(player);
        lock = false;
    }

    @Override
    public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack) {
        if (lock || stack.isEmpty()) return;

        Object lastState = lastSlotStates.get(slotInd);
        if (shouldSkipUpdate(stack, lastState)) return;

        lastSlotStates.put(slotInd, PouchConfig.ignoreNBT ? getSimpleKey(stack) : getStackHash(stack));

        lock = true;
        PouchOfUnknownEvents.detect(player, slotInd);
        lock = false;
    }

    private boolean shouldSkipUpdate(ItemStack current, Object previousState) {
        if (previousState == null || current.isEmpty()) return false;

        if (PouchConfig.ignoreNBT) {
            String currentKey = getSimpleKey(current);
            return currentKey.equals(previousState);
        } else {
            int currentHash = getStackHash(current);
            return currentHash == (int) previousState;
        }
    }

    private String getSimpleKey(ItemStack stack) {
        return stack.getItem().getRegistryName() + "#" + stack.getCount();
    }

    private int getStackHash(ItemStack stack) {
        int hash = stack.getItem().hashCode() ^ stack.getCount();
        if (stack.hasTagCompound()) {
            if (stack.getTagCompound() != null) {
                hash ^= stack.getTagCompound().hashCode();
            }
        }
        return hash;
    }

    @Override
    public void sendWindowProperty(Container containerIn, int varToUpdate, int newValue) {

    }

    @Override
    public void sendAllWindowProperties(Container containerIn, IInventory inventory) {

    }
}
