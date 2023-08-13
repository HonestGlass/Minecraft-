package net.smileycorp.ldoh.common.entity;

import com.dhanantry.scapeandrunparasites.entity.ai.misc.EntityParasiteBase;
import com.google.common.collect.Lists;
import com.mrcrayfish.guns.GunConfig;
import com.mrcrayfish.guns.common.ProjectileFactory;
import com.mrcrayfish.guns.entity.EntityProjectile;
import com.mrcrayfish.guns.init.ModGuns;
import com.mrcrayfish.guns.init.ModSounds;
import com.mrcrayfish.guns.item.AmmoRegistry;
import com.mrcrayfish.guns.item.ItemGun;
import com.mrcrayfish.guns.network.PacketHandler;
import com.mrcrayfish.guns.network.message.MessageBullet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.scoreboard.Team;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.smileycorp.atlas.api.util.DataUtils;
import net.smileycorp.ldoh.common.LDOHTweaks;
import net.smileycorp.ldoh.common.entity.ai.AITurretShoot;
import net.smileycorp.ldoh.common.entity.ai.AITurretTarget;
import net.smileycorp.ldoh.common.inventory.InventoryTurretAmmo;
import net.smileycorp.ldoh.common.inventory.InventoryTurretUpgrades;
import net.smileycorp.ldoh.common.item.ItemTurretUpgrade;
import net.smileycorp.ldoh.common.item.LDOHItems;
import net.smileycorp.ldoh.common.tile.TileAbstractTurret;
import net.smileycorp.ldoh.common.tile.TileTurret;
import net.smileycorp.ldoh.common.util.ModUtils;
import net.smileycorp.ldoh.common.util.TurretUpgrade;
import rafradek.TF2weapons.TF2weapons;
import rafradek.TF2weapons.entity.mercenary.EntityTF2Character;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class EntityTurret extends EntityAbstractTurret<TileTurret, EntityTurret> {

	protected static final DataParameter<String> TEAM = EntityDataManager.<String>createKey(EntityTurret.class, DataSerializers.STRING);
	protected static final DataParameter<Boolean> IS_ENEMY = EntityDataManager.<Boolean>createKey(EntityTurret.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<EnumFacing> FACING = EntityDataManager.<EnumFacing>createKey(EntityTurret.class, DataSerializers.FACING);
	protected static final DataParameter<Integer> COOLDOWN = EntityDataManager.<Integer>createKey(EntityTurret.class, DataSerializers.VARINT);
	protected static final DataParameter<Float> SPIN = EntityDataManager.<Float>createKey(EntityTurret.class, DataSerializers.FLOAT);
	protected static final DataParameter<Integer> TARGET = EntityDataManager.<Integer>createKey(EntityTurret.class, DataSerializers.VARINT);

	//read these values from the nbt and cache them to make sure it gets loaded / synced properly
	protected static final DataParameter<BlockPos> TILE_POS = EntityDataManager.<BlockPos>createKey(EntityTurret.class, DataSerializers.BLOCK_POS);
	protected static final DataParameter<String> OWNER_UUID = EntityDataManager.<String>createKey(EntityTurret.class, DataSerializers.STRING);

	public static final DataParameter<BlockPos> TURRET_UPGRADES = EntityDataManager.<BlockPos>createKey(EntityTurret.class, DataSerializers.BLOCK_POS);

	protected EntityLivingBase target = null;
	protected InventoryTurretAmmo inventory = new InventoryTurretAmmo();
	protected InventoryTurretUpgrades upgrades = new InventoryTurretUpgrades(this);

	protected EntityPlayer owner = null;
	protected TileTurret tile = null;
	protected String username = null;
	protected Vec3d turretPos;

	public EntityTurret(World world) {
		super(world);
		setSize(0.5f, 0.5f);
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		dataManager.register(TEAM, "");
		dataManager.register(IS_ENEMY, false);
		dataManager.register(FACING, EnumFacing.UP);
		dataManager.register(COOLDOWN, 0);
		dataManager.register(SPIN, 0f);
		dataManager.register(TARGET, getEntityId());
		dataManager.register(TILE_POS, null);
		dataManager.register(OWNER_UUID, "");
		dataManager.register(TURRET_UPGRADES, new BlockPos(0, 0, 0));
	}

	@Override
	public void applyEntityAttributes() {
		super.applyEntityAttributes();
		getAttributeMap().getAttributeInstance(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0);
		getAttributeMap().getAttributeInstance(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40);
		getAttributeMap().getAttributeInstance(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(255);
	}

	@Override
	protected void initEntityAI() {
		targetTasks.addTask(1, new AITurretTarget(this));
		tasks.addTask(1, new AITurretShoot(this));
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);
		if (nbt.hasKey("inventory")) inventory.readFromNBT(nbt.getCompoundTag("inventory"));
		if (nbt.hasKey("isEnemy")) dataManager.set(IS_ENEMY, nbt.getBoolean("isEnemy"));
		if (!isEnemy() && nbt.hasKey("owner")) dataManager.set(OWNER_UUID, nbt.getString("owner"));
		if (nbt.hasKey("tilePos")) {
			NBTTagCompound pos = nbt.getCompoundTag("tilePos");
			dataManager.set(TILE_POS, new BlockPos(pos.getInteger("x"), pos.getInteger("y"), pos.getInteger("z")));
		}
		if (nbt.hasKey("facing")) {
			dataManager.set(FACING, EnumFacing.getFront(nbt.getInteger("facing")));
		}
		if (nbt.hasKey("cooldown")) dataManager.set(COOLDOWN, nbt.getInteger("cooldown"));
		if (nbt.hasKey("spin")) dataManager.set(SPIN, nbt.getFloat("spin"));
		if (nbt.hasKey("upgrades")) dataManager.set(TURRET_UPGRADES, ModUtils.arrayToPos(nbt.getIntArray("upgrades")));
	}

	@Override
	public boolean canBePushed() {
		return false;
	}

	@Override
	public float getEyeHeight() {
		return 0.25f;
	}

	@Override
	protected void collideWithEntity(Entity entityIn){}

	@Override
	public void onLivingUpdate() {
		if (tile == null) {
			BlockPos tilepos = dataManager.get(TILE_POS);
			TileEntity tile = world.getTileEntity(getPosition().add(tilepos));
			if (tile instanceof TileTurret) {
				this.tile = (TileTurret) tile;
				((TileTurret) tile).setEntity(this);
			} else if (world.getTileEntity(tilepos) instanceof TileTurret) {// update turrets placed before 0.4.6 to the new system
				tile = world.getTileEntity(tilepos);
				this.tile = (TileTurret) tile;
				((TileTurret) tile).setEntity(this);
				dataManager.set(TILE_POS, tile.getPos().subtract(this.getPosition()));
			}
		}
		if (hasTarget()) {
			EntityLivingBase target = getTarget();
			getLookHelper().setLookPosition(target.posX + (target.width*0.5), target.posY + (target.height * 0.75), target.posZ + (target.width*0.5), 10, 90);
		}
		if (getCooldown() > 0) {
			setCooldown(getCooldown()-1);
			setSpin(getSpin()+0.34906585f);
		}
		super.onLivingUpdate();
	}

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		BlockPos pos = dataManager.get(TILE_POS);
		if (pos != null) {
			BlockPos tilepos = getPosition().add(pos);
			world.destroyBlock(tilepos, false);
			world.removeTileEntity(tilepos);
		}
		if (world.isRemote) return;
		for (ItemStack stack : inventory.getItems()) entityDropItem(stack, 0.0f);
		for (TurretUpgrade upgrade : getInstalledUpgrades()) if (!isEnemy() || rand.nextInt(100) < 25) entityDropItem(upgrade.getItem(), 0.0f);
		if (isEnemy()) {
			entityDropItem(new ItemStack(ModGuns.BASIC_AMMO, rand.nextInt(15)+10), 0.0f);
			entityDropItem(new ItemStack(LDOHItems.INCENDIARY_AMMO, rand.nextInt(6)), 0.0f);
			entityDropItem(new ItemStack(LDOHItems.DIAMOND_NUGGET, rand.nextInt(3)+1), 0.0f);
			entityDropItem(new ItemStack(Items.QUARTZ, rand.nextInt(3)+1), 0.0f);
			if (rand.nextInt(100) < 50) entityDropItem(new ItemStack(TF2weapons.itemTF2, 1, 3), 0.0f);
			if (rand.nextInt(100) < 25) entityDropItem(new ItemStack(ModGuns.CHAIN_GUN), 0.0f);
		}
	}

	@Override
	public void dropEnemyItems() {
		entityDropItem(new ItemStack(ModGuns.BASIC_AMMO, rand.nextInt(15)+10), 0.0f);
		entityDropItem(new ItemStack(LDOHItems.INCENDIARY_AMMO, rand.nextInt(6)), 0.0f);
		entityDropItem(new ItemStack(LDOHItems.DIAMOND_NUGGET, rand.nextInt(3)+1), 0.0f);
		entityDropItem(new ItemStack(Items.QUARTZ, rand.nextInt(3)+1), 0.0f);
		if (rand.nextInt(100) < 50) entityDropItem(new ItemStack(TF2weapons.itemTF2, 1, 3), 0.0f);
		if (rand.nextInt(100) < 25) entityDropItem(new ItemStack(ModGuns.CHAIN_GUN), 0.0f);
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (source != null) {
			if (source.getTrueSource() instanceof EntityLivingBase) {
				EntityLivingBase entity = (EntityLivingBase) source.getTrueSource();
				if (canTarget(entity)) setTarget(entity);
			} else if (source.getImmediateSource() instanceof EntityLivingBase) {
				EntityLivingBase entity = (EntityLivingBase) source.getImmediateSource();
				if (canTarget(entity)) setTarget(entity);
			}
		}
		return super.attackEntityFrom(source, amount);
	}

	public boolean hasTarget() {
		EntityLivingBase target = getTarget();
		if (target == null) return false;
		return !target.isDead && target.isAddedToWorld() && canTarget(target) && getEntitySenses().canSee(target);
	}

	public void setTarget(EntityLivingBase target) {
		this.target = target;
		dataManager.set(TARGET, target == null ? getEntityId() : target.getEntityId());
	}

	public EntityLivingBase getTarget() {
		if (world.isRemote) {
			int id = dataManager.get(TARGET);
			if (id != getEntityId()) {
				Entity target = world.getEntityByID(id);
				if (target instanceof EntityLivingBase) return (EntityLivingBase) target;
			}
			return null;
		} else return target;
	}

	@Override
	public boolean hasNoGravity() {
		return true;
	}

	@Override
	protected boolean canDespawn() {
		return false;
	}

	@Override
	public boolean isInRangeToRenderDist(double distance) {
		double d0 = this.getEntityBoundingBox().getAverageEdgeLength() * 10.0D;
		if (Double.isNaN(d0)) {
			d0 = 1.0D;
		}
		d0 = d0 * 64.0D * getRenderDistanceWeight();
		return distance < d0 * d0;
	}

	@Override
	public boolean isEnemy() {
		return dataManager.get(IS_ENEMY);
	}

	@Override
	public Team getTeam() {
		if (isEnemy()) return null; //TODO: add green team (optional)
		String team = dataManager.get(TEAM);
		return team.isEmpty() ? null : world.getScoreboard().getTeam(team);
	}

	public boolean isSameTeam(Entity entity) {
		if (isEnemy()) {
			if (entity instanceof IEnemyMachine) return (((IEnemyMachine) entity).isEnemy());
			return false;
		}
		if (getTeam() == null) return entity.getTeam() == null;
		return getTeam().equals(entity.getTeam());
	}

	public boolean canTarget(EntityLivingBase entity) {
		if (entity == null) return false;
		if (getDistance(entity) > getRange()) return false;
		if (isEnemy()) {
			if (entity instanceof EntityPlayer) return ((EntityPlayer) entity).isSpectator() || ((EntityPlayer) entity).isCreative() ? false : true;
			if (entity instanceof EntityTF2Character && entity.getTeam() != null) {
				if (!entity.getTeam().getName().equals("GREEN")) return true;
			}
		}
		return ModUtils.canTarget(this, entity);
	}

	public EntityPlayer getOwner() {
		return isEnemy() ? null : owner;
	}

	public UUID getOwnerUUID() {
		if (isEnemy()) return null;
		String uuidString = dataManager.get(OWNER_UUID);
		return DataUtils.isValidUUID(uuidString) ? UUID.fromString(uuidString) : null;
	}

	public EnumFacing getFacing() {
		return dataManager.get(FACING);
	}

	public int getCooldown() {
		return dataManager.get(COOLDOWN);
	}

	public void setCooldown(int cooldown) {
		dataManager.set(COOLDOWN, cooldown);
	}

	public float getSpin() {
		return dataManager.get(SPIN);
	}

	public void setSpin(float spin) {
		dataManager.set(SPIN, spin);
	}

	public InventoryTurretAmmo getInventory() {
		return inventory;
	}

	public InventoryTurretUpgrades getUpgradeInventory() {
		return upgrades;
	}

	public boolean hasAmmo() {
		return inventory.hasAmmo();
	}

	public ItemStack getAmmo(Entity target) {
		boolean optimize = hasUpgrade(TurretUpgrade.AMMO_OPTIMIZATION);
		return isEnemy() ? new ItemStack((target instanceof EntityParasiteBase && optimize) ?
				LDOHItems.INCENDIARY_AMMO : ModGuns.BASIC_AMMO) : inventory.getAmmo(optimize ? target : null);
	}

	@SideOnly(Side.CLIENT)
	public String getOwnerUsername() {
		return username;
	}

	public boolean isActive() {
		if (hasUpgrade(TurretUpgrade.REDSTONE_CONTROL)) {
			BlockPos tile_pos = dataManager.get(TILE_POS);
			if (tile_pos != null) return world.isBlockPowered(tile_pos);
		}
		return true;
	}

	public int getFireRate() {
		return 4 - getUpgradeCount(TurretUpgrade.BARREL_SPIN);
	}

	public int getRange () {
		return 30 + (20 * getUpgradeCount(TurretUpgrade.RANGE));
	}

	public int getProjectileSpeed() {
		return 10 * (1 + getUpgradeCount(TurretUpgrade.RIFLING));
	}

	@Override
	public String getName() {
		return isEnemy() ? I18n.translateToLocal("entity.hundreddayz.EnemyTurret.name") : super.getName();
	}

	public List<TurretUpgrade> getInstalledUpgrades() {
		List<TurretUpgrade> upgrades = Lists.newArrayList();
		for (int i : ModUtils.posToArray(dataManager.get(TURRET_UPGRADES))) if (!TurretUpgrade.isBlank(i)) upgrades.add(TurretUpgrade.get(i));
		return upgrades;
	}

	public boolean hasUpgrade(TurretUpgrade upgrade) {
		for (int i : ModUtils.posToArray(dataManager.get(TURRET_UPGRADES))) if (TurretUpgrade.get(i) == upgrade) return true;
		return false;
	}

	public int getUpgradeCount(TurretUpgrade upgrade) {
		if (upgrade == null) return 0;
		List<TurretUpgrade> upgrades = getInstalledUpgrades();
		if (!upgrade.isStackable()) return upgrades.contains(upgrade) ? 1 :0;
		int count = 0;
		for (int i : ModUtils.posToArray(dataManager.get(TURRET_UPGRADES))) if (TurretUpgrade.get(i) == upgrade) count++;
		return count;
	}

	public boolean applyUpgrade(ItemStack upgrade) {
		int[] array =  ModUtils.posToArray(dataManager.get(TURRET_UPGRADES));
		for (int i = 0; i < array.length; i++) {
			int id = array[i];
			if (TurretUpgrade.isBlank(id)) {
				array[i] = upgrade.getMetadata();
				dataManager.set(TURRET_UPGRADES, ModUtils.arrayToPos(array));
				return true;
			}
		}
		return false;
	}

	public void updateUpgrades(TurretUpgrade... upgrades) {
		int[] array = new int[] {0, 0, 0};
		for (int i = 0; i < upgrades.length; i++) {
			if (i == array.length) break;
			TurretUpgrade upgrade = upgrades[i];
			if (upgrade == null || upgrade == TurretUpgrade.BLANK) continue;
			array[i] = upgrade.ordinal();
		}
		dataManager.set(TURRET_UPGRADES, ModUtils.arrayToPos(array));
	}

	public void updateUpgrades(int[] upgrades) {
		if (upgrades.length < 3) return;
		dataManager.set(TURRET_UPGRADES, ModUtils.arrayToPos(upgrades));
	}

	public boolean hasUpgrades() {
		for (int upgrade : ModUtils.posToArray(dataManager.get(TURRET_UPGRADES))) {
			if (!TurretUpgrade.isBlank(upgrade)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void shoot(Vec3d pos) {
		ItemStack ammo = getAmmo(getTarget());
		ProjectileFactory factory = AmmoRegistry.getInstance().getFactory(ammo.getItem().getRegistryName());
		EntityProjectile bullet = factory.create(world, this, (ItemGun) ModGuns.CHAIN_GUN, fakegun);
		bullet.setPosition(pos.x, pos.y, pos.z);
		bullet.motionX = dir.x * SPEED;
		bullet.motionY = dir.y * SPEED;
		bullet.motionZ = dir.z * SPEED;
		turret.world.spawnEntity(bullet);
		ammo.shrink(1);
		turret.setCooldown(turret.getFireRate());
		String sound = fakegun.sounds.getFire(fakegun);
		SoundEvent event = ModSounds.getSound(sound);
		if(event == null) event = SoundEvent.REGISTRY.getObject(new ResourceLocation(sound));
		if(event != null) turret.world.playSound(null, turret.getPosition(), event, SoundCategory.HOSTILE, 5.0F, 0.8F + turret.world.rand.nextFloat() * 0.2F);
		MessageBullet messageBullet = new MessageBullet(bullet.getEntityId(), bullet.posX, bullet.posY, bullet.posZ, bullet.motionX, bullet.motionY, bullet.motionZ, 0, 0);
		PacketHandler.INSTANCE.sendToAllAround(messageBullet, new NetworkRegistry.TargetPoint(turret.dimension, turret.posX, turret.posY, turret.posZ, GunConfig.SERVER.network.projectileTrackingRange));
		if (ray.entityHit != target &! ray.entityHit.isDead) {
			turret.setTarget((EntityLivingBase) ray.entityHit);
		}
	}

}
