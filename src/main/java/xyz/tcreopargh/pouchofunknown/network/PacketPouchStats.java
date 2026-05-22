package xyz.tcreopargh.pouchofunknown.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import xyz.tcreopargh.pouchofunknown.client.PouchClientCache;

/**
 * 服务端→客户端：推送当前玩家的袋子统计数字。
 */
public class PacketPouchStats implements IMessage {
    private int totalStacks;
    private int pickupableStacks;

    @SuppressWarnings("unused")
    public PacketPouchStats() {}

    public PacketPouchStats(int total, int pickupable) {
        this.totalStacks = total;
        this.pickupableStacks = pickupable;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        totalStacks = buf.readInt();
        pickupableStacks = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(totalStacks);
        buf.writeInt(pickupableStacks);
    }

    public static class Handler implements IMessageHandler<PacketPouchStats, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketPouchStats message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                PouchClientCache.setStats(message.totalStacks, message.pickupableStacks);
            });
            return null;
        }
    }
}