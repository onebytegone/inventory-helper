package com.onebytegone.inventoryhelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent.InitScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Mod("inventoryhelper")
@OnlyIn(Dist.CLIENT)
public class InventoryHelper {

   private static final int SLOTS_PER_ROW = 9;
   private static final int PLAYER_INVENTORY_SIZE = 3 * SLOTS_PER_ROW;
   private static final int PLAYER_HOTBAR_SIZE = SLOTS_PER_ROW;

   // Directly reference a log4j logger.
   private static final Logger LOGGER = LogManager.getLogger();

   // TODO: Make this list configurable
   private List<String> ignoredItems = Arrays.asList(
      // Consumable tools
      "minecraft:arrow",
      "minecraft:spectral_arrow",
      "minecraft:tipped_arrow",
      "minecraft:firework_rocket",
      "minecraft:torch",

      // Tools
      "minecraft:bow",
      "minecraft:crossbow",
      "minecraft:diamond_axe",
      "minecraft:diamond_boots",
      "minecraft:diamond_chestplate",
      "minecraft:diamond_pickaxe",
      "minecraft:diamond_shovel",
      "minecraft:diamond_sword",
      "minecraft:elytra",
      "minecraft:netherite_axe",
      "minecraft:netherite_boots",
      "minecraft:netherite_chestplate",
      "minecraft:netherite_pickaxe",
      "minecraft:netherite_shovel",
      "minecraft:netherite_sword",

      // Shulker boxes
      "minecraft:shulker_box",
      "minecraft:white_shulker_box",
      "minecraft:orange_shulker_box",
      "minecraft:magenta_shulker_box",
      "minecraft:light_blue_shulker_box",
      "minecraft:yellow_shulker_box",
      "minecraft:lime_shulker_box",
      "minecraft:pink_shulker_box",
      "minecraft:gray_shulker_box",
      "minecraft:light_gray_shulker_box",
      "minecraft:cyan_shulker_box",
      "minecraft:purple_shulker_box",
      "minecraft:blue_shulker_box",
      "minecraft:brown_shulker_box",
      "minecraft:green_shulker_box",
      "minecraft:red_shulker_box",
      "minecraft:black_shulker_box"
   );

   public InventoryHelper() {
      MinecraftForge.EVENT_BUS.register(this);
   }

   @SubscribeEvent
   @OnlyIn(Dist.CLIENT)
   public void onScreenInit(InitScreenEvent.Post e) {
      Screen screen = e.getScreen();

      if (screen != null) {
         LOGGER.info("Screen init event " + screen.getClass().getName());

         if (screen instanceof ContainerScreen || screen instanceof ShulkerBoxScreen) {
            int leftSideX = 0;
            int rightSideX = 0;
            int containerY = 0;
            int inventoryY = 0;

            if (screen instanceof ContainerScreen) {
               ContainerScreen containerScreen = (ContainerScreen) screen;
               ChestMenu menu = containerScreen.getMenu();
               Slot firstPlayerSlot = menu.getSlot(menu.getRowCount() * SLOTS_PER_ROW);
               leftSideX = containerScreen.getGuiLeft() - 52;
               rightSideX = containerScreen.getGuiLeft() + containerScreen.getXSize() + 2;
               containerY = containerScreen.getGuiTop() + menu.getSlot(0).y - 2;
               inventoryY = containerScreen.getGuiTop() + firstPlayerSlot.y - 2;
            } else if (screen instanceof ShulkerBoxScreen) {
               ShulkerBoxScreen shulkerBoxScreen = (ShulkerBoxScreen) screen;
               ShulkerBoxMenu menu = shulkerBoxScreen.getMenu();
               Slot firstPlayerSlot = menu.getSlot(3 * SLOTS_PER_ROW);
               leftSideX = shulkerBoxScreen.getGuiLeft() - 52;
               rightSideX = shulkerBoxScreen.getGuiLeft() + shulkerBoxScreen.getXSize() + 2;
               containerY = shulkerBoxScreen.getGuiTop() + menu.getSlot(0).y - 2;
               inventoryY = shulkerBoxScreen.getGuiTop() + firstPlayerSlot.y - 2;
            }

            Button containerCombineButton = makeCombineButton(leftSideX, containerY, false);
            Button inventoryCombineButton = makeCombineButton(leftSideX, inventoryY, true);
            e.addListener(containerCombineButton);

            Button containerUnloadButton = makeUnloadContainerButton(rightSideX, containerY);
            Button restockButton = makeRestockButton(rightSideX, inventoryY);
            Button fillButton = makeFillButton(rightSideX, restockButton.y + restockButton.getHeight() + 2);
            Button unloadButton = makeUnloadButton(rightSideX, fillButton.y + fillButton.getHeight() + 2);

            e.addListener(containerCombineButton);
            e.addListener(inventoryCombineButton);
            e.addListener(containerUnloadButton);
            e.addListener(restockButton);
            e.addListener(fillButton);
            e.addListener(unloadButton);
         } else if (screen instanceof InventoryScreen) {
            InventoryScreen inventoryScreen = (InventoryScreen) screen;
            Button inventoryCombineButton = makeCombineButton(
               inventoryScreen.getGuiLeft() + inventoryScreen.getXSize() / 2 - 25,
               inventoryScreen.getGuiTop() + inventoryScreen.getYSize() + 2,
               true
            );

            e.addListener(inventoryCombineButton);
         }
      }
   }

