package com.warsheep.scamp.processors;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.warsheep.scamp.PrefabFactory;
import com.warsheep.scamp.components.*;
import com.warsheep.scamp.screens.MainGameScreen;

public class LevelingProcessor extends IteratingSystem {

    private LevelComponent levelComp;
    private Entity entity;
    private final int[] levelMilestones = {2, 4, 7, 12, 18};
    private PrefabFactory fab;

    public LevelingProcessor() {
        super(Family.all(LevelComponent.class).get());
        fab = new PrefabFactory();
    }

    @Override
    public void processEntity(Entity entity, float deltaTime) {
        this.entity = entity;
        levelComp = ECSMapper.level.get(entity);

        int currentLevel = currentLevel();

        if (currentLevel > levelComp.level) {
            levelUp();
        }
    }

    private int currentLevel() {
        return (int) (0.1 * Math.sqrt(levelComp.experiencePoints));
    }

    private void levelUp() {
        levelComp.level++;
        levelComp.nextLevelExp = (levelComp.level+1) * (levelComp.level+1) * 100;

        AttackerComponent atkComp = ECSMapper.attack.get(entity);
        DamageableComponent dmgComp = ECSMapper.damage.get(entity);

        if (atkComp != null && levelComp.level % 2 == 0) { // +1 dmg every other level
            atkComp.baseDamage += levelComp.damageOnLevel;
        }

        if (dmgComp != null) {
            dmgComp.maxHealth += levelComp.healthOnLevel;
            dmgComp.currentHealth = dmgComp.maxHealth;
        }

        if (hitMilestone(levelComp.level)) {
            addSpell(levelComp.level);
        }

        SpellbookComponent spellbook = ECSMapper.spellBook.get(entity);
        if (spellbook != null) {
            if (levelComp.level % 4 == 0) { // TODO: Temp! Change this after MVP
                EffectHealingComponent effectHealingComponent = ECSMapper.effectHealing.get(spellbook.spellbook.get(0));
                effectHealingComponent.healAmount++;
            }
            if (levelComp.level > 4 && levelComp.level % 4 == 0) {
                EffectDamagingComponent effectDamagingComponent = ECSMapper.effectDamaging.get(spellbook.spellbook.get(1));
                effectDamagingComponent.damage++;
            }
            if (levelComp.level > 7 && levelComp.level % 5 == 0) {
                EffectShieldingComponent effectShieldingComponent = ECSMapper.effectShielding.get(spellbook.spellbook.get(2));
                effectShieldingComponent.duration++;
            }
        }
        if (levelComp.level % 5 == 0) {
            ControllableComponent controllableComponent = ECSMapper.control.get(entity);
            if (controllableComponent != null) {
                controllableComponent.movementBonus++;
            }
        }

        resetCooldowns(); // Reset all cooldowns on levelUp
    }

    private boolean hitMilestone(int level) {
        for (int i = 0; i < levelMilestones.length; i++) {
            if (levelMilestones[i] == level) {
                return true;
            }
        }
        return false;
    }

    private void addSpell(int milestone) {
        Entity spell = new Entity();

        EffectCooldownComponent cooldownComponent = new EffectCooldownComponent();

        switch (milestone) {
            case 2:
                spell = this.fab.buildEntity("spells/heal");
                break;
            case 4:
                spell = this.fab.buildEntity("spells/meteor"); // 1 + 8 + 16 = 25 blocks
                break;
            case 7:
                spell = this.fab.buildEntity("spells/shield");
                break;
            case 12:
                spell.add(cooldownComponent);
                break;
            case 18:
                spell.add(cooldownComponent);
                break;
            default:
                break;
        }

        // Add spell to Entity System
        MainGameScreen.ecs.addEntity(spell);

        // Add spell to the player's spellbook
        SpellbookComponent spellbook = ECSMapper.spellBook.get(entity);
        spellbook.spellbook.add(spell);
    }

    private void resetCooldowns() {
        SpellbookComponent spellbook = ECSMapper.spellBook.get(entity);
        for (Entity spell : spellbook.spellbook) {
            EffectCooldownComponent cooldownComponent = ECSMapper.cooldown.get(spell);
            if (cooldownComponent != null) {
                cooldownComponent.currentCooldown = 0;
            }
        }
    }

}
