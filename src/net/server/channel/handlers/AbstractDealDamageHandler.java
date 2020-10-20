/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.server.channel.handlers;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import client.inventory.Equip;
import client.inventory.MapleInventoryType;
import config.YamlConfig;
import constants.inventory.ItemConstants;
import constants.skills.*;
import net.AbstractMaplePacketHandler;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.TimerManager;
import server.life.Element;
import server.life.ElementalEffectiveness;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.life.MonsterDropEntry;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.LittleEndianAccessor;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleJob;
import client.Skill;
import client.SkillFactory;
import client.autoban.AutobanFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.game.GameConstants;
import net.server.PlayerBuffValueHolder;
import scripting.AbstractPlayerInteraction;

public abstract class AbstractDealDamageHandler extends AbstractMaplePacketHandler {

    public static class AttackInfo {

        public int numAttacked, numDamage, numAttackedAndDamage, skill, skilllevel, stance, direction, rangedirection, charge, display;
        public Map<Integer, List<Integer>> allDamage;
        public boolean ranged, magic;
        public int speed = 4;
        public Point position = new Point();

        public MapleStatEffect getAttackEffect(MapleCharacter chr, Skill theSkill) {
            Skill mySkill = theSkill;
            if (mySkill == null) {
                mySkill = SkillFactory.getSkill(skill);
            }

            int skillLevel = chr.getSkillLevel(mySkill);
            if (skillLevel == 0 && GameConstants.isPqSkillMap(chr.getMapId()) && GameConstants.isPqSkill(mySkill.getId()))
                skillLevel = 1;

            if (skillLevel == 0) {
                return null;
            }
            if (display > 80) { //Hmm
                if (!mySkill.getAction()) {
                    AutobanFactory.FAST_ATTACK.autoban(chr, "WZ Edit; adding action to a skill: " + display);
                    return null;
                }
            }
            return mySkill.getEffect(skillLevel);
        }
    }

