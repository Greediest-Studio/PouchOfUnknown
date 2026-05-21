package xyz.tcreopargh.pouchofunknown;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class PlayerInventoryListener implements IContainerListener {
    private static final Map<Container, Set<UUID>> LISTENING_PLAYERS = new WeakHashMap<>();

    private final EntityPlayerMP player;

    private final Map<Integer, Object> lastSlotStates = new HashMap<>();

    private boolean lock = false;

    public PlayerInventoryListener(EntityPlayerMP player) {
        this.player = player;
    }

    public static void addTo(Container container, EntityPlayerMP player) {
        Set<UUID> playerIds = LISTENING_PLAYERS.get(container);
        if (playerIds == null) {
            playerIds = new HashSet<>();
            LISTENING_PLAYERS.put(container, playerIds);
        }
        if (playerIds.add(player.getUniqueID())) {
            container.addListener(new PlayerInventoryListener(player));
        }
    }

    @Override
    public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {
        if(lock) {
            return;
        }
        try {
            lock = true;
            PouchOfUnknownEvents.detect(player);
        } finally {
            lock = false;
        }
    }

    @Override
    public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack) {
        if (lock) return;
        int playerSlot = getPlayerInventorySlot(containerToSend, slotInd);
        if (playerSlot < 0) {
            lastSlotStates.remove(slotInd);
            return;
        }
        if (stack.isEmpty()) {
            lastSlotStates.remove(slotInd);
            return;
        }

        Object lastState = lastSlotStates.get(slotInd);
        if (shouldSkipUpdate(stack, lastState)) return;

        lastSlotStates.put(slotInd, PouchConfig.ignoreNBT ? getSimpleKey(stack) : getStackHash(stack));

        try {
            lock = true;
            PouchOfUnknownEvents.detect(player, playerSlot);
        } finally {
            lock = false;
        }
    }

    private int getPlayerInventorySlot(Container container, int containerSlot) {
        if (containerSlot < 0 || containerSlot >= container.inventorySlots.size()) {
            return -1;
        }
        Slot slot = container.inventorySlots.get(containerSlot);
        if (slot.inventory != player.inventory) {
            return -1;
        }
        return slot.getSlotIndex();
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
        return stack.getItem().getRegistryName() + "#" + stack.getMetadata() + "#" + stack.getCount();
    }

    private int getStackHash(ItemStack stack) {
        int hash = stack.getItem().hashCode() ^ stack.getMetadata() ^ stack.getCount();
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