   private Button makeUnloadButton(int x, int y) {
      // "Unload": Move all inventory except for those on the exclude list
      return new Button(x, y, 50, 20, new TextComponent("Unload"), (button) -> {
         Minecraft minecraft = Minecraft.getInstance();
         Player player = minecraft.player;
         Inventory playerInventory = player.getInventory();
         AbstractContainerMenu containerMenu = player.containerMenu;

         List<Slot> allPlayerSlots = containerMenu.slots.stream()
            .filter(slot -> (slot.container == playerInventory))
            .collect(Collectors.toList());

         List<Slot> inventorySlots = allPlayerSlots.subList(0, PLAYER_INVENTORY_SIZE);

         this.logSlots("Inv.", inventorySlots);

         inventorySlots.stream().forEach(sourceSlot -> {
            ItemStack itemStack = sourceSlot.getItem();
            ResourceLocation resourceLocation = ForgeRegistries.ITEMS.getKey(itemStack.getItem());

            if (!itemStack.isEmpty() && !this.ignoredItems.contains(resourceLocation.toString())) {
               LOGGER.info("Unloading inventory: " + resourceLocation);
               minecraft.gameMode.handleInventoryMouseClick(containerMenu.containerId, sourceSlot.index, 0, ClickType.QUICK_MOVE, player);
            }
         });

         if (Screen.hasShiftDown()) {
            List<Slot> hotbarSlots = allPlayerSlots.subList(PLAYER_INVENTORY_SIZE, PLAYER_INVENTORY_SIZE + PLAYER_HOTBAR_SIZE);

            hotbarSlots.stream().forEach(sourceSlot -> {
               ItemStack itemStack = sourceSlot.getItem();
               ResourceLocation resourceLocation = ForgeRegistries.ITEMS.getKey(itemStack.getItem());

               if (!itemStack.isEmpty() && !this.ignoredItems.contains(resourceLocation.toString())) {
                  LOGGER.info("Unloading hotbar: " + resourceLocation);
                  minecraft.gameMode.handleInventoryMouseClick(containerMenu.containerId, sourceSlot.index, 0, ClickType.QUICK_MOVE, player);
               }
            });
         }
      });
   }

   private Button makeUnloadContainerButton(int x, int y) {
      // "Unload": Move all from container
      return new Button(x, y, 50, 20, new TextComponent("Unload"), (button) -> {
         Minecraft minecraft = Minecraft.getInstance();
         Player player = minecraft.player;
         Inventory playerInventory = player.getInventory();
         AbstractContainerMenu containerMenu = player.containerMenu;

         List<Slot> containerSlots = containerMenu.slots.stream()
            .filter(slot -> (slot.container != playerInventory))
            .collect(Collectors.toList());

         containerSlots.stream().forEach(sourceSlot -> {
            ItemStack itemStack = sourceSlot.getItem();
            ResourceLocation resourceLocation = ForgeRegistries.ITEMS.getKey(itemStack.getItem());

            if (!itemStack.isEmpty()) {
               LOGGER.info("Unloading container: " + resourceLocation);
               minecraft.gameMode.handleInventoryMouseClick(containerMenu.containerId, sourceSlot.index, 0, ClickType.QUICK_MOVE, player);
            }
         });
      });
   }

