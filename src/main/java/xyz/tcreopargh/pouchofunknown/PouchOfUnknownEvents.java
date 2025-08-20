package xyz.tcreopargh.pouchofunknown;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.darkhax.gamestages.GameStageHelper;
import net.darkhax.gamestages.event.GameStageEvent;
import net.darkhax.itemstages.ItemStages;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static xyz.tcreopargh.pouchofunknown.Util.findValidPouch;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PouchOfUnknownEvents {

    public static final int MAX_SLOT_NUMBER = 40;

    public static final AtomicReference<Method> method = new AtomicReference<>();

    static {
        try {
            method.set(ItemStages.class.getDeclaredMethod("getUnfamiliarName", ItemStack.class));
            method.get().setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }


    public static void detect(EntityPlayer player) {
        detect(player, -1);
    }

    public static void detect(EntityPlayer player, int index) {
        ItemStack stack = player.inventory.getStackInSlot(index).copy();
        if (stack.isEmpty() || isQualified(player, stack, true)) return;

        ItemStack pouch = findValidPouch(player);
        boolean hasValidPouch = !pouch.isEmpty() && isQualified(player, pouch, true);

        if (hasValidPouch) {
            handleWithPouch(player, stack, pouch);
        } else {
            handleWithoutPouch(player, stack);
        }

        player.inventory.setInventorySlotContents(index, ItemStack.EMPTY);
        syncInventory(player);
    }

    private static void handleWithPouch(EntityPlayer player, ItemStack stack, ItemStack pouch) {
        if (isDisabledStage(stack)) {
            if (PouchConfig.showMessage) {
                sendMessage(player, "pouchofunknown.disabled_item_message", TextFormatting.RED);
            }
            return;
        }

        ItemStack remnant = Util.insertItem(pouch, stack);
        String displayName = getDisplayName(stack, player);

        if (remnant.isEmpty()) {
            if (PouchConfig.showMessage) {
                sendMessage(player, "pouchofunknown.pickup_message", TextFormatting.YELLOW, displayName);
            }
        } else {
            boolean destroy = PouchConfig.destroyItemWithoutPouch;
            if (!destroy) player.dropItem(remnant, true);

            String messageKey = destroy ? "pouchofunknown.full_destroy_message" : "pouchofunknown.full_message";
            sendMessage(player, messageKey, TextFormatting.YELLOW, displayName, "\n");
        }
    }

    private static void handleWithoutPouch(EntityPlayer player, ItemStack stack) {
        boolean destroy = PouchConfig.destroyItemWithoutPouch;
        if (!destroy) player.dropItem(stack, true);

        if (PouchConfig.showMessage && !stack.isEmpty()) {
            String displayName = getDisplayName(stack, player);
            String messageKey = destroy ? "pouchofunknown.destroy_message" : "pouchofunknown.drop_message";
            sendMessage(player, messageKey, TextFormatting.YELLOW, displayName, "\n");
        }
    }

    private static boolean isDisabledStage(ItemStack stack) {
        String stage = ItemStages.getStage(stack);
        return stage != null && Arrays.asList(PouchConfig.disabledStagesList).contains(stage);
    }

    private static void sendMessage(EntityPlayer player, String key, TextFormatting color, Object... args) {
        player.sendMessage(new TextComponentTranslation(key, args).setStyle(new Style().setColor(color)));
    }

    private static void syncInventory(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        }
        player.inventoryContainer.detectAndSendChanges();
    }

    @SubscribeEvent
    public static void onEntityItemPickup(EntityItemPickupEvent event) {
        if (!PouchConfig.enablePickupDetection) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        boolean hasPouch = false;
        ItemStack pouch = ItemStack.EMPTY;
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (isValidPouch(stack)) {
                hasPouch = true;
                pouch = stack;
                break;
            }
        }
        if (!hasPouch) {
            for (int i = 0; i < MAX_SLOT_NUMBER; i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (isValidPouch(stack)) {
                    hasPouch = true;
                    pouch = stack;
                    break;
                }
            }
        }

        ItemStack stack = event.getItem().getItem();
        ItemStack remnant = stack;
        if (!isQualified(player, stack, true)) {
            event.setCanceled(true);
            if (hasPouch && isQualified(player, pouch, true)) {
                if (Arrays.asList(PouchConfig.disabledStagesList).contains(ItemStages.getStage(stack))) {
                    event.getItem().world.removeEntity(event.getItem());
                    if (PouchConfig.showMessage) {
                        player.sendMessage(new TextComponentTranslation(
                                "pouchofunknown.disabled_item_message")
                                .setStyle(new Style().setColor(TextFormatting.RED)));
                    }
                } else {
                    remnant = Util.insertItem(pouch, remnant);
                    String displayString = getDisplayName(stack, player);
                    if (!remnant.isEmpty()) {
                        player.sendStatusMessage(new TextComponentTranslation(
                                "pouchofunknown.cant_pickup_pouch_full_message")
                                .setStyle(new Style().setColor(TextFormatting.YELLOW)), true);
                    } else {
                        event.getItem().world.removeEntity(event.getItem());
                        if (PouchConfig.showMessage) {
                            player.sendMessage(new TextComponentTranslation(
                                    "pouchofunknown.pickup_message", displayString)
                                    .setStyle(new Style().setColor(TextFormatting.YELLOW)));
                        }
                    }
                }
            } else {
                if (PouchConfig.showMessage && !stack.isEmpty()) {
                    player.sendStatusMessage(new TextComponentTranslation(
                            "pouchofunknown.cant_pickup_message")
                            .setStyle(new Style().setColor(TextFormatting.YELLOW)), true);
                }
            }
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

    public static boolean isValidPouch(ItemStack pouch) {
        return Objects.equals(Objects.requireNonNull(pouch.getItem().getRegistryName()).toString(), ItemPouchOfUnknown.registryName);
    }

    public static boolean isQualified(EntityPlayer player, ItemStack stack, boolean ignoreCreative) {
        if (ItemStages.getStage(stack) == null || stack.isEmpty()) {
            return true;
        }
        if (player.isCreative() && ignoreCreative) {
            return true;
        }
        if (PouchConfig.ignoreNBT) {
            ItemStack baseStack = new ItemStack(new Item().setRegistryName(Objects.requireNonNull(stack.getItem().getRegistryName())), stack.getCount(), stack.getItemDamage());
            return GameStageHelper.hasStage(player, ItemStages.getStage(baseStack));
        } else {
            return GameStageHelper.hasStage(player, ItemStages.getStage(stack));
        }
    }

    public static String getDisplayName(ItemStack stack, ICommandSender sender) {
        String unfamiliarName;
        if (PouchConfig.showItemName) {
            unfamiliarName = stack.getDisplayName();
        } else {
            try {
                unfamiliarName = (String) method.get().invoke(null, stack);
            } catch (Exception e) {
                unfamiliarName = new TextComponentTranslation("pouchofunknown.unfamiliar.default.name").getFormattedText();
            }
        }
        return TextFormatting.GOLD + unfamiliarName + " " + TextFormatting.YELLOW + "*" + " " + TextFormatting.AQUA + stack.getCount() + TextFormatting.YELLOW;
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
        if (eventArgs.getModID().equals(Tags.MOD_ID)) {
            PouchOfUnknownMod.logger.info("Pouch Of Unknown config changed!");
            ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onOpenContainer(net.minecraftforge.event.entity.player.PlayerContainerEvent event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP) {
            event.getContainer().addListener(new PlayerInventoryListener((EntityPlayerMP) event.getEntityPlayer()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            event.player.inventoryContainer.addListener(new PlayerInventoryListener((EntityPlayerMP) event.player));
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP) {
            event.getEntityPlayer().inventoryContainer.addListener(new PlayerInventoryListener((EntityPlayerMP) event.getEntityPlayer()));
        }
    }

    @SubscribeEvent
    public static void onGameStageRemoved(GameStageEvent.Removed event) {
        detect(event.getEntityPlayer());
    }
}

