package net.smileycorp.ldoh.common;

import goblinbob.mobends.core.addon.AddonHelper;
import ivorius.reccomplex.events.RCEventBus;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.smileycorp.ldoh.common.apocalypseevent.ApocalypseEventListener;
import net.smileycorp.ldoh.common.apocalypseevent.ApocalypseSpawnTable;
import net.smileycorp.ldoh.common.apocalypseevent.CommandBossEvent;
import net.smileycorp.ldoh.common.capabilities.IBreakBlocks;
import net.smileycorp.ldoh.common.capabilities.IBreakBlocks.BreakBlocks;
import net.smileycorp.ldoh.common.capabilities.IHunger;
import net.smileycorp.ldoh.common.capabilities.IHunger.Hunger;
import net.smileycorp.ldoh.common.capabilities.IMiniRaid;
import net.smileycorp.ldoh.common.capabilities.ISpawnTracker;
import net.smileycorp.ldoh.common.capabilities.ISpawnTracker.SpawnTracker;
import net.smileycorp.ldoh.common.capabilities.IUnburiedSpawner;
import net.smileycorp.ldoh.common.capabilities.IUnburiedSpawner.UnburiedSpawner;
import net.smileycorp.ldoh.common.capabilities.MiniRaid;
import net.smileycorp.ldoh.common.network.CommonPacketHandler;
import net.smileycorp.ldoh.integration.mobends.LDOHMobendsAddon;

public class CommonProxy {

	public void preInit(FMLPreInitializationEvent event) {
		//Setup boss event
		ApocalypseSpawnTable.init();
		//Register event listeners
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(new EventListener());
		MinecraftForge.ORE_GEN_BUS.register(new EventListener());
		RCEventBus.INSTANCE.register(new EventListener());
		MinecraftForge.EVENT_BUS.register(new ApocalypseEventListener());
		//Add Safehouse loot tables
		LootTableList.register(ModDefinitions.SAFEHOUSE_CHEST);
		LootTableList.register(ModDefinitions.SAFEHOUSE_CABINET);
		LootTableList.register(ModDefinitions.SAFEHOUSE_FRIDGE);
		//Setup Packets for use
		CommonPacketHandler.initPackets();
	}

	public void init(FMLInitializationEvent event) {
		//Syringe Sterilisation
		GameRegistry.addSmelting(new ItemStack(ModContent.SYRINGE, 1, 3), new ItemStack(ModContent.SYRINGE, 1, 0), 0.1f);
		//Mobends support for nurse model
		AddonHelper.registerAddon(ModDefinitions.modid, new LDOHMobendsAddon());
		//Register Capabilities
		CapabilityManager.INSTANCE.register(ISpawnTracker.class, new ISpawnTracker.Storage(), () -> new SpawnTracker());
		CapabilityManager.INSTANCE.register(IBreakBlocks.class, new IBreakBlocks.Storage(), () -> new BreakBlocks(null));
		CapabilityManager.INSTANCE.register(IUnburiedSpawner.class, new IUnburiedSpawner.Storage(), () -> new UnburiedSpawner(null));
		CapabilityManager.INSTANCE.register(IMiniRaid.class, new IMiniRaid.Storage(), () -> new MiniRaid());
		CapabilityManager.INSTANCE.register(IHunger.class, new IHunger.Storage(), () -> new Hunger());
	}

	public void postInit(FMLPostInitializationEvent event) {

	}

	public void serverStart(FMLServerStartingEvent event) {
		//Register Boss Command
		event.registerServerCommand(new CommandBossEvent());
	}

}