   private Button makeFillButton(int x, int y) {
      // "Fill": Move any like items from the inventory to container
      return new Button(x, y, 50, 20, new TextComponent("Fill"), (button) -> {
         Minecraft minecraft = Minecraft.getInstance();
         Player player = minecraft.player;
         Inventory playerInventory = player.getInventory();
         AbstractContainerMenu containerMenu = player.containerMenu;

         List<Slot> containerSlots = containerMenu.slots.stream()
            .filter(slot -> !(slot.container == playerInventory))
            .collect(Collectors.toList());

         List<Slot> allPlayerSlots = containerMenu.slots.stream()
            .filter(slot -> (slot.container == playerInventory))
            .collect(Collectors.toList());


         this.logSlots("Chest", containerSlots);
         this.logSlots("Player", allPlayerSlots);

         // NOTE: The follow loops are not very efficient, but since there's only ~90
         // items we're looping over, it shouldn't be _too_ bad
         containerSlots.stream().forEach(targetSlot -> {
            ItemStack targetItemStack = targetSlot.getItem();
            ResourceLocation resourceLocation = ForgeRegistries.ITEMS.getKey(targetItemStack.getItem());

            if (!targetItemStack.isEmpty()) {
               LOGGER.info("Transferring all of: " + resourceLocation);

               allPlayerSlots.stream().forEach(sourceSlot -> {
                  if (ItemStack.isSame(targetItemStack, sourceSlot.getItem())) {
                     minecraft.gameMode.handleInventoryMouseClick(containerMenu.containerId, sourceSlot.index, 0, ClickType.QUICK_MOVE, player);
                  }
               });
            }
         });
      });
   }

   private Button makeRestockButton(int x, int y) {
      // "Restock": Fill any partial stacks in the container
      return new Button(x, y, 50, 20, new TextComponent("Restock"), (button) -> {
         Minecraft minecraft = Minecraft.getInstance();
         Player player = minecraft.player;
         Inventory playerInventory = player.getInventory();
         AbstractContainerMenu containerMenu = player.containerMenu;

         List<Slot> containerSlots = containerMenu.slots.stream()
            .filter(slot -> !(slot.container == playerInventory))
            .collect(Collectors.toList());

         List<Slot> allPlayerSlots = containerMenu.slots.stream()
            .filter(slot -> (slot.container == playerInventory))
            .collect(Collectors.toList());

         this.logSlots("Chest", containerSlots);
         this.logSlots("Player", allPlayerSlots);

         // NOTE: The follow loops are not very efficient, but since there's only ~90
         // items we're looping over, it shouldn't be _too_ bad
         containerSlots.stream().forEach(targetSlot -> {
            ItemStack targetItemStack = targetSlot.getItem();
            ResourceLocation resourceLocation = ForgeRegistries.ITEMS.getKey(targetItemStack.getItem());

            if (!targetItemStack.isEmpty() && targetItemStack.isStackable() && targetItemStack.getCount() < targetItemStack.getMaxStackSize()) {
               LOGGER.info("Attempting to restock: " + resourceLocation);

               Iterator<Slot> allPlayerInventoryIterator = allPlayerSlots.iterator();

               while (targetItemStack.getCount() < targetItemStack.getMaxStackSize() && allPlayerInventoryIterator.hasNext()) {
                  Slot sourceSlot = allPlayerInventoryIterator.next();
                  ItemStack sourceItemStack = sourceSlot.getItem();

                  if (ItemStack.isSame(targetItemStack, sourceSlot.getItem())) {
                     int neededToFill = targetItemStack.getMaxStackSize() - targetItemStack.getCount();

                     LOGGER.info("Attempting to restock " + resourceLocation + " with " + neededToFill + " items");
                     LOGGER.info("Stack of " + resourceLocation + " has " + sourceItemStack.getCount() + " items");

                     if (sourceItemStack.getCount() <= neededToFill) {
                        LOGGER.info("Quick moving " + sourceItemStack.getCount() + " of " + resourceLocation);
                        minecraft.gameMode.handleInventoryMouseClick(containerMenu.containerId, sourceSlot.index, 0, ClickType.QUICK_MOVE, player);
                     } else {
                        LOGGER.info("Manually moving " + neededToFill + " of " + sourceItemStack.getCount() + " of the item " + resourceLocation);

                        // 1. Pickup stack
                        minecraft.gameMode.handleInventoryMouseClick(containerMenu.containerId, sourceSlot.index, 0, ClickType.PICKUP, player);

                        // 2. Fill stack items
                        for (int i = 0; i < neededToFill; i++) {
                           LOGGER.info(i + " Stack of " + resourceLocation + " has " + targetItemStack.getCount() + " items");
                           minecraft.gameMode.handleInventoryMouseClick(containerMenu.containerId, targetSlot.index, 1, ClickType.PICKUP, player);
                        }

                        // 3. Return unneeded
                        minecraft.gameMode.handleInventoryMouseClick(containerMenu.containerId, sourceSlot.index, 0, ClickType.PICKUP, player);
                     }
                  }
               }
               LOGGER.info("Stack of " + resourceLocation + " now has " + targetItemStack.getCount() + " items");
            }
         });
      });
   }

