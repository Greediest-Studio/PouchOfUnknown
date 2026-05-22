package xyz.tcreopargh.pouchofunknown;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;

public final class ItemKey {
    public final ResourceLocation registryName;
    public final int meta;
    public final int tagHash;
    @Nullable
    public final NBTTagCompound fullTag;

    private ItemKey(ResourceLocation registryName, int meta, int tagHash, @Nullable NBTTagCompound fullTag) {
        this.registryName = registryName;
        this.meta = meta;
        this.tagHash = tagHash;
        this.fullTag = fullTag != null ? fullTag.copy() : null;
    }

    public static ItemKey from(ItemStack stack) {
        ResourceLocation name = stack.getItem().getRegistryName();
        int meta = stack.getMetadata();
        NBTTagCompound tag = stack.getTagCompound();
        int hash = 0;
        NBTTagCompound cache = null;
        if (tag != null && !tag.isEmpty()) {
            cache = tag.copy();
            hash = tag.hashCode();
        }
        return new ItemKey(name, meta, hash, cache);
    }

    public ItemStack toStack() {
        Item item = Item.getByNameOrId(registryName.toString());
        if (item == null) return ItemStack.EMPTY; // 物品未注册时的安全兜底
        ItemStack stack = new ItemStack(item, 1, meta);
        if (fullTag != null && !fullTag.isEmpty()) {
            stack.setTagCompound(fullTag.copy());
        }
        return stack;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("id", registryName.toString());
        nbt.setInteger("meta", meta);
        nbt.setInteger("hash", tagHash);
        if (fullTag != null) {
            nbt.setTag("tag", fullTag.copy());
        }
        return nbt;
    }

    public static ItemKey fromNBT(NBTTagCompound nbt) {
        ResourceLocation id = new ResourceLocation(nbt.getString("id"));
        int meta = nbt.getInteger("meta");
        int hash = nbt.getInteger("hash");
        NBTTagCompound tag = nbt.hasKey("tag") ? nbt.getCompoundTag("tag") : null;
        return new ItemKey(id, meta, hash, tag);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemKey)) return false;
        ItemKey that = (ItemKey) o;
        if (meta != that.meta) return false;
        if (!registryName.equals(that.registryName)) return false;
        if (tagHash != that.tagHash) return false;
        if (fullTag == null) return that.fullTag == null;
        return that.fullTag != null && fullTag.equals(that.fullTag);
    }

    @Override
    public int hashCode() {
        int result = registryName.hashCode();
        result = 31 * result + meta;
        result = 31 * result + tagHash;
        return result;
    }
}