    protected void applyAttack(AttackInfo attack, final MapleCharacter player, int attackCount) {
        final MapleMap map = player.getMap();
        if (map.isOwnershipRestricted(player)) {
            return;
        }

        Skill theSkill = null;
        MapleStatEffect attackEffect = null;
        final int job = player.getJob().getId();
        try {
            if (player.isBanned()) {
                return;
            }
            if (attack.skill != 0) {
                theSkill = SkillFactory.getSkill(attack.skill); // thanks Conrad for noticing some Aran skills not consuming MP
                attackEffect = attack.getAttackEffect(player, theSkill); //returns back the player's attack effect so we are gucci
                if (attackEffect == null) {
                    player.announce(MaplePacketCreator.enableActions());
                    return;
                }

                if (player.getMp() < attackEffect.getMpCon()) {
                    if (!(player.getMapId() == 801040100 || player.getMapId() == 280030000))
                        AutobanFactory.MPCON.addPoint(player.getAutobanManager(), "Skill: " + attack.skill + "; Player MP: " + player.getMp() + "; MP Needed: " + attackEffect.getMpCon());
                }

                int mobCount = attackEffect.getMobCount();
                if (attack.skill != Cleric.HEAL) {
                    if (attack.skill == DawnWarrior.FINAL_ATTACK || attack.skill == WindArcher.FINAL_ATTACK || attack.skill == Page.FINAL_ATTACK_BW || attack.skill == Page.FINAL_ATTACK_SWORD || attack.skill == Fighter.FINAL_ATTACK_SWORD
                            || attack.skill == Fighter.FINAL_ATTACK_AXE || attack.skill == Spearman.FINAL_ATTACK_SPEAR || attack.skill == Spearman.FINAL_ATTACK_POLEARM
                            || attack.skill == Hunter.FINAL_ATTACK || attack.skill == Crossbowman.FINAL_ATTACK) {

                        mobCount = 15;
                    } else if (attack.skill == Aran.HIDDEN_FULL_DOUBLE || attack.skill == Aran.HIDDEN_FULL_TRIPLE || attack.skill == Aran.HIDDEN_OVER_DOUBLE || attack.skill == Aran.HIDDEN_OVER_TRIPLE) {
                        mobCount = 12;
                    }
                    if (player.isAlive()) {
                        if (attack.skill == NightWalker.POISON_BOMB) {// Poison Bomb
                            attackEffect.applyTo(player, new Point(attack.position.x, attack.position.y));
                        } else {
                            attackEffect.applyTo(player);
                        }
                    } else {
                        player.announce(MaplePacketCreator.enableActions());
                    }
                }

                if (attack.numAttacked > mobCount) {
                    AutobanFactory.MOB_COUNT.autoban(player, "Skill: " + attack.skill + "; Count: " + attack.numAttacked + " Max: " + mobCount);
                    return;
                }
            }
            if (!player.isAlive()) {
                return;
            }

            //WTF IS THIS F3,1
            /*if (attackCount != attack.numDamage && attack.skill != ChiefBandit.MESO_EXPLOSION && attack.skill != NightWalker.VAMPIRE && attack.skill != WindArcher.WIND_SHOT && attack.skill != Aran.COMBO_SMASH && attack.skill != Aran.COMBO_FENRIR && attack.skill != Aran.COMBO_TEMPEST && attack.skill != NightLord.NINJA_AMBUSH && attack.skill != Shadower.NINJA_AMBUSH) {
                return;
            }*/

            int totDamage = 0;

            if (attack.skill == ChiefBandit.MESO_EXPLOSION) {
                int delay = 0;
                for (Integer oned : attack.allDamage.keySet()) {
                    MapleMapObject mapobject = map.getMapObject(oned.intValue());
                    if (mapobject != null && mapobject.getType() == MapleMapObjectType.ITEM) {
                        final MapleMapItem mapitem = (MapleMapItem) mapobject;
                        if (mapitem.getMeso() == 0) { //Maybe it is possible some how?
                            return;
                        }

                        mapitem.lockItem();
                        try {
                            if (mapitem.isPickedUp()) {
                                return;
                            }
                            TimerManager.getInstance().schedule(new Runnable() {
                                @Override
                                public void run() {
                                    mapitem.lockItem();
                                    try {
                                        if (mapitem.isPickedUp()) {
                                            return;
                                        }
                                        map.pickItemDrop(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 4, 0), mapitem);
                                    } finally {
                                        mapitem.unlockItem();
                                    }
                                }
                            }, delay);
                            delay += 100;
                        } finally {
                            mapitem.unlockItem();
                        }
                    } else if (mapobject != null && mapobject.getType() != MapleMapObjectType.MONSTER) {
                        return;
                    }
                }
            }
            for (Integer oned : attack.allDamage.keySet()) {
                final MapleMonster monster = map.getMonsterByOid(oned.intValue());
                if (monster != null) {
                    double distance = player.getPosition().distanceSq(monster.getPosition());
                    double distanceToDetect = 400000.0;

                    if (attack.ranged)
                        distanceToDetect += 450000;

                    if (attack.magic)
                        distanceToDetect += 250000;

                    if (player.getJob().isA(MapleJob.ARAN1))
                        distanceToDetect += 250000; // Arans have extra range over normal warriors.

                    if (player.getJob().isA(MapleJob.CORSAIR))
                        distanceToDetect += 500000;

                    if (attack.skill == Aran.COMBO_SMASH || attack.skill == Aran.BODY_PRESSURE)
                        distanceToDetect += 45000;

                    else if (attack.skill == Bishop.GENESIS || attack.skill == ILArchMage.BLIZZARD || attack.skill == FPArchMage.METEOR_SHOWER)
                        distanceToDetect += 275000;

                    else if (attack.skill == Hero.BRANDISH || attack.skill == DragonKnight.SPEAR_CRUSHER || attack.skill == DragonKnight.POLE_ARM_CRUSHER)
                        distanceToDetect += 43000;

                    else if (attack.skill == DragonKnight.DRAGON_ROAR || attack.skill == SuperGM.SUPER_DRAGON_ROAR)
                        distanceToDetect += 250000;

                    else if (attack.skill == Shadower.BOOMERANG_STEP)
                        distanceToDetect += 65000;

                    if (distance > distanceToDetect) {
                        AutobanFactory.DISTANCE_HACK.alert(player, "Distance Sq to monster: " + distance + " SID: " + attack.skill + " MID: " + monster.getId());
                        monster.refreshMobPosition();
                    }

                    int totDamageToOneMonster = 0;
                    List<Integer> onedList = attack.allDamage.get(oned);

                    if (attack.magic) { // thanks BHB, Alex (CanIGetaPR) for noticing no immunity status check here
                        if (monster.isBuffed(MonsterStatus.MAGIC_IMMUNITY)) {
                            Collections.fill(onedList, 1);
                        }
                    } else {
                        if (monster.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) {
                            Collections.fill(onedList, 1);
                        }
                    }

                    for (Integer eachd : onedList) {
                        if (eachd < 0) eachd += Integer.MAX_VALUE;
                        totDamageToOneMonster += eachd;
                    }
                    totDamage += totDamageToOneMonster;
                    monster.aggroMonsterDamage(player, totDamageToOneMonster);
                    if (player.getBuffedValue(MapleBuffStat.PICKPOCKET) != null && (attack.skill == 0 || attack.skill == Rogue.DOUBLE_STAB || attack.skill == Bandit.SAVAGE_BLOW || attack.skill == ChiefBandit.ASSAULTER || attack.skill == ChiefBandit.BAND_OF_THIEVES || attack.skill == Shadower.ASSASSINATE || attack.skill == Shadower.TAUNT || attack.skill == Shadower.BOOMERANG_STEP)) {
                        Skill pickpocket = SkillFactory.getSkill(ChiefBandit.PICKPOCKET);
                        int picklv = (player.isGM()) ? pickpocket.getMaxLevel() : player.getSkillLevel(pickpocket);
                        if (picklv > 0) {
                            int delay = 0;
                            final int maxmeso = player.getBuffedValue(MapleBuffStat.PICKPOCKET).intValue();
                            for (Integer eachd : onedList) {
                                eachd += Integer.MAX_VALUE;

                                if (pickpocket.getEffect(picklv).makeChanceResult()) {
                                    final Integer eachdf;
                                    if (eachd < 0)
                                        eachdf = eachd + Integer.MAX_VALUE;
                                    else
                                        eachdf = eachd;

                                    TimerManager.getInstance().schedule(new Runnable() {
                                        @Override
                                        public void run() {
                                            map.spawnMesoDrop(Math.min((int) Math.max(((double) eachdf / (double) 20000) * (double) maxmeso, (double) 1), maxmeso), new Point((int) (monster.getPosition().getX() + Randomizer.nextInt(100) - 50), (int) (monster.getPosition().getY())), monster, player, true, (byte) 2);
                                        }
                                    }, delay);
                                    delay += 100;
                                }
                            }
                        }
                    } else if (attack.skill == Marauder.ENERGY_DRAIN || attack.skill == ThunderBreaker.ENERGY_DRAIN || attack.skill == NightWalker.VAMPIRE || attack.skill == Assassin.DRAIN) {
                        long healHp = Math.min(monster.getMaxHp(), Math.min((int) ((double) totDamage * (double) SkillFactory.getSkill(attack.skill).getEffect(player.getSkillLevel(SkillFactory.getSkill(attack.skill))).getX() / 100.0), player.getCurrentMaxHp() / 2));
                        player.addHP((int) healHp);
                    } else if (attack.skill == Bandit.STEAL) {
                        Skill steal = SkillFactory.getSkill(Bandit.STEAL);
                        if (monster.getStolen().size() < 1) { // One steal per mob <3
                            monster.addStolen(0); // set the steal even if it fails
                            if (steal.getEffect(player.getSkillLevel(steal)).makeChanceResult()) {

                                MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
                                List<Integer> dropPool = mi.retrieveDropPool(monster.getId());
                                if (!dropPool.isEmpty()) {
                                    Integer rndPool = (int) Math.floor(Math.random() * dropPool.get(dropPool.size() - 1));

                                    int i = 0;
                                    while (rndPool >= dropPool.get(i)) i++;

                                    List<MonsterDropEntry> toSteal = new ArrayList<>();
                                    MonsterDropEntry mde = mi.retrieveDrop(monster.getId()).get(i);
                                    MonsterDropEntry stolenDrop = new MonsterDropEntry(mde.itemId, mde.chance, mde.Minimum, mde.Maximum, mde.questid, mde.shouldStack);
                                    if (!stolenDrop.shouldStack) {
                                        stolenDrop.Minimum = 1;
                                        stolenDrop.Maximum = 1;
                                    }
                                    toSteal.add(stolenDrop);

                                    map.dropItemsFromMonster(toSteal, player, monster);
                                    monster.addStolen(toSteal.get(0).itemId);
                                }
                            }
                        }
                    } else if (attack.skill == FPArchMage.FIRE_DEMON) {
                        monster.setTempEffectiveness(Element.ICE, ElementalEffectiveness.WEAK, SkillFactory.getSkill(FPArchMage.FIRE_DEMON).getEffect(player.getSkillLevel(SkillFactory.getSkill(FPArchMage.FIRE_DEMON))).getDuration() * 1000);
                    } else if (attack.skill == ILArchMage.ICE_DEMON) {
                        monster.setTempEffectiveness(Element.FIRE, ElementalEffectiveness.WEAK, SkillFactory.getSkill(ILArchMage.ICE_DEMON).getEffect(player.getSkillLevel(SkillFactory.getSkill(ILArchMage.ICE_DEMON))).getDuration() * 1000);
                    } else if (attack.skill == Outlaw.HOMING_BEACON || attack.skill == Corsair.BULLSEYE) {
                        MapleStatEffect beacon = SkillFactory.getSkill(attack.skill).getEffect(player.getSkillLevel(attack.skill));
                        beacon.applyBeaconBuff(player, monster.getObjectId());
                        player.setBeaconMob(monster.getObjectId());
                        monster.setBeacon(true);
                    } else if (attack.skill == Outlaw.FLAME_THROWER) {
                        if (!monster.isBoss()) {
                            Skill type = SkillFactory.getSkill(Outlaw.FLAME_THROWER);
                            if (player.getSkillLevel(type) > 0) {
                                MapleStatEffect DoT = type.getEffect(player.getSkillLevel(type));
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), type, null, false);
                                monster.applyStatus(player, monsterStatusEffect, true, DoT.getDuration(), false);
                            }
                        }
                    }

                    if (player.isAran()) {
                        if (player.getBuffedValue(MapleBuffStat.WK_CHARGE) != null) {
                            Skill snowCharge = SkillFactory.getSkill(Aran.SNOW_CHARGE);
                            if (totDamageToOneMonster > 0) {
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, snowCharge.getEffect(player.getSkillLevel(snowCharge)).getX()), snowCharge, null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, snowCharge.getEffect(player.getSkillLevel(snowCharge)).getY() * 1000);
                            }
                        }
                    }
                    if (player.getBuffedValue(MapleBuffStat.HAMSTRING) != null) {
                        Skill hamstring = SkillFactory.getSkill(Bowmaster.HAMSTRING);
                        if (hamstring.getEffect(player.getSkillLevel(hamstring)).makeChanceResult()) {
                            MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, hamstring.getEffect(player.getSkillLevel(hamstring)).getX()), hamstring, null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, hamstring.getEffect(player.getSkillLevel(hamstring)).getY() * 1000);
                        }
                    }
                    if (player.getBuffedValue(MapleBuffStat.SLOW) != null) {
                        Skill slow = SkillFactory.getSkill(Evan.SLOW);
                        if (slow.getEffect(player.getSkillLevel(slow)).makeChanceResult()) {
                            MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, slow.getEffect(player.getSkillLevel(slow)).getX()), slow, null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, slow.getEffect(player.getSkillLevel(slow)).getY() * 60 * 1000);
                        }
                    }
                    if (player.getBuffedValue(MapleBuffStat.BLIND) != null) {
                        Skill blind = SkillFactory.getSkill(Marksman.BLIND);
                        if (blind.getEffect(player.getSkillLevel(blind)).makeChanceResult()) {
                            MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.ACC, blind.getEffect(player.getSkillLevel(blind)).getX()), blind, null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, blind.getEffect(player.getSkillLevel(blind)).getY() * 1000);
                        }
                    }
                    if (job == 121 || job == 122) {
                        for (int charge = 1211005; charge < 1211007; charge++) {
                            Skill chargeSkill = SkillFactory.getSkill(charge);
                            if (player.isBuffFrom(MapleBuffStat.WK_CHARGE, chargeSkill)) {
                                if (totDamageToOneMonster > 0) {
                                    if (charge == WhiteKnight.BW_ICE_CHARGE || charge == WhiteKnight.SWORD_ICE_CHARGE) {
                                        monster.setTempEffectiveness(Element.ICE, ElementalEffectiveness.WEAK, chargeSkill.getEffect(player.getSkillLevel(chargeSkill)).getY() * 1000);
                                        break;
                                    }
                                    if (charge == WhiteKnight.BW_FIRE_CHARGE || charge == WhiteKnight.SWORD_FIRE_CHARGE) {
                                        monster.setTempEffectiveness(Element.FIRE, ElementalEffectiveness.WEAK, chargeSkill.getEffect(player.getSkillLevel(chargeSkill)).getY() * 1000);
                                        break;
                                    }
                                }
                            }
                        }
                        if (job == 122) {
                            for (int charge = 1221003; charge < 1221004; charge++) {
                                Skill chargeSkill = SkillFactory.getSkill(charge);
                                if (player.isBuffFrom(MapleBuffStat.WK_CHARGE, chargeSkill)) {
                                    if (totDamageToOneMonster > 0) {
                                        monster.setTempEffectiveness(Element.HOLY, ElementalEffectiveness.WEAK, chargeSkill.getEffect(player.getSkillLevel(chargeSkill)).getY() * 1000);
                                        break;
                                    }
                                }
                            }
                        }
                    } else if (player.getBuffedValue(MapleBuffStat.COMBO_DRAIN) != null) {
                        Skill skill;
                        if (player.getBuffedValue(MapleBuffStat.COMBO_DRAIN) != null) {
                            skill = SkillFactory.getSkill(21100005);
                            player.addHP(((totDamage * skill.getEffect(player.getSkillLevel(skill)).getX()) / 100));
                        }
                    } else if (job == 412 || job == 422 || job == 1411) {
                        Skill type = SkillFactory.getSkill(player.getJob().getId() == 412 ? 4120005 : (player.getJob().getId() == 1411 ? 14110004 : 4220005));
                        if (player.getSkillLevel(type) > 0) {
                            MapleStatEffect venomEffect = type.getEffect(player.getSkillLevel(type));
                            for (int i = 0; i < attackCount; i++) {
                                if (venomEffect.makeChanceResult()) {
                                    if (monster.getVenomMulti() < 3) {
                                        monster.setVenomMulti((monster.getVenomMulti() + 1));
                                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), type, null, false);
                                        monster.applyStatus(player, monsterStatusEffect, false, venomEffect.getDuration(), true);
                                    }
                                }
                            }
                        }
                    } else if (job >= 311 && job <= 322) {
                        if (!monster.isBoss()) {
                            Skill mortalBlow;
                            if (job == 311 || job == 312) {
                                mortalBlow = SkillFactory.getSkill(Ranger.MORTAL_BLOW);
                            } else {
                                mortalBlow = SkillFactory.getSkill(Sniper.MORTAL_BLOW);
                            }

                            int skillLevel = player.getSkillLevel(mortalBlow);
                            if (skillLevel > 0) {
                                MapleStatEffect mortal = mortalBlow.getEffect(skillLevel);
                                if (monster.getHp() <= (monster.getStats().getHp() * mortal.getX()) / 100) {
                                    if (Randomizer.rand(1, 100) <= mortal.getY()) {
                                        map.damageMonster(player, monster, Integer.MAX_VALUE);  // thanks Conrad for noticing reduced EXP gain from skill kill
                                    }
                                }
                            }
                        }
                    }
                    if (attack.skill != 0) {
                        if (attackEffect.getFixDamage() != -1) {
                            if (totDamageToOneMonster > attackEffect.getFixDamage() && totDamageToOneMonster != 0) {
                                AutobanFactory.FIX_DAMAGE.autoban(player, attack.skill + " - " + totDamageToOneMonster + " damage");
                            }

                            int threeSnailsId = player.getJobType() * 10000000 + 1000;
                            if (attack.skill == threeSnailsId) {
                                if (YamlConfig.config.server.USE_ULTRA_THREE_SNAILS) {
                                    int skillLv = player.getSkillLevel(threeSnailsId);

                                    if (skillLv > 0) {
                                        AbstractPlayerInteraction api = player.getAbstractPlayerInteraction();

                                        int shellId;
                                        switch (skillLv) {
                                            case 1:
                                                shellId = 4000019;
                                                break;

                                            case 2:
                                                shellId = 4000000;
                                                break;

                                            default:
                                                shellId = 4000016;
                                        }

                                        if (api.haveItem(shellId, 1)) {
                                            api.gainItem(shellId, (short) -1, false);
                                            totDamageToOneMonster *= player.getLevel();
                                        } else {
                                            //player.dropMessage(5, "You have ran out of shells to activate the hidden power of Three Snails.");
                                        }
                                    } else {
                                        totDamageToOneMonster = 0;
                                    }
                                }
                            }
                        }
                    }
                    if (totDamageToOneMonster > 0 && attackEffect != null) {
                        Map<MonsterStatus, Integer> attackEffectStati = attackEffect.getMonsterStati();
                        if (!attackEffectStati.isEmpty()) {
                            if (attackEffect.makeChanceResult()) {
                                monster.applyStatus(player, new MonsterStatusEffect(attackEffectStati, theSkill, null, false), attackEffect.isPoison(), attackEffect.getDuration());
                            }
                        }
                    }
                    if (attack.skill == Paladin.HEAVENS_HAMMER) {
                        if (!monster.isBoss()) {
                            int monsterHp = monster.getHp() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) monster.getHp();
                            damageMonsterWithSkill(player, map, monster, (int) monsterHp - 1, attack.skill, 1777);
                        } else {
                            int HHDmg = (int) Math.ceil(player.calculateMaxBaseDamage(player.getTotalWatk()) * (SkillFactory.getSkill(Paladin.HEAVENS_HAMMER).getEffect(player.getSkillLevel(SkillFactory.getSkill(Paladin.HEAVENS_HAMMER))).getDamage() / 100.0));
                            damageMonsterWithSkill(player, map, monster, (int) (Math.floor(Math.random() * (HHDmg / 5) + HHDmg * .8)), attack.skill, 1777);
                        }
                    } else if (attack.skill == Aran.COMBO_TEMPEST) {
                        if (!monster.isBoss()) {
                            int monsterHp = monster.getHp() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) monster.getHp();
                            damageMonsterWithSkill(player, map, monster, (int) monsterHp, attack.skill, 0);
                            monster.setTempestFreeze(true);
                        } else {
                            int TmpDmg = (int) Math.ceil(player.calculateMaxBaseDamage(player.getTotalWatk()) * (SkillFactory.getSkill(Aran.COMBO_TEMPEST).getEffect(player.getSkillLevel(SkillFactory.getSkill(Aran.COMBO_TEMPEST))).getDamage() / 100.0));
                            damageMonsterWithSkill(player, map, monster, (int) (Math.floor(Math.random() * (TmpDmg / 5) + TmpDmg * .8)), attack.skill, 0);
                        }
                    } else {
                        if (attack.skill == Aran.BODY_PRESSURE) {
                            map.broadcastMessage(MaplePacketCreator.damageMonster(monster.getObjectId(), totDamageToOneMonster));
                        }

                        map.damageMonster(player, monster, totDamageToOneMonster);
                    }
                    if (monster.isBuffed(MonsterStatus.WEAPON_REFLECT) && !attack.magic) {
                        List<Pair<Integer, Integer>> mobSkills = monster.getSkills();

                        for (Pair<Integer, Integer> ms : mobSkills) {
                            if (ms.left == 145) {
                                MobSkill toUse = MobSkillFactory.getMobSkill(ms.left, ms.right);
                                player.addHP(-toUse.getX());
                                map.broadcastMessage(player, MaplePacketCreator.damagePlayer(0, monster.getId(), player.getId(), toUse.getX(), 0, 0, false, 0, true, monster.getObjectId(), 0, 0), true);
                            }
                        }
                    }
                    if (monster.isBuffed(MonsterStatus.MAGIC_REFLECT) && attack.magic) {
                        List<Pair<Integer, Integer>> mobSkills = monster.getSkills();

                        for (Pair<Integer, Integer> ms : mobSkills) {
                            if (ms.left == 145) {
                                MobSkill toUse = MobSkillFactory.getMobSkill(ms.left, ms.right);
                                player.addHP(-toUse.getY());
                                map.broadcastMessage(player, MaplePacketCreator.damagePlayer(0, monster.getId(), player.getId(), toUse.getY(), 0, 0, false, 0, true, monster.getObjectId(), 0, 0), true);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void damageMonsterWithSkill(final MapleCharacter attacker, final MapleMap map, final MapleMonster monster, final int damage, int skillid, int fixedTime) {
        int animationTime;

        if (fixedTime == 0) animationTime = SkillFactory.getSkill(skillid).getAnimationTime();
        else animationTime = fixedTime;

        if (animationTime > 0) { // be sure to only use LIMITED ATTACKS with animation time here
            TimerManager.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    map.broadcastMessage(MaplePacketCreator.damageMonster(monster.getObjectId(), damage), monster.getPosition());
                    map.damageMonster(attacker, monster, damage);
                }
            }, animationTime);
        } else {
            map.broadcastMessage(MaplePacketCreator.damageMonster(monster.getObjectId(), damage), monster.getPosition());
            map.damageMonster(attacker, monster, damage);
        }
    }

    protected AttackInfo parseDamage(LittleEndianAccessor lea, MapleCharacter chr, boolean ranged, boolean magic) {
        //2C 00 00 01 91 A1 12 00 A5 57 62 FC E2 75 99 10 00 47 80 01 04 01 C6 CC 02 DD FF 5F 00
        AttackInfo ret = new AttackInfo();
        lea.readByte();
        ret.numAttackedAndDamage = lea.readByte();
        ret.numAttacked = (ret.numAttackedAndDamage >>> 4) & 0xF;
        ret.numDamage = ret.numAttackedAndDamage & 0xF;
        ret.allDamage = new HashMap<>();
        ret.skill = lea.readInt();
        ret.ranged = ranged;
        ret.magic = magic;

        if (ret.skill > 0) {
            ret.skilllevel = chr.getSkillLevel(ret.skill);
            if (ret.skilllevel == 0 && GameConstants.isPqSkillMap(chr.getMapId()) && GameConstants.isPqSkill(ret.skill))
                ret.skilllevel = 1;
        }

        if (ret.skill == Evan.ICE_BREATH || ret.skill == Evan.FIRE_BREATH || ret.skill == FPArchMage.BIG_BANG || ret.skill == ILArchMage.BIG_BANG || ret.skill == Bishop.BIG_BANG || ret.skill == Gunslinger.GRENADE || ret.skill == Brawler.CORKSCREW_BLOW || ret.skill == ThunderBreaker.CORKSCREW_BLOW || ret.skill == NightWalker.POISON_BOMB) {
            ret.charge = lea.readInt();
        } else {
            ret.charge = 0;
        }

        lea.skip(8);
        ret.display = lea.readByte();
        ret.direction = lea.readByte();
        ret.stance = lea.readByte();
        if (ret.skill == ChiefBandit.MESO_EXPLOSION) {
            if (ret.numAttackedAndDamage == 0) {
                lea.skip(10);
                int bullets = lea.readByte();
                for (int j = 0; j < bullets; j++) {
                    int mesoid = lea.readInt();
                    lea.skip(1);
                    ret.allDamage.put(Integer.valueOf(mesoid), null);
                }
                return ret;
            } else {
                lea.skip(6);
            }
            List<Integer> damageList = new ArrayList<>();
            List<Integer> mesoList = new ArrayList<>();
            for (int i = 0; i < ret.numAttacked + 1; i++) {
                int oid = lea.readInt();
                if (i < ret.numAttacked) {
                    lea.skip(12);
                    int bullets = lea.readByte();
                    List<Integer> allDamageNumbers = new ArrayList<>();
                    for (int j = 0; j < bullets; j++) {
                        int damage = lea.readInt();
                        damageList.add(damage);
                        allDamageNumbers.add(Integer.valueOf(damage));
                    }
                    ret.allDamage.put(Integer.valueOf(oid), allDamageNumbers);
                    lea.skip(4);
                } else {
                    int bullets = lea.readByte();
                    for (int j = 0; j < bullets; j++) {
                        int mesoid = lea.readInt();
                        lea.skip(1);
                        mesoList.add(((MapleMapItem)chr.getMap().getMapObject(mesoid)).getMeso());
                        ret.allDamage.put(Integer.valueOf(mesoid), null);
                    }
                }
            }

            for (int i = 0; i < damageList.size(); i++) {
                int explosionDmg = damageList.get(i);
                int mesosUsed = mesoList.get(i);

                double maxDmg = -1.0;
                Skill me = SkillFactory.getSkill(ChiefBandit.MESO_EXPLOSION);
                int meLvl = chr.getSkillLevel(ChiefBandit.MESO_EXPLOSION);
                if (me != null && meLvl > 0) {
                    if (mesosUsed <= 1000) {
                        maxDmg = 50.0 * me.getEffect(meLvl).getX() * (mesosUsed * 0.82 + 28.0) / 5300.0;
                    } else {
                        maxDmg = 50.0 * me.getEffect(meLvl).getX() * mesosUsed / (mesosUsed + 5250.0);
                    }
                }
                if (explosionDmg > maxDmg * 1.025) {
                    AutobanFactory.DAMAGE_HACK.alert(chr, "DMG: " + explosionDmg + " MaxDMG: " + maxDmg + " SID: " + ret.skill + " Map: " + chr.getMap().getMapName() + " (" + chr.getMapId() + ")");
                }

                // Add a ab point if its over 5% more than what we calculated.
                if (explosionDmg > maxDmg * 1.05) {
                    AutobanFactory.DAMAGE_HACK.addPoint(chr.getAutobanManager(), "DMG: " + explosionDmg + " MaxDMG: " + maxDmg + " SID: " + " Map: " + chr.getMap().getMapName() + " (" + chr.getMapId() + ")");
                }
            }
            return ret;
        }
        if (ranged) {
            lea.readByte();
            ret.speed = lea.readByte();
            lea.readByte();
            ret.rangedirection = lea.readByte();
            lea.skip(7);
            if (ret.skill == Bowmaster.HURRICANE || ret.skill == Marksman.PIERCING_ARROW || ret.skill == Corsair.RAPID_FIRE || ret.skill == WindArcher.HURRICANE) {
                lea.skip(4);
            }
        } else {
            lea.readByte();
            ret.speed = lea.readByte();
            lea.skip(4);
        }

        // Find the base damage to base futher calculations on.
        // Several skills have their own formula in this section.
        double calcDmgMax;
        if (magic && ret.skill != 0) {
            Equip weapon = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            Map<String, Integer> weaponStats = MapleItemInformationProvider.getInstance().getEquipStats(weapon.getItemId());
            Skill skill = SkillFactory.getSkill(ret.skill);
            double elem = 1.5; // assume the max in case skill is null for some reason (this is hardcoded for items such as ele staff 5+ and VL)

            if (skill != null) {
                if (skill.getElement() == Element.NEUTRAL)
                    elem = 1.0;
                else if (weaponStats != null)
                    elem = getElementalMultiplier(weaponStats, skill.getElement()); // elemental damage multiplier
            }

            double tma = chr.getTotalMagic();
            double intStat = chr.getTotalInt();
            calcDmgMax = (((tma * tma / 1000.0) + tma) / 30.0 + intStat / 200.0) * elem;
        } else if (ret.skill == 4001344 || ret.skill == NightWalker.LUCKY_SEVEN || ret.skill == NightLord.TRIPLE_THROW) {
            calcDmgMax = chr.getTotalLuk() * 5.0 * chr.getTotalWatk() / 100.0;
        } else if (ret.skill == DragonKnight.DRAGON_ROAR) {
            calcDmgMax = (chr.getTotalStr() * 4.0 + chr.getTotalDex()) * chr.getTotalWatk() / 100.0;
        } else if (ret.skill == NightLord.VENOMOUS_STAR || ret.skill == Shadower.VENOMOUS_STAB) {
            calcDmgMax = (18.5 * (chr.getTotalStr() + chr.getTotalLuk()) + chr.getTotalDex() * 2.0) / 100.0; // pretty sure DoT doesnt get here for venom
        } else if (ret.skill == Crossbowman.POWER_KNOCKBACK || ret.skill == Hunter.POWER_KNOCKBACK) {
            calcDmgMax = (chr.getTotalDex() * 3.4 + chr.getTotalStr()) * chr.getTotalWatk() / 150.0;
        } else if (ret.skill == Shadower.ASSASSINATE) { // for level 30 = 1.3x normal. formula might be 1+skillvl/100
            calcDmgMax = 1.3 * chr.calculateMaxBaseDamage(chr.getTotalWatk()) * chr.getDarkSightCharge();
        } else {
            calcDmgMax = chr.calculateMaxBaseDamage(chr.getTotalWatk()); // TODO: claw punching
        }

        double critDamage = 0;
        boolean canCrit = false;
        if (chr.getBuffEffect(MapleBuffStat.SHARP_EYES) != null) {
            canCrit = true;
            if (magic) // magic seems to be a straight 1.4x multiplier
                calcDmgMax *= chr.getBuffEffect(MapleBuffStat.SHARP_EYES).getY() / 100.0;
            else if (ret.skill != Shadower.ASSASSINATE)
                critDamage += chr.getBuffEffect(MapleBuffStat.SHARP_EYES).getY() / 100.0;
        }
        int critSkill = -1;
        if (chr.getJob().isA(MapleJob.ASSASSIN)) {
            critSkill = Assassin.CRITICAL_THROW;
        } else if (chr.getJob().isA(MapleJob.NIGHTWALKER2)) {
            critSkill = NightWalker.CRITICAL_THROW;
        } else if (chr.getJob().isA(MapleJob.BOWMAN)) {
            critSkill = Archer.CRITICAL_SHOT;
        } else if (chr.getJob().isA(MapleJob.WINDARCHER1)) {
            critSkill = WindArcher.CRITICAL_SHOT;
        } else if (chr.getJob().isA(MapleJob.THUNDERBREAKER3)) {
            critSkill = ThunderBreaker.CRITICAL_PUNCH;
        } else if (chr.getJob().isA(MapleJob.ARAN3)) {
            critSkill = Aran.COMBO_CRITICAL;
        }
        if (critSkill > 0) {
            canCrit = true;
            int critLvl = chr.getSkillLevel(critSkill);
            if (critLvl > 0) {
                if (critSkill == Aran.COMBO_CRITICAL) {
                    MapleStatEffect eff = chr.getBuffEffect(MapleBuffStat.ARAN_COMBO);
                    if (eff != null)
                        critDamage += (100.0 + SkillFactory.getSkill(critSkill).getEffect(critLvl).getDamage() * eff.getX()) / 100.0;
                } else
                    critDamage += (SkillFactory.getSkill(critSkill).getEffect(critLvl).getDamage() - 100.0) / 100.0;
            }
        }

        if (ret.skill != 0) {
            Skill skill = SkillFactory.getSkill(ret.skill);
            MapleStatEffect effect = skill.getEffect(ret.skilllevel);

            if (ret.skill == WhiteKnight.CHARGE_BLOW && chr.getSkillLevel(Paladin.ADVANCED_CHARGE) > 0) {
                skill = SkillFactory.getSkill(Paladin.ADVANCED_CHARGE);
                effect = skill.getEffect(chr.getSkillLevel(Paladin.ADVANCED_CHARGE));
            }

            if (magic) {
                // Since the skill is magic based, use the magic formula
                if (chr.getJob() == MapleJob.IL_ARCHMAGE || chr.getJob() == MapleJob.IL_MAGE) {
                    int skillLvl = chr.getSkillLevel(ILMage.ELEMENT_AMPLIFICATION);
                    if (skillLvl > 0)
                        calcDmgMax = calcDmgMax * SkillFactory.getSkill(ILMage.ELEMENT_AMPLIFICATION).getEffect(skillLvl).getY() / 100.0;
                } else if (chr.getJob() == MapleJob.FP_ARCHMAGE || chr.getJob() == MapleJob.FP_MAGE) {
                    int skillLvl = chr.getSkillLevel(FPMage.ELEMENT_AMPLIFICATION);
                    if (skillLvl > 0)
                        calcDmgMax = calcDmgMax * SkillFactory.getSkill(FPMage.ELEMENT_AMPLIFICATION).getEffect(skillLvl).getY() / 100.0;
                } else if (chr.getJob() == MapleJob.BLAZEWIZARD3 || chr.getJob() == MapleJob.BLAZEWIZARD4) {
                    int skillLvl = chr.getSkillLevel(BlazeWizard.ELEMENT_AMPLIFICATION);
                    if (skillLvl > 0)
                        calcDmgMax = calcDmgMax * SkillFactory.getSkill(BlazeWizard.ELEMENT_AMPLIFICATION).getEffect(skillLvl).getY() / 100.0;
                } else if (chr.getJob() == MapleJob.EVAN7 || chr.getJob() == MapleJob.EVAN8 || chr.getJob() == MapleJob.EVAN9 || chr.getJob() == MapleJob.EVAN10) {
                    int skillLvl = chr.getSkillLevel(Evan.MAGIC_AMPLIFICATION);
                    if (skillLvl > 0)
                        calcDmgMax = calcDmgMax * SkillFactory.getSkill(Evan.MAGIC_AMPLIFICATION).getEffect(skillLvl).getY() / 100.0;
                }

                calcDmgMax *= effect.getMatk();
                if (ret.skill == Cleric.HEAL) {
                    double targetMulti = 1.5 + 5.0 / (1 + ret.numAttacked); // TODO: check if ret.numberAttacked includes party members (or needs to include them)
                    calcDmgMax = (chr.getTotalInt() * 1.2 + chr.getTotalLuk()) * chr.getTotalMagic() / 1000 * targetMulti;
                    calcDmgMax *= effect.getHp() / 100.0 * (canCrit ? chr.getBuffEffect(MapleBuffStat.SHARP_EYES).getY() / 100.0 : 1.0);

                    ret.speed = 7;
                }
            } else if (ret.skill == Hunter.ARROW_BOMB) {
                double seMult = 1.0;
                if (chr.getBuffEffect(MapleBuffStat.SHARP_EYES) != null) {
                    seMult = chr.getBuffEffect(MapleBuffStat.SHARP_EYES).getY() / 100.0;
                    critDamage -= seMult;
                }
                calcDmgMax *= 2 * effect.getX() / 100.0 * critDamage * seMult;
            } else if (ret.skill == Hermit.SHADOW_MESO) {
                // Shadow Meso also has its own formula
                calcDmgMax = (800 + critDamage * 100) * 10; // hard coding it to the max meso it uses at level 30. (moneycon=570, (340+800)/2=570 )
                calcDmgMax = (int) Math.floor(calcDmgMax * 1.5);
            } else if (ret.skill == Outlaw.FLAME_THROWER || ret.skill == Outlaw.ICE_SPLITTER) {
                double elemDmg = chr.getSkillLevel(Corsair.ELEMENTAL_BOOST) > 0 ?
                        SkillFactory.getSkill(Corsair.ELEMENTAL_BOOST).getEffect(chr.getSkillLevel(Corsair.ELEMENTAL_BOOST)).getDamage() : 0.0;
                if (skill.getElement() == Element.FIRE && chr.countItem(2331000) > 0 ||
                        skill.getElement() == Element.ICE && chr.countItem(2332000) > 0) {
                    calcDmgMax = Math.floor(calcDmgMax * (critDamage + (effect.getDamage() + elemDmg) / 100.0)) * 1.15; // capsules have some weird formula, this is close but I don't think it's exact
                } else {
                    calcDmgMax *= (critDamage + (effect.getDamage() + elemDmg) / 100.0) / 2.0; // no capsule on on these skills = 50% damage
                }
            } else {
                // Normal damage formula for skills
                calcDmgMax *= (effect.getDamage() / 100.0) + critDamage;
            }
        } else {
            calcDmgMax *= 1 + critDamage;
        }

        Integer comboBuff = chr.getBuffedValue(MapleBuffStat.COMBO);
        if (comboBuff != null && comboBuff > 1) {
            comboBuff -= 1;
            int oid = chr.isCygnus() ? DawnWarrior.COMBO : Crusader.COMBO;
            int advcomboid = chr.isCygnus() ? DawnWarrior.ADVANCED_COMBO : Hero.ADVANCED_COMBO;

            if (comboBuff >= 6) {
                // Advanced Combo
                MapleStatEffect ceffect = SkillFactory.getSkill(advcomboid).getEffect(chr.getSkillLevel(advcomboid));
                calcDmgMax *= (ceffect.getDamage() + 20.0) / 100 + (comboBuff - 5) * 0.04;
            } else {
                // Normal Combo
                int skillLv = chr.getSkillLevel(oid);
                if (skillLv <= 0 || chr.isGM()) skillLv = SkillFactory.getSkill(oid).getMaxLevel();

                if (skillLv > 0) {
                    MapleStatEffect ceffect = SkillFactory.getSkill(oid).getEffect(skillLv);
                    calcDmgMax *= (ceffect.getDamage() + comboBuff * skillLv * 0.8) / 100.0;
                }
            }

            if (GameConstants.isFinisherSkill(ret.skill)) {
                // Finisher skills do more damage based on how many orbs the player has.
                if (comboBuff == 2)
                    calcDmgMax *= 1.2;
                else if (comboBuff == 3)
                    calcDmgMax *= 1.54;
                else if (comboBuff == 4)
                    calcDmgMax *= 2.0;
                else if (comboBuff >= 5)
                    calcDmgMax *= 2.5;
            }
        }

        // nightwalker vanish multiplier
        if (chr.getBuffEffect(MapleBuffStat.DARKSIGHT) != null && chr.getSkillLevel(NightWalker.VANISH) > 0) {
            calcDmgMax *= SkillFactory.getSkill(NightWalker.VANISH).getEffect(chr.getSkillLevel(NightWalker.VANISH)).getDamage() / 100.0;
        }
        // wind archer wind walk multiplier
        if (chr.getBuffEffect(MapleBuffStat.WIND_WALK) != null && chr.getSkillLevel(WindArcher.WIND_WALK) > 0) {
            calcDmgMax *= SkillFactory.getSkill(WindArcher.WIND_WALK).getEffect(chr.getSkillLevel(WindArcher.WIND_WALK)).getDamage() / 100.0;
        }

        if (chr.hasBerserk()) {
            calcDmgMax *= (100 + SkillFactory.getSkill(DarkKnight.BERSERK).getEffect(chr.getSkillLevel(DarkKnight.BERSERK)).getDamage()) / 100.0;
        }

        if (chr.getMapId() >= 914000000 && chr.getMapId() <= 914000500) {
            calcDmgMax += 80000; // Aran Tutorial.
        }

        boolean shadowPartner = false;
        if (chr.getBuffEffect(MapleBuffStat.SHADOWPARTNER) != null) {
            shadowPartner = true;
        }

        int wkChargeId = -1;
        int wkChargeLevel = -1;
        // get the charge multiplier
        if (chr.getBuffEffect(MapleBuffStat.WK_CHARGE) != null) {
            wkChargeId = chr.getBuffSource(MapleBuffStat.WK_CHARGE);
            wkChargeLevel = chr.getSkillLevel(wkChargeId);
            double chargeMult = SkillFactory.getSkill(wkChargeId).getEffect(wkChargeLevel).getDamage() / 100.0;
            calcDmgMax *= chargeMult;
        }
        for (int i = 0; i < ret.numAttacked; i++) {
            double monsterSpecificDmgMult = 1.0; // modified damage from the monster itself
            double maxMobDmg = calcDmgMax;
            int oid = lea.readInt();
            lea.skip(14);
            List<Integer> allDamageNumbers = new ArrayList<>();
            MapleMonster monster = chr.getMap().getMonsterByOid(oid);

            if (ret.skill == Marksman.PIERCING_ARROW) {
                maxMobDmg *= Math.pow(1.2, i);
            }

            if (oid == chr.getBeaconMob()) {
                MapleStatEffect beacon = chr.getBuffEffect(MapleBuffStat.HOMING_BEACON);
                if (monster != null && beacon != null) {
                    maxMobDmg *= (100.0 + beacon.getX()) / 100.0;
                }
            }

            if (monster != null && !monster.isBoss() && monster.getStati(MonsterStatus.STUN) != null && chr.getSkillLevel(Marauder.STUN_MASTERY) > 0) {
                Skill skill = SkillFactory.getSkill(ret.skill);
                int skillDmg = skill != null ? skill.getEffect(ret.skilllevel).getDamage() : 100;
                int stunDmg = SkillFactory.getSkill(Marauder.STUN_MASTERY).getEffect(chr.getSkillLevel(Marauder.STUN_MASTERY)).getDamage();
                maxMobDmg /= (skillDmg / 100.0) + critDamage;
                maxMobDmg *= critDamage + (100.0 + stunDmg + skillDmg) / 100.0;
            }

            if (wkChargeId > 0) { // Charge, so now we need to check elemental effectiveness against the monster
                if (monster != null) {
                    if (wkChargeId == WhiteKnight.BW_FIRE_CHARGE || wkChargeId == WhiteKnight.SWORD_FIRE_CHARGE) {
                        if (monster.getStats().getEffectiveness(Element.FIRE) == ElementalEffectiveness.WEAK) {
                            monsterSpecificDmgMult *= 1.05 + wkChargeLevel * 0.015;
                        } else if (monster.getStats().getEffectiveness(Element.FIRE) == ElementalEffectiveness.STRONG) {
                            monsterSpecificDmgMult *= 0.95 - wkChargeLevel * 0.015;
                        }
                    } else if (wkChargeId == WhiteKnight.BW_ICE_CHARGE || wkChargeId == WhiteKnight.SWORD_ICE_CHARGE) {
                        if (monster.getStats().getEffectiveness(Element.ICE) == ElementalEffectiveness.WEAK) {
                            monsterSpecificDmgMult *= 1.05 + wkChargeLevel * 0.015;
                        } else if (monster.getStats().getEffectiveness(Element.ICE) == ElementalEffectiveness.STRONG) {
                            monsterSpecificDmgMult *= 0.95 - wkChargeLevel * 0.015;
                        }
                    } else if (wkChargeId == WhiteKnight.BW_LIT_CHARGE || wkChargeId == WhiteKnight.SWORD_LIT_CHARGE) {
                        if (monster.getStats().getEffectiveness(Element.LIGHTING) == ElementalEffectiveness.WEAK) {
                            monsterSpecificDmgMult *= 1.05 + wkChargeLevel * 0.015;
                        } else if (monster.getStats().getEffectiveness(Element.LIGHTING) == ElementalEffectiveness.STRONG) {
                            monsterSpecificDmgMult *= 0.95 - wkChargeLevel * 0.015;
                        }
                    } else if (wkChargeId == Paladin.BW_HOLY_CHARGE || wkChargeId == Paladin.SWORD_HOLY_CHARGE) {
                        if (monster.getStats().getEffectiveness(Element.HOLY) == ElementalEffectiveness.WEAK) {
                            monsterSpecificDmgMult *= 1.20 + wkChargeLevel * 0.015;
                        } else if (monster.getStats().getEffectiveness(Element.HOLY) == ElementalEffectiveness.STRONG) {
                            monsterSpecificDmgMult *= 0.8 - wkChargeLevel * 0.015;
                        }
                    } else if (wkChargeId == Aran.SNOW_CHARGE) {
                        if (monster.getStats().getEffectiveness(Element.ICE) == ElementalEffectiveness.WEAK) {
                            monsterSpecificDmgMult *= 1.20 + wkChargeLevel * 0.015;
                        } else if (monster.getStats().getEffectiveness(Element.ICE) == ElementalEffectiveness.STRONG) {
                            monsterSpecificDmgMult *= 0.8 - wkChargeLevel * 0.015;
                        }
                    }
                } else {
                    // Since we already know the skill has an elemental attribute, but we dont know if the monster is weak or not, lets
                    // take the safe approach and just assume they are weak.
                    monsterSpecificDmgMult *= 1.5;
                }
            }

            if (ret.skill != 0) {
                Skill skill = SkillFactory.getSkill(ret.skill);
                if (skill.getElement() != Element.NEUTRAL && chr.getBuffedValue(MapleBuffStat.ELEMENTAL_RESET) == null && chr.getBuffEffect(MapleBuffStat.WK_CHARGE) == null) {
                    // The skill has an element effect, so we need to factor that in.
                    if (monster != null) {
                        ElementalEffectiveness eff = monster.getElementalEffectiveness(skill.getElement());
                        if (eff == ElementalEffectiveness.WEAK) {
                            if (ret.skill == Sniper.BLIZZARD || ret.skill == Ranger.INFERNO)
                                monsterSpecificDmgMult *= (1.1 + chr.getSkillLevel(ret.skill) * 0.005);
                            else
                                monsterSpecificDmgMult *= 1.5;
                        } else if (eff == ElementalEffectiveness.STRONG) {
                            if (ret.skill == Sniper.BLIZZARD || ret.skill == Ranger.INFERNO)
                                monsterSpecificDmgMult *= (0.9 - chr.getSkillLevel(ret.skill) * 0.005);
                            else
                                monsterSpecificDmgMult *= 0.5;
                        }
                    } else {
                        // Since we already know the skill has an elemental attribute, but we dont know if the monster is weak or not, lets
                        // take the safe approach and just assume they are weak.
                        monsterSpecificDmgMult *= 1.5;
                    }
                }
                maxMobDmg = maxMobDmg * monsterSpecificDmgMult;

                int fixed = ret.getAttackEffect(chr, SkillFactory.getSkill(ret.skill)).getFixDamage();
                if (fixed > 0)
                    maxMobDmg = fixed;
                else if (ret.skill == FPWizard.POISON_BREATH || ret.skill == FPMage.POISON_MIST || ret.skill == FPArchMage.FIRE_DEMON || ret.skill == ILArchMage.ICE_DEMON) {
                    if (monster != null) {
                        // Turns out poison is completely server side, so I don't know why I added this. >.<
                        //calcDmgMax = monster.getHp() / (70 - chr.getSkillLevel(skill));
                    }
                } else if (ret.skill == Hermit.SHADOW_WEB) {
                    if (monster != null) {
                        maxMobDmg = monster.getHp() / (50.0 - chr.getSkillLevel(skill));
                    }
                } else if (ret.skill == Paladin.HEAVENS_HAMMER || ret.skill == Aran.COMBO_TEMPEST) {
                    if (monster != null && !monster.isBoss())
                        maxMobDmg = monster.getMobMaxHp() - 1;
                } else if (ret.skill == Hermit.SHADOW_MESO) {
                    if (monster != null) {
                        monster.debuffMob(Hermit.SHADOW_MESO);
                    }
                }
            } else {
                maxMobDmg = maxMobDmg * monsterSpecificDmgMult;
            }

            for (int j = 0; j < ret.numDamage; j++) {
                double hitDmg = maxMobDmg;
                int damage = lea.readInt();

                if (ret.skill == Buccaneer.BARRAGE || ret.skill == ThunderBreaker.BARRAGE) {
                    if (j > 3)
                        hitDmg *= Math.pow(2, (j - 3));
                }
                if (ret.skill == Shadower.ASSASSINATE && ret.numDamage == 1) { // 4th nate hit. Formula is still unknown but this slight over estimation still works well
                    MapleStatEffect nateEffect = SkillFactory.getSkill(Shadower.ASSASSINATE).getEffect(chr.getMasterLevel(Shadower.ASSASSINATE));
                    hitDmg = chr.getDarkSightCharge() * chr.calculateMaxBaseDamage(chr.getTotalWatk()) *
                            ((canCrit ? chr.getBuffEffect(MapleBuffStat.SHARP_EYES).getY() : 0) +
                                    nateEffect.getDamage() + nateEffect.getCriticalDamage() - 100.0) / 100.0;
                }
                if (shadowPartner) {
                    // For shadow partner, the second half of the hits only do 50% damage. So calc that
                    // in for the crit effects.
                    if (j >= ret.numDamage / 2) {
                        hitDmg *= ret.skill == 0 ? 0.8 : 0.5;
                    }
                }

                if (ret.skill == Beginner.BAMBOO_RAIN || ret.skill == Noblesse.BAMBOO_RAIN || ret.skill == Evan.BAMBOO_THRUST || ret.skill == Legend.BAMBOO_THRUST) {
                    hitDmg = 82569000; // 30% of Max HP of strongest Dojo boss
                }

                boolean skipBan = false;
                if (monster != null) {
                    if (monster.isTempestFreeze()) {
                        hitDmg = monster.getMaxHp();
                    }

                    MonsterStatusEffect matkBuff = monster.getStati(MonsterStatus.MAGIC_ATTACK_UP);
                    if (matkBuff != null && matkBuff.getMobSkill() != null && matkBuff.getMobSkill().getSkillId() == 111) { // mob skill 111 buffs magic attack by x but increases damage taken from physical by 1.3x
                        hitDmg *= matkBuff.getMobSkill().getX() / 100.0;
                        skipBan = true;
                    }
                }

                hitDmg = Math.floor(hitDmg); // round it at the very end only
                // Warn if the damage is over 2.5% what we calculated above.
                if (damage > hitDmg * 1.025) {
                    AutobanFactory.DAMAGE_HACK.alert(chr, "DMG: " + damage + " MaxDMG: " + hitDmg + " SID: " + ret.skill + " MobID: " + (monster != null ? monster.getId() : "null") + " Map: " + chr.getMap().getMapName() + " (" + chr.getMapId() + ")");
                }

                // Add a ab point if its over 5% what we calculated.
                if (!skipBan && damage > hitDmg * 1.05) {
                    AutobanFactory.DAMAGE_HACK.addPoint(chr.getAutobanManager(), "DMG: " + damage + " MaxDMG: " + hitDmg + " SID: " + ret.skill + " MobID: " + (monster != null ? monster.getId() : "null") + " Map: " + chr.getMap().getMapName() + " (" + chr.getMapId() + ")");
                }

                if (canCrit && damage > hitDmg) {
                    // If the skill is a crit, inverse the damage to make it show up onutobanManager(), "DMG: "  clients.
                    damage = -Integer.MAX_VALUE + damage - 1;
                }

                allDamageNumbers.add(damage);
            }
            if (ret.skill != Corsair.RAPID_FIRE || ret.skill != Aran.HIDDEN_FULL_DOUBLE || ret.skill != Aran.HIDDEN_FULL_TRIPLE || ret.skill != Aran.HIDDEN_OVER_DOUBLE || ret.skill != Aran.HIDDEN_OVER_TRIPLE) {
                lea.skip(4);
            }
            ret.allDamage.put(Integer.valueOf(oid), allDamageNumbers);
        }
        if (ret.skill == NightWalker.POISON_BOMB) { // Poison Bomb
            lea.skip(4);
            ret.position.setLocation(lea.readShort(), lea.readShort());
        }
        return ret;
    }

    private static int rand(int l, int u) {
        return (int) ((Math.random() * (u - l + 1)) + l);
    }

    private double getElementalMultiplier(Map<String, Integer> stats, Element elem) {
        double mult = 1.0;

        if (elem == Element.FIRE && stats.containsKey("RMAF")) {
            mult = stats.get("RMAF") / 100.0;
        }
        if (elem == Element.POISON && stats.containsKey("RMAS")) {
            mult = stats.get("RMAS") / 100.0;
        }
        if (elem == Element.ICE && stats.containsKey("RMAI")) {
            mult = stats.get("RMAI") / 100.0;
        }
        if (elem == Element.LIGHTING && stats.containsKey("RMAL")) {
            mult = stats.get("RMAL") / 100.0;
        }
        if (elem == Element.HOLY && stats.containsKey("RMAH")) {
            mult = stats.get("RMAH") / 100.0;
        }

        if (mult == 1.0 && stats.containsKey("elemDefault"))
            mult = stats.get("elemDefault") / 100.0;

        return mult;
    }

}
