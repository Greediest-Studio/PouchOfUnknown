package xyz.tcreopargh.pouchofunknown;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;
import xyz.tcreopargh.pouchofunknown.capability.IPouchStorage;
import xyz.tcreopargh.pouchofunknown.client.PouchClientCache;

import javax.annotation.Nullable;
import java.util.List;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ItemPouchOfUnknown extends Item implements IBauble {

    public static final ItemPouchOfUnknown itemPouchOfUnknown = new ItemPouchOfUnknown();
    public static final String registryName = Tags.MOD_ID + ":pouch";

    public ItemPouchOfUnknown() {
        this.setTranslationKey("pouchofunknown.pouch");
        this.setRegistryName(registryName);
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabs.MISC);
    }

    @SubscribeEvent
    public static void registerItem(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(itemPouchOfUnknown);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onModelReg(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(itemPouchOfUnknown, 0,
                new ModelResourceLocation(itemPouchOfUnknown.getRegistryName(), "inventory"));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        // 完全不访问 stack 的 NBT，直接从客户端缓存读取
        int total = PouchClientCache.getTotal();
        int pickupable = PouchClientCache.getPickupable();
        int max = PouchConfig.pouchCapacity;

        tooltip.add(TextFormatting.GREEN + I18n.format("pouchofunknown.tooltip.pickupable", pickupable));
        tooltip.add(TextFormatting.YELLOW + I18n.format("pouchofunknown.tooltip.total", total));
        tooltip.add(TextFormatting.GRAY + I18n.format("pouchofunknown.tooltip.maxcapacity", max));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            IPouchStorage storage = player.getCapability(IPouchStorage.CAPABILITY, null);
            if (storage == null) {
                return new ActionResult<>(EnumActionResult.FAIL, player.getHeldItem(hand));
            }

            boolean takeAll = player.isSneaking();
            List<ItemStack> items = storage.withdraw(player, takeAll);
            int count = 0;
            for (ItemStack is : items) {
                ItemHandlerHelper.giveItemToPlayer(player, is);
                count++;
            }

            if (count == 0) {
                player.sendMessage(new TextComponentTranslation("pouchofunknown.open_message_empty")
                        .setStyle(new Style().setColor(TextFormatting.RED)));
            } else {
                player.sendMessage(new TextComponentTranslation("pouchofunknown.open_message", count)
                        .setStyle(new Style().setColor(TextFormatting.GREEN)));
            }

            // 同步统计到客户端
            PouchOfUnknownEvents.syncStatsToPlayer(playerMP);
            playerMP.sendContainerToPlayer(player.inventoryContainer);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public EnumRarity getForgeRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.TRINKET;
    }
}