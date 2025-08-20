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

    private final Map<Integer, ItemStack> lastSlotStates = new HashMap<>();

    private final EntityPlayerMP player;

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

        ItemStack last = lastSlotStates.get(slotInd);
        if (shouldSkipUpdate(stack, last)) return;

        lastSlotStates.put(slotInd, stack.copy());
        lock = true;
        PouchOfUnknownEvents.detect(player, slotInd);
        lock = false;
    }

    private boolean shouldSkipUpdate(ItemStack current, ItemStack previous) {
        return previous != null &&
                ItemStack.areItemsEqual(current, previous) &&
                current.getCount() == previous.getCount() &&
                (PouchConfig.ignoreNBT || ItemStack.areItemStackTagsEqual(current, previous));
    }

    @Override
    public void sendWindowProperty(Container containerIn, int varToUpdate, int newValue) {

    }

    @Override
    public void sendAllWindowProperties(Container containerIn, IInventory inventory) {

    }
}