   private Button makeCombineButton(int x, int y, boolean forPlayerInventory) {
      // "Combine": Merge any partial stacks
      return new Button(x, y, 50, 20, new TextComponent("Combine"), (button) -> {
         Minecraft minecraft = Minecraft.getInstance();
         Player player = minecraft.player;
         Inventory playerInventory = player.getInventory();
         AbstractContainerMenu containerMenu = player.containerMenu;

         List<Slot> containerSlots = containerMenu.slots.stream()
            .filter(slot -> !(slot.container == playerInventory))
            .collect(Collectors.toList());

         List<Slot> allPlayerSlots = containerMenu.slots.stream()
            .filter(slot -> (slot.container == playerInventory))
            .collect(Collectors.toList());

         this.logSlots("Chest", containerSlots);
         this.logSlots("Player", allPlayerSlots);

         List<Slot> slots = (forPlayerInventory ? allPlayerSlots : containerSlots);

         // NOTE: The follow loops are not very efficient, but since there's only ~54
         // items we're looping over, it shouldn't be _too_ bad
         slots.stream().forEach(targetSlot -> {
            ItemStack targetItemStack = targetSlot.getItem();
            ResourceLocation resourceLocation = ForgeRegistries.ITEMS.getKey(targetItemStack.getItem());

            if (!targetItemStack.isEmpty() && targetItemStack.isStackable() && targetItemStack.getCount() < targetItemStack.getMaxStackSize()) {
               LOGGER.info("Attempting to merge: " + resourceLocation);

               for (int sourceSlotIndex = slots.indexOf(targetSlot) + 1; sourceSlotIndex < slots.size(); sourceSlotIndex++) {
                  Slot sourceSlot = slots.get(sourceSlotIndex);
                  ItemStack sourceItemStack = sourceSlot.getItem();

                  if (ItemStack.isSame(targetItemStack, sourceSlot.getItem())) {
                     int neededToFill = targetItemStack.getMaxStackSize() - targetItemStack.getCount();

                     LOGGER.info("Attempting to fill " + resourceLocation + " with " + neededToFill + " items");
                     LOGGER.info("Stack of " + resourceLocation + " has " + sourceItemStack.getCount() + " items");

                     if (sourceItemStack.getCount() <= neededToFill) {
                        LOGGER.info("Moving all " + sourceItemStack.getCount() + " of " + resourceLocation);
                        minecraft.gameMode.handleInventoryMouseClick(containerMenu.containerId, sourceSlot.index, 0, ClickType.PICKUP, player);
                        minecraft.gameMode.handleInventoryMouseClick(containerMenu.containerId, targetSlot.index, 0, ClickType.PICKUP, player);
                     } else {
                        LOGGER.info("Moving " + neededToFill + " of " + sourceItemStack.getCount() + " of " + resourceLocation);

                        // 1. Pickup stack
                        minecraft.gameMode.handleInventoryMouseClick(containerMenu.containerId, sourceSlot.index, 0, ClickType.PICKUP, player);

                        // 2. Fill stack items
                        for (int i = 0; i < neededToFill; i++) {
                           LOGGER.info(i + " Stack of " + resourceLocation + " has " + targetItemStack.getCount() + " items");
                           minecraft.gameMode.handleInventoryMouseClick(containerMenu.containerId, targetSlot.index, 1, ClickType.PICKUP, player);
                        }

                        // 3. Return unneeded
                        minecraft.gameMode.handleInventoryMouseClick(containerMenu.containerId, sourceSlot.index, 0, ClickType.PICKUP, player);
                     }
                  }
               }

               LOGGER.info("Stack of " + resourceLocation + " now has " + targetItemStack.getCount() + " items");
            }
         });
      });
   }

   private void logSlots(String name, List<Slot> slots) {
      slots.stream().forEach(slot -> {
         LOGGER.info(name + " (" + slot.index + "): " + slot.getItem());
      });
   }

}
