package xyz.tcreopargh.pouchofunknown;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.darkhax.gamestages.GameStageHelper;
import net.darkhax.gamestages.event.GameStageEvent;
import net.darkhax.itemstages.ItemStages;
import net.minecraft.command.ICommandSender;
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

import java.util.Objects;

@Mod.EventBusSubscriber(modid = PouchOfUnknownMod.MODID)
public final class PouchOfUnknownEvents {

    public static void detect(EntityPlayer player) {
        detect(player, -1);
    }

    public static void detect(EntityPlayer player, int index) {

        ItemStack pouch = findPouch(player);
        String pouchStage = getItemStage(pouch);
        int indexBegin = 0;
        int indexEnd = player.inventory.getSizeInventory() - 1;


        if (index >= 0 && index <= indexEnd) {
            indexBegin = index;
            indexEnd = index;
        }

        for (int slot = indexBegin; slot <= indexEnd; slot++) {
            ItemStack stack = player.inventory.getStackInSlot(slot).copy();
            ItemStack remnant = stack;
            String stage = getItemStage(stack);
            if (!isQualified(player, stage, true)) {
                if (!pouch.isEmpty() && isQualified(player, pouchStage, true)) {
                    if (isDisabledStage(stage)) {
                        if (PouchConfig.showMessage) {
                            player.sendMessage(new TextComponentTranslation(
                                    "pouchofunknown.disabled_item_message")
                                    .setStyle(new Style().setColor(TextFormatting.RED)));
                        }
                    } else {
                        remnant = Util.insertItem(pouch, remnant);
                        String displayString = getDisplayName(stack, player);
                        if (!remnant.isEmpty()) {
                            if (!PouchConfig.destroyItemWithoutPouch) {
                                player.dropItem(remnant, true);
                                player.sendMessage(new TextComponentTranslation(
                                        "pouchofunknown.full_message", displayString, "\n")
                                        .setStyle(new Style().setColor(TextFormatting.YELLOW)));
                            } else {
                                player.sendMessage(new TextComponentTranslation(
                                        "pouchofunknown.full_destroy_message", displayString, "\n")
                                        .setStyle(new Style().setColor(TextFormatting.YELLOW)));
                            }

                        } else {
                            if (PouchConfig.showMessage) {
                                player.sendMessage(new TextComponentTranslation(
                                        "pouchofunknown.pickup_message", displayString)
                                        .setStyle(new Style().setColor(TextFormatting.YELLOW)));
                            }
                        }
                    }
                } else {
                    if (!PouchConfig.destroyItemWithoutPouch) {
                        player.dropItem(stack, true);
                    }
                    if (PouchConfig.showMessage && !stack.isEmpty()) {
                        String displayString = getDisplayName(stack, player);
                        if (!stack.isEmpty()) {
                            if (!PouchConfig.destroyItemWithoutPouch) {
                                player.sendMessage(new TextComponentTranslation(
                                        "pouchofunknown.drop_message", displayString, "\n")
                                        .setStyle(new Style().setColor(TextFormatting.YELLOW)));
                            } else {
                                player.sendMessage(new TextComponentTranslation(
                                        "pouchofunknown.destroy_message", displayString, "\n")
                                        .setStyle(new Style().setColor(TextFormatting.YELLOW)));
                            }
                        }
                    }
                }
                player.inventory.setInventorySlotContents(slot, ItemStack.EMPTY);
                if (player instanceof EntityPlayerMP) {
                    EntityPlayerMP playerMP = (EntityPlayerMP) player;
                    playerMP.sendContainerToPlayer(player.inventoryContainer);
                }
                player.inventoryContainer.detectAndSendChanges();
            }
        }
    }

    @SubscribeEvent
    public static void onEntityItemPickup(EntityItemPickupEvent event) {
        if (!PouchConfig.enablePickupDetection) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        ItemStack pouch = findPouch(player);
        String pouchStage = getItemStage(pouch);
        ItemStack stack = event.getItem().getItem();
        ItemStack remnant = stack;
        String stage = getItemStage(stack);
        if (!isQualified(player, stage, true)) {
            event.setCanceled(true);
            if (!pouch.isEmpty() && isQualified(player, pouchStage, true)) {
                if (isDisabledStage(stage)) {
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
                        event.getItem().setItem(remnant);
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
        return !pouch.isEmpty() && Objects.equals(pouch.getItem().getRegistryName(), ItemPouchOfUnknown.itemPouchOfUnknown.getRegistryName());
    }

    public static ItemStack findPouch(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (isValidPouch(stack)) {
                return stack;
            }
        }
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isValidPouch(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static String getItemStage(ItemStack stack) {
        return stack.isEmpty() ? null : ItemStages.getStage(stack);
    }

    public static boolean isDisabledStage(String stage) {
        if (stage == null) {
            return false;
        }
        for (String disabledStage : PouchConfig.disabledStagesList) {
            if (stage.equals(disabledStage)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isQualified(EntityPlayer player, ItemStack stack, boolean ignoreCreative) {
        return isQualified(player, getItemStage(stack), ignoreCreative);
    }

    public static boolean isQualified(EntityPlayer player, String stage, boolean ignoreCreative) {
        if (stage == null) {
            return true;
        }
        if (player.isCreative() && ignoreCreative) {
            return true;
        }
        return GameStageHelper.hasStage(player, stage);
    }

    public static String getDisplayName(ItemStack stack, ICommandSender sender) {
        String unfamiliarName;
        if (PouchConfig.showItemName) {
            unfamiliarName = stack.getDisplayName();
        } else {
            unfamiliarName = ItemStages.CUSTOM_NAMES.containsKey(stack)
                    ? ItemStages.CUSTOM_NAMES.get(stack)
                    : new TextComponentTranslation("pouchofunknown.unfamiliar.default.name").getFormattedText();
        }
        return TextFormatting.GOLD + unfamiliarName + " " + TextFormatting.YELLOW + "*" + " " + TextFormatting.AQUA + stack.getCount() + TextFormatting.YELLOW;
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
        if (eventArgs.getModID().equals(PouchOfUnknownMod.MODID)) {
            PouchOfUnknownMod.logger.info("Pouch Of Unknown config changed!");
            ConfigManager.sync(PouchOfUnknownMod.MODID, Config.Type.INSTANCE);
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
            PlayerInventoryListener.addTo(event.player.inventoryContainer, (EntityPlayerMP) event.player);
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP) {
            PlayerInventoryListener.addTo(event.getEntityPlayer().inventoryContainer, (EntityPlayerMP) event.getEntityPlayer());
        }
    }

    @SubscribeEvent
    public static void onGameStageRemoved(GameStageEvent.Removed event) {
        detect(event.getEntityPlayer());
    }
}

