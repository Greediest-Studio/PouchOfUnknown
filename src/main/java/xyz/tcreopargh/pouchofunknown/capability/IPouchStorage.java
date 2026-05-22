package xyz.tcreopargh.pouchofunknown.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

import javax.annotation.Nullable;
import java.util.List;

public interface IPouchStorage {
    @CapabilityInject(IPouchStorage.class)
    Capability<IPouchStorage> CAPABILITY = null;

    /**
     * 存入物品，自动按 stage 归类。
     * @param stage 物品的阶段（可为 null）
     * @param stack 待存入的物品
     * @return 无法存入的剩余部分（可能为空）
     */
    ItemStack deposit(@Nullable String stage, ItemStack stack);

    /**
     * 取出可解锁阶段的物品。
     * @param player 玩家
     * @param takeAll 是否取出所有符合条件的物品
     * @return 取出的 ItemStack 列表（每个已是独立副本）
     */
    List<ItemStack> withdraw(EntityPlayer player, boolean takeAll);

    /**
     * 获取袋内总堆叠种类数（唯一 ItemKey 数 + 有 NBT 物品堆叠数）。
     */
    int getTotalStacks();

    /**
     * 获取玩家当前可解锁的堆叠种类数。
     */
    int getPickupableStacks(EntityPlayer player);

    NBTTagCompound serializeNBT();
    void deserializeNBT(NBTTagCompound nbt);
}