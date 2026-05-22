package xyz.tcreopargh.pouchofunknown;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.darkhax.gamestages.GameStageHelper;
import net.darkhax.gamestages.event.GameStageEvent;
import net.darkhax.itemstages.ItemStages;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import xyz.tcreopargh.pouchofunknown.capability.IPouchStorage;
import xyz.tcreopargh.pouchofunknown.network.PacketPouchStats;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PouchOfUnknownEvents {

    /**
     * 将玩家袋子的统计信息通过网络包发送给客户端。
     */
    public static void syncStatsToPlayer(EntityPlayerMP player) {
        IPouchStorage storage = player.getCapability(IPouchStorage.CAPABILITY, null);
        if (storage == null) return;
        int total = storage.getTotalStacks();
        int pickupable = storage.getPickupableStacks(player);
        PouchOfUnknownMod.NETWORK.sendTo(new PacketPouchStats(total, pickupable), player);
    }

    public static void detect(EntityPlayer player) {
        detect(player, -1);
    }

    public static void detect(EntityPlayer player, int slotIndex) {
        IPouchStorage storage = player.getCapability(IPouchStorage.CAPABILITY, null);
        if (storage == null) return;

        boolean changed = false;
        ItemStack pouch = findPouch(player);
        String pouchStage = pouch.isEmpty() ? null : getItemStage(pouch);

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            if (slotIndex >= 0 && i != slotIndex) continue;
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            String stage = getItemStage(stack);
            if (isQualified(player, stage, true)) continue;

            // 玩家无权持有此物品
            if (!pouch.isEmpty() && isQualified(player, pouchStage, true) && !isDisabledStage(stage)) {
                // 有可用袋子，尝试存入
                ItemStack remnant = storage.deposit(stage, stack.copy());
                player.inventory.setInventorySlotContents(i, remnant.isEmpty() ? ItemStack.EMPTY : remnant);
                if (PouchConfig.showMessage && remnant.getCount() < stack.getCount()) {
                    String display = getDisplayName(stack, stack.getCount() - remnant.getCount());
                    if (remnant.isEmpty()) {
                        player.sendMessage(new TextComponentTranslation("pouchofunknown.pickup_message", display)
                                .setStyle(new Style().setColor(TextFormatting.YELLOW)));
                    } else {
                        player.sendMessage(new TextComponentTranslation("pouchofunknown.full_message", display)
                                .setStyle(new Style().setColor(TextFormatting.YELLOW)));
                    }
                }
                changed = true;
            } else {
                // 无袋子，或袋子不可用，或禁用阶段，丢弃/销毁
                player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
                if (!player.world.isRemote) {
                    if (!PouchConfig.destroyItemWithoutPouch) {
                        player.dropItem(stack, true);
                    }
                    if (PouchConfig.showMessage) {
                        player.sendMessage(new TextComponentTranslation(
                                PouchConfig.destroyItemWithoutPouch ? "pouchofunknown.destroy_message" : "pouchofunknown.drop_message",
                                getDisplayName(stack, stack.getCount()))
                                .setStyle(new Style().setColor(TextFormatting.YELLOW)));
                    }
                }
                changed = true;
            }
        }

        if (changed) {
            if (player instanceof EntityPlayerMP) {
                syncStatsToPlayer((EntityPlayerMP) player);
                ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
            }
            player.inventoryContainer.detectAndSendChanges();
        }
    }

    @SubscribeEvent
    public static void onEntityItemPickup(EntityItemPickupEvent event) {
        if (!PouchConfig.enablePickupDetection) return;
        EntityPlayer player = event.getEntityPlayer();
        IPouchStorage storage = player.getCapability(IPouchStorage.CAPABILITY, null);
        if (storage == null) return;

        ItemStack stack = event.getItem().getItem();
        String stage = getItemStage(stack);
        if (isQualified(player, stage, true)) return; // 玩家有资格，正常拾取

        // 无资格，拦截
        event.setCanceled(true);
        ItemStack pouch = findPouch(player);
        String pouchStage = pouch.isEmpty() ? null : getItemStage(pouch);

        if (!pouch.isEmpty() && isQualified(player, pouchStage, true)) {
            if (isDisabledStage(stage)) {
                event.getItem().setDead();
                if (PouchConfig.showMessage) {
                    player.sendMessage(new TextComponentTranslation("pouchofunknown.disabled_item_message")
                            .setStyle(new Style().setColor(TextFormatting.RED)));
                }
            } else {
                ItemStack remnant = storage.deposit(stage, stack);
                if (remnant.isEmpty()) {
                    event.getItem().setDead();
                    if (PouchConfig.showMessage) {
                        player.sendMessage(new TextComponentTranslation("pouchofunknown.pickup_message", getDisplayName(stack, stack.getCount()))
                                .setStyle(new Style().setColor(TextFormatting.YELLOW)));
                    }
                } else {
                    event.getItem().setItem(remnant);
                    player.sendStatusMessage(new TextComponentTranslation("pouchofunknown.cant_pickup_pouch_full_message")
                            .setStyle(new Style().setColor(TextFormatting.YELLOW)), true);
                }
            }
        } else {
            if (PouchConfig.showMessage) {
                player.sendStatusMessage(new TextComponentTranslation("pouchofunknown.cant_pickup_message")
                        .setStyle(new Style().setColor(TextFormatting.YELLOW)), true);
            }
        }

        if (player instanceof EntityPlayerMP) {
            syncStatsToPlayer((EntityPlayerMP) player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.player instanceof EntityPlayerMP && !event.crafting.isEmpty()) {
            detect(event.player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.player instanceof EntityPlayerMP && !event.smelting.isEmpty()) {
            detect(event.player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onOpenContainer(net.minecraftforge.event.entity.player.PlayerContainerEvent event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP) {
            PlayerInventoryListener.addTo(event.getContainer(), (EntityPlayerMP) event.getEntityPlayer());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            PlayerInventoryListener.addTo(player.inventoryContainer, player);
            syncStatsToPlayer(player); // 登录时同步一次统计数据
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
            PlayerInventoryListener.addTo(player.inventoryContainer, player);
            syncStatsToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onGameStageAdded(GameStageEvent.Added event) {
        // 解锁阶段可能影响 pickupable 统计
        if (event.getEntityPlayer() instanceof EntityPlayerMP) {
            syncStatsToPlayer((EntityPlayerMP) event.getEntityPlayer());
        }
    }

    @SubscribeEvent
    public static void onGameStageRemoved(GameStageEvent.Removed event) {
        // 移除阶段可能使背包内物品变为无资格，触发检测
        detect(event.getEntityPlayer());
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
        if (eventArgs.getModID().equals(Tags.MOD_ID)) {
            PouchOfUnknownMod.LOGGER.info("Pouch Of Unknown config changed!");
            ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
        }
    }

    // ---------- 辅助方法 ----------

    public static boolean isValidPouch(ItemStack pouch) {
        return !pouch.isEmpty() && pouch.getItem() instanceof ItemPouchOfUnknown;
    }

    public static ItemStack findPouch(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (isValidPouch(stack)) return stack;
        }
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isValidPouch(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static String getItemStage(ItemStack stack) {
        return stack.isEmpty() ? null : ItemStages.getStage(stack);
    }

    public static boolean isDisabledStage(String stage) {
        if (stage == null) return false;
        for (String s : PouchConfig.disabledStagesList) {
            if (s.equals(stage)) return true;
        }
        return false;
    }

    public static boolean isQualified(EntityPlayer player, String stage, boolean ignoreCreative) {
        if (stage == null) return true;
        if (player.isCreative() && ignoreCreative) return true;
        return GameStageHelper.hasStage(player, stage);
    }

    public static String getDisplayName(ItemStack stack, int count) {
        String name;
        if (PouchConfig.showItemName) {
            name = stack.getDisplayName();
        } else {
            name = ItemStages.CUSTOM_NAMES.containsKey(stack)
                    ? ItemStages.CUSTOM_NAMES.get(stack)
                    : new TextComponentTranslation("pouchofunknown.unfamiliar.default.name").getFormattedText();
        }
        return TextFormatting.GOLD + name + TextFormatting.YELLOW + " * " + TextFormatting.AQUA + count + TextFormatting.YELLOW;
    }
}