package net.smileycorp.ldoh.integration.mobends;

import goblinbob.mobends.core.addon.AddonAnimationRegistry;
import goblinbob.mobends.core.addon.AddonHelper;
import goblinbob.mobends.core.addon.IAddon;
import goblinbob.mobends.standard.client.renderer.entity.mutated.ZombieRenderer;
import net.smileycorp.ldoh.common.ModDefinitions;
import net.smileycorp.ldoh.common.entity.EntityZombieFireman;
import net.smileycorp.ldoh.common.entity.EntityZombieNurse;

public class LDOHMobendsAddon implements IAddon {

	public void register() {
		AddonHelper.registerAddon(ModDefinitions.MODID, this);
	}

	@Override
	public String getDisplayName() {
		return ModDefinitions.NAME;
	}

	@Override
	public void registerContent(AddonAnimationRegistry registry) {
		registry.registerNewEntity(EntityZombieNurse.class, ZombieNurseData::new, ZombieNurseMutator::new, new ZombieRenderer<EntityZombieNurse>(),
				"head", "body", "leftArm", "rightArm", "leftForeArm", "rightForeArm",
				"leftLeg", "rightLeg", "leftForeLeg", "rightForeLeg");
		registry.registerNewEntity(EntityZombieFireman.class, ZombieFiremanData::new, ZombieFiremanMutator::new, new ZombieRenderer<EntityZombieFireman>(),
				"head", "body", "leftArm", "rightArm", "leftForeArm", "rightForeArm",
				"leftLeg", "rightLeg", "leftForeLeg", "rightForeLeg");
	}

}
