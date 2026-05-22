package xyz.tcreopargh.pouchofunknown.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 客户端内存缓存，保存当前玩家的袋子统计数字。
 * 由于单人游戏或专用服务器上客户端只有一个玩家，使用静态字段即可。
 */
@SideOnly(Side.CLIENT)
public class PouchClientCache {
    private static int total = 0;
    private static int pickupable = 0;

    public static void setStats(int total, int pickupable) {
        PouchClientCache.total = total;
        PouchClientCache.pickupable = pickupable;
    }

    public static int getTotal() {
        return total;
    }

    public static int getPickupable() {
        return pickupable;
    }
}