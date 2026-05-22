package xyz.tcreopargh.pouchofunknown;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public class PlayerInventoryListener implements IContainerListener {
    private final EntityPlayerMP player;

    public PlayerInventoryListener(EntityPlayerMP player) {
        this.player = player;
    }

    public static void addTo(Container container, EntityPlayerMP player) {
        container.addListener(new PlayerInventoryListener(player));
    }

    @Override
    public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {
        PouchOfUnknownEvents.detect(player);
    }

    @Override
    public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack) {
        int playerSlot = getPlayerInventorySlot(containerToSend, slotInd);
        if (playerSlot >= 0) {
            PouchOfUnknownEvents.detect(player, playerSlot);
        }
    }

    private int getPlayerInventorySlot(Container container, int containerSlot) {
        if (containerSlot < 0 || containerSlot >= container.inventorySlots.size()) return -1;
        Slot slot = container.inventorySlots.get(containerSlot);
        if (slot.inventory != player.inventory) return -1;
        return slot.getSlotIndex();
    }

    @Override
    public void sendWindowProperty(Container containerIn, int varToUpdate, int newValue) {}

    @Override
    public void sendAllWindowProperties(Container containerIn, net.minecraft.inventory.IInventory inventory) {}
}