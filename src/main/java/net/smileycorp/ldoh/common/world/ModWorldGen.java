package net.smileycorp.ldoh.common.world;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.smileycorp.ldoh.common.block.LDOHBlocks;
import net.smileycorp.ldoh.common.tile.TileHordeSpawner;
import biomesoplenty.api.biome.BOPBiomes;

import com.legacy.wasteland.world.WastelandWorld;

public class ModWorldGen implements IWorldGenerator {

	@Override
	public void generate(Random rand, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
		//generate horde spawning blocks
		int x = (chunkX << 4) +rand.nextInt(16);
		int z = (chunkZ << 4) + rand.nextInt(16);
		Biome biome = world.getBiomeProvider().getBiomes(null, x, z, 1, 1, false)[0];
		if (rand.nextInt(biome == WastelandWorld.apocalypse_city ? 7 : biome == BOPBiomes.wasteland.get() ? 13 : biome == Biomes.DEEP_OCEAN ? 24 : 18) == 0) {
			BlockPos pos = new BlockPos(x, world.getHeight(x, z)+1, z);
			world.setBlockState(pos, LDOHBlocks.HORDE_SPAWNER.getDefaultState(), 18);
			((TileHordeSpawner)world.getTileEntity(pos)).setNatural();
		}
		BlockPos chunkpos = new BlockPos(x, 0 , z);
		//adds our custom oregen to biomes other than the apocalyptic desert
		if (biome != WastelandWorld.apocalypse_desert) {
			genOre(world, chunkpos, rand, Blocks.COAL_ORE);
			genOre(world, chunkpos, rand, Blocks.IRON_ORE);
			genOre(world, chunkpos, rand, Blocks.GOLD_ORE);
		}
		if (biome == Biomes.DEEP_OCEAN && rand.nextInt(15) == 0) {
			genSurfaceBlock(world, rand, chunkX, chunkZ);
		}
		genNest(world, biome, rand, chunkX, chunkZ);
	}

	protected void genOre(World world, BlockPos chunkpos, Random rand, Block block){
		ChunkGeneratorSettings.Factory factory = new ChunkGeneratorSettings.Factory();
		factory.coalMaxHeight = 28;
		factory.ironMaxHeight = 28;
		factory.goldMaxHeight = 28;
		factory.coalCount = (int) Math.round(factory.coalCount/2.5d);
		factory.ironCount = Math.round(factory.ironCount/2);
		ChunkGeneratorSettings provider = factory.build();
		WorldGenerator generator = null;
		int  count = 0;
		if (block == Blocks.COAL_ORE) {
			generator = new WorldGenMinable(block.getDefaultState(), provider.coalSize);
			count = provider.coalCount;
		} else if (block == Blocks.IRON_ORE) {
			generator = new WorldGenMinable(block.getDefaultState(), provider.ironSize);
			count = provider.ironCount;
		} else if (block == Blocks.GOLD_ORE) {
			generator = new WorldGenMinable(block.getDefaultState(), provider.goldSize);
			count = provider.goldCount;
		}
		for (int j = 0; j < count; ++j) {
			BlockPos blockpos = chunkpos.add(rand.nextInt(16), rand.nextInt(30), rand.nextInt(16));
			generator.generate(world, rand, blockpos);
		}
	}

	private void genSurfaceBlock(World world, Random rand, int chunkX, int chunkZ) {
		int x = (chunkX << 4) +rand.nextInt(16);
		int z = (chunkZ << 4) + rand.nextInt(16);
		WorldGenSurface gen = new WorldGenSurface();
		gen.generate(world, rand, new BlockPos(x, world.getChunkFromChunkCoords(chunkX, chunkZ).getHeightValue(x&15, z&15)-1, z));
	}

	private void genNest(World world, Biome biome, Random rand, int chunkX, int chunkZ) {
		if (biome != BOPBiomes.wasteland.get()) return;
		if (rand.nextInt(160) == 0) {
			int x = (chunkX << 4) +rand.nextInt(16);
			int z = (chunkZ << 4) + rand.nextInt(16);
			new WorldGenNest().generate(world, rand, world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z)));
		}
	}

}
