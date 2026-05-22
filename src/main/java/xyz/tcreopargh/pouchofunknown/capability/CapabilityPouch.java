package xyz.tcreopargh.pouchofunknown.capability;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xyz.tcreopargh.pouchofunknown.Tags;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class CapabilityPouch {
    public static final ResourceLocation POUCH_CAP = new ResourceLocation(Tags.MOD_ID, "pouch");

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            // 直接添加实现了 ICapabilitySerializable 的 PouchStorage 实例
            event.addCapability(POUCH_CAP, new PouchStorage());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        EntityPlayer oldPlayer = event.getOriginal();
        EntityPlayer newPlayer = event.getEntityPlayer();
        IPouchStorage oldData = oldPlayer.getCapability(IPouchStorage.CAPABILITY, null);
        IPouchStorage newData = newPlayer.getCapability(IPouchStorage.CAPABILITY, null);
        if (oldData instanceof PouchStorage && newData instanceof PouchStorage) {
            newData.deserializeNBT(oldData.serializeNBT());
        }
    }
}