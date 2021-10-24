package net.smileycorp.ldoh.common.tile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.smileycorp.ldoh.common.block.BlockLandmine;

public class TileLandmine extends TileEntity implements ITickable {
	
	private int cooldown = 5;
	private int primeTimer = 2400;
	
	@Override
	public void update() {
		if (!world.isRemote) {
			IBlockState state = world.getBlockState(pos);
			if (!state.getValue(BlockLandmine.PRIMED)) {
				if (primeTimer-- <= 0) {
					BlockLandmine.prime(world, pos, state);
				}
			}
			if (state.getValue(BlockLandmine.PRESSED)) {
				if (world.getEntitiesWithinAABB(EntityLivingBase.class, BlockLandmine.HITBOX_AABB.offset(pos)).isEmpty()) {
					if (cooldown--<=0) BlockLandmine.explode(world, pos);
        		}
        	}
		}	
	}
	
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		if (compound.hasKey("primeTimer")) {
			primeTimer = compound.getInteger("primeTimer");
		}
		if (compound.hasKey("cooldown")) {
			cooldown = compound.getInteger("cooldown");
		}
        super.readFromNBT(compound);
    }

    @Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
    	compound.setInteger("primeTimer", primeTimer);
        compound.setInteger("cooldown", cooldown);
        return super.writeToNBT(compound);
    }

}
