package net.smileycorp.ldoh.common.capabilities;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.smileycorp.ldoh.common.ModContent;

public interface IBreakBlocks {
	
	public boolean canBreakBlocks();
	
	public boolean tryBreakBlocks();
	
	public void enableBlockBreaking(boolean canBreak);

	public static class Storage implements IStorage<IBreakBlocks> {

		@Override
		public NBTBase writeNBT(Capability<IBreakBlocks> capability, IBreakBlocks instance, EnumFacing side) {
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setBoolean("canBreakBlocks", instance.canBreakBlocks());
			return nbt;
		}
	
		@Override
		public void readNBT(Capability<IBreakBlocks> capability, IBreakBlocks instance, EnumFacing side, NBTBase nbt) {
			instance.enableBlockBreaking(((NBTTagCompound)nbt).getBoolean("canBreakBlocks"));
		}
		
		
	}
	
	public static class BreakBlocks implements IBreakBlocks {
		
		private final EntityLiving entity;
		private final World world;
		
		public BreakBlocks(EntityLiving entity) {
			this.entity = entity;
			this.world = entity == null ? null : entity.world;
		}
		
		private boolean canBreakBlocks = false;
		
		@Override
		public boolean canBreakBlocks() {
			return canBreakBlocks;
		}
		
		@Override
		public boolean tryBreakBlocks() {
			if (canBreakBlocks && entity != null) {
				if (!world.isRemote &! entity.getNavigator().noPath()) {
					RayTraceResult ray = world.rayTraceBlocks(entity.getPositionVector(), entity.getPositionVector().add(entity.getLookVec()));
					if (ray != null) {
						if (ray.typeOfHit == Type.BLOCK) {
							BlockPos pos = ray.getBlockPos();
							Vec3d hit = ray.hitVec;
							EnumFacing facing = EnumFacing.getFacingFromVector((float) hit.x, (float) hit.y, (float) hit.z);
							EnumFacing[] offsets = facing.getAxis().getPlane().facings();
							boolean broke = false;
							for (int i = -1; i <= 1; i++) {
								for (int j = -1; j <= 1; j++) {
									BlockPos pos0 = pos.offset(offsets[0], i).offset(offsets[1], j);
									IBlockState state = world.getBlockState(pos0);
									if (!world.isAirBlock(pos0) && state.getBlock().getHarvestLevel(state) <= 2 ) {
										if (world.destroyBlock(pos0, false)) broke = true;
									}
								}
							}
							return broke;
						}
					}
				}
			}
			return false;
		}

		@Override
		public void enableBlockBreaking(boolean canBreak) {
			canBreakBlocks = canBreak;
		}

	}
	
	public static class Provider implements ICapabilitySerializable<NBTBase> {
		
		protected final IBreakBlocks instance;
		
		public Provider(EntityLiving entity) {
			instance = new BreakBlocks(entity);
		}

		@Override
		public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
			return capability == ModContent.BLOCK_BREAKING;
		}

		@Override
		public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
			return capability == ModContent.BLOCK_BREAKING ? ModContent.BLOCK_BREAKING.cast(instance) : null;
		}

		@Override
		public NBTBase serializeNBT() {
			return ModContent.BLOCK_BREAKING.getStorage().writeNBT(ModContent.BLOCK_BREAKING, instance, null);
		}

		@Override
		public void deserializeNBT(NBTBase nbt) {
			ModContent.BLOCK_BREAKING.getStorage().readNBT(ModContent.BLOCK_BREAKING, instance, null, nbt);
		}

}
 
}