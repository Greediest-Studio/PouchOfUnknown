package xyz.tcreopargh.pouchofunknown;

import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.tcreopargh.pouchofunknown.capability.IPouchStorage;
import xyz.tcreopargh.pouchofunknown.capability.PouchStorage;
import xyz.tcreopargh.pouchofunknown.network.PacketPouchStats;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION,
        dependencies = "required-after:itemstages;required-after:baubles")
public class PouchOfUnknownMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_ID);
    public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID + ":stats");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        CapabilityManager.INSTANCE.register(IPouchStorage.class, new PouchStorage.Storage(), PouchStorage::new);
        NETWORK.registerMessage(PacketPouchStats.Handler.class, PacketPouchStats.class, 0, Side.CLIENT);
    }
}