package xyz.tcreopargh.pouchofunknown;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static xyz.tcreopargh.pouchofunknown.PouchOfUnknownEvents.MAX_SLOT_NUMBER;
import static xyz.tcreopargh.pouchofunknown.PouchOfUnknownEvents.isValidPouch;

public class Util {

    public static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound();
        } else {
            NBTTagCompound newTag = new NBTTagCompound();
            stack.setTagCompound(newTag);
            return newTag;
        }
    }

    public static NBTBase getOrCreateSubtag(NBTTagCompound tagCompound, String key, NBTBase defaultTag) {
        NBTBase existing = tagCompound.getTag(key);
        if (existing != null && existing.getId() == defaultTag.getId()) {
            return existing;
        }
        tagCompound.setTag(key, defaultTag);
        return defaultTag;
    }

    public static ItemStack insertItem(ItemStack pouch, ItemStack stack) {
        NBTTagList tagList = (NBTTagList) getOrCreateSubtag(
                getOrCreateTag(pouch),
                ItemPouchOfUnknown.INVENTORY_TAG_NAME,
                new NBTTagList()
        );

        ItemStack remnant = stack;

        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTBase itemTag = tagList.get(i);
            if (itemTag instanceof NBTTagCompound) {
                ItemStack tagStack = new ItemStack((NBTTagCompound) itemTag);
                StackResult result = stackItem(tagStack, remnant);
                remnant = result.getRemnant();
                tagList.set(i, result.getResult().serializeNBT());
                if (remnant.isEmpty()) break;
            }
        }

        if (tagList.tagCount() < PouchConfig.pouchCapacity && !remnant.isEmpty()) {
            tagList.appendTag(remnant.serializeNBT());
            remnant = ItemStack.EMPTY;
        }

        Objects.requireNonNull(pouch.getTagCompound())
                .setTag(ItemPouchOfUnknown.INVENTORY_TAG_NAME, tagList);

        return remnant;
    }

    public static ItemStack extractItem(ItemStack pouch) {
        NBTTagList tagList = (NBTTagList) getOrCreateSubtag(
                getOrCreateTag(pouch),
                ItemPouchOfUnknown.INVENTORY_TAG_NAME,
                new NBTTagList()
        );

        int size = tagList.tagCount();
        if (size == 0) return ItemStack.EMPTY;

        NBTBase itemTag = tagList.get(size - 1);
        ItemStack ret = ItemStack.EMPTY;

        if (itemTag instanceof NBTTagCompound) {
            ret = new ItemStack(((NBTTagCompound) itemTag).copy());
            tagList.removeTag(size - 1);
        }

        Objects.requireNonNull(pouch.getTagCompound())
                .setTag(ItemPouchOfUnknown.INVENTORY_TAG_NAME, tagList);

        return ret;
    }

    public static List<ItemStack> getItems(ItemStack pouch) {
        NBTTagList tagList = (NBTTagList) getOrCreateSubtag(
                getOrCreateTag(pouch),
                ItemPouchOfUnknown.INVENTORY_TAG_NAME,
                new NBTTagList()
        );

        List<ItemStack> stackList = new ArrayList<>();
        for (NBTBase tag : tagList) {
            if (tag instanceof NBTTagCompound) {
                stackList.add(new ItemStack((NBTTagCompound) tag));
            }
        }
        return stackList;
    }

    public static void setItems(ItemStack pouch, List<ItemStack> items) {
        NBTTagCompound tag = getOrCreateTag(pouch);
        NBTTagList newList = new NBTTagList();

        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                newList.appendTag(stack.serializeNBT());
            }
        }

        tag.setTag(ItemPouchOfUnknown.INVENTORY_TAG_NAME, newList);
        pouch.setTagCompound(tag);
    }

    public static StackResult stackItem(ItemStack existing, ItemStack incoming) {
        int space = incoming.getMaxStackSize();

        if (!existing.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(incoming, existing)) {
                return new StackResult(existing, incoming);
            }
            space -= existing.getCount();
        }

        if (space <= 0) {
            return new StackResult(existing, incoming);
        }

        boolean overflow = incoming.getCount() > space;

        if (existing.isEmpty()) {
            existing = overflow
                    ? ItemHandlerHelper.copyStackWithSize(incoming, space)
                    : incoming;
        } else {
            existing.grow(overflow ? space : incoming.getCount());
        }

        ItemStack remnant = overflow
                ? ItemHandlerHelper.copyStackWithSize(incoming, incoming.getCount() - space)
                : ItemStack.EMPTY;

        return new StackResult(existing, remnant);
    }

    public static class StackResult {
        private final ItemStack result;
        private final ItemStack remnant;

        public StackResult(ItemStack result, ItemStack remnant) {
            this.result = result;
            this.remnant = remnant;
        }

        public ItemStack getResult() {
            return result;
        }

        public ItemStack getRemnant() {
            return remnant;
        }
    }

    public static ItemStack findValidPouch(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (isValidPouch(stack)) return stack;
        }

        for (int i = 0; i < MAX_SLOT_NUMBER; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isValidPouch(stack)) return stack;
        }

        return ItemStack.EMPTY;
    }
}