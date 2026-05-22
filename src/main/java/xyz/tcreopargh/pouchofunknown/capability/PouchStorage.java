package xyz.tcreopargh.pouchofunknown.capability;

import net.darkhax.gamestages.GameStageHelper;
import net.darkhax.itemstages.ItemStages;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.items.ItemHandlerHelper;
import xyz.tcreopargh.pouchofunknown.ItemKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class PouchStorage implements IPouchStorage, ICapabilitySerializable<NBTTagCompound> {

    // 无 NBT 物品：ItemKey -> 总数
    private final Map<ItemKey, Integer> simpleItems = new HashMap<>();
    // 有 NBT 物品：保留完整堆叠列表（通常数量极少）
    private final List<ItemStack> complexItems = new ArrayList<>();

    @Override
    public ItemStack deposit(@Nullable String stage, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemKey key = ItemKey.from(stack);

        if (!stack.hasTagCompound()) {
            // 无 NBT，直接合并数量
            simpleItems.merge(key, stack.getCount(), Integer::sum);
            return ItemStack.EMPTY;
        } else {
            // 有 NBT，尝试与已有堆叠合并
            int remaining = stack.getCount();
            for (ItemStack existing : complexItems) {
                if (ItemHandlerHelper.canItemStacksStack(existing, stack)) {
                    int limit = existing.getMaxStackSize() - existing.getCount();
                    if (limit <= 0) continue;
                    int transfer = Math.min(remaining, limit);
                    existing.grow(transfer);
                    remaining -= transfer;
                    if (remaining <= 0) return ItemStack.EMPTY;
                }
            }
            if (remaining > 0) {
                ItemStack newStack = stack.copy();
                newStack.setCount(remaining);
                complexItems.add(newStack);
                return ItemStack.EMPTY;
            }
            return ItemStack.EMPTY;
        }
    }

    @Override
    public List<ItemStack> withdraw(EntityPlayer player, boolean takeAll) {
        List<ItemStack> result = new ArrayList<>();

        // 从简单计数表取出
        Iterator<Map.Entry<ItemKey, Integer>> iter = simpleItems.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<ItemKey, Integer> entry = iter.next();
            ItemKey key = entry.getKey();
            ItemStack sample = key.toStack();
            String stage = ItemStages.getStage(sample);
            if (stage == null || GameStageHelper.hasStage(player, stage)) {
                int total = entry.getValue();
                if (total <= 0) {
                    iter.remove();
                    continue;
                }
                int toTake = takeAll ? total : Math.min(total, sample.getMaxStackSize());
                ItemStack taken = sample.copy();
                taken.setCount(toTake);
                result.add(taken);

                int remainder = total - toTake;
                if (remainder <= 0) {
                    iter.remove();
                } else {
                    entry.setValue(remainder);
                }
                if (!takeAll) break;
            }
        }

        // 从复杂列表取出
        Iterator<ItemStack> complexIter = complexItems.iterator();
        while (complexIter.hasNext()) {
            ItemStack stack = complexIter.next();
            String stage = ItemStages.getStage(stack);
            if (stage == null || GameStageHelper.hasStage(player, stage)) {
                result.add(stack.copy());
                complexIter.remove();
                if (!takeAll) break;
            }
        }

        return result;
    }

    @Override
    public int getTotalStacks() {
        return simpleItems.size() + complexItems.size();
    }

    @Override
    public int getPickupableStacks(EntityPlayer player) {
        int count = 0;
        for (ItemKey key : simpleItems.keySet()) {
            ItemStack sample = key.toStack();
            String stage = ItemStages.getStage(sample);
            if (stage == null || GameStageHelper.hasStage(player, stage)) {
                count++;
            }
        }
        for (ItemStack stack : complexItems) {
            String stage = ItemStages.getStage(stack);
            if (stage == null || GameStageHelper.hasStage(player, stage)) {
                count++;
            }
        }
        return count;
    }

    // ========== ICapabilitySerializable 实现 ==========
    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == IPouchStorage.CAPABILITY;
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == IPouchStorage.CAPABILITY) {
            return (T) this;
        }
        return null;
    }

    // ========== INBTSerializable 实现（序列化）==========
    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();

        NBTTagList simpleList = new NBTTagList();
        for (Map.Entry<ItemKey, Integer> entry : simpleItems.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("key", entry.getKey().writeToNBT());
            tag.setInteger("count", entry.getValue());
            simpleList.appendTag(tag);
        }
        nbt.setTag("simple", simpleList);

        NBTTagList complexList = new NBTTagList();
        for (ItemStack stack : complexItems) {
            complexList.appendTag(stack.serializeNBT());
        }
        nbt.setTag("complex", complexList);

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        simpleItems.clear();
        complexItems.clear();

        NBTTagList simpleList = nbt.getTagList("simple", 10);
        for (int i = 0; i < simpleList.tagCount(); i++) {
            NBTTagCompound tag = simpleList.getCompoundTagAt(i);
            ItemKey key = ItemKey.fromNBT(tag.getCompoundTag("key"));
            int count = tag.getInteger("count");
            simpleItems.put(key, count);
        }

        NBTTagList complexList = nbt.getTagList("complex", 10);
        for (int i = 0; i < complexList.tagCount(); i++) {
            complexItems.add(new ItemStack(complexList.getCompoundTagAt(i)));
        }
    }

    // ========== Capability.IStorage 内部类（供 CapabilityManager 注册）==========
    public static class Storage implements Capability.IStorage<IPouchStorage> {
        @Nullable
        @Override
        public NBTBase writeNBT(Capability<IPouchStorage> capability, IPouchStorage instance, EnumFacing side) {
            if (instance instanceof PouchStorage) {
                return instance.serializeNBT();
            }
            return null;
        }

        @Override
        public void readNBT(Capability<IPouchStorage> capability, IPouchStorage instance, EnumFacing side, NBTBase nbt) {
            if (nbt instanceof NBTTagCompound && instance instanceof PouchStorage) {
                instance.deserializeNBT((NBTTagCompound) nbt);
            }
        }
    }
}