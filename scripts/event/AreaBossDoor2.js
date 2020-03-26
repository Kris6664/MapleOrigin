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
/**
-- Odin JavaScript --------------------------------------------------------------------------------
	Door boss Spawner (based on xQuasar's King Clang spawner)
**/

importPackage(Packages.tools);

var timer = 3 * 60 * 60 * 1000; // 3hrs
var randomize = Randomizer.rand(((-0.2 * timer) | 0), ((0.2 * timer) | 0)) // randomize by += 20%

function init() {
    scheduleNew();
}

function scheduleNew() {
    setupTask = em.schedule("start", 0);    //spawns upon server start. Each 3 hours an server event checks if boss exists, if not spawns it instantly.
}

function cancelSchedule() {
    if (setupTask != null)
        setupTask.cancel(true);
}

function start() {
    var bossMobid = 9400609;
    var bossMapid = 677000005;
    var bossMsg = "Andras has appeared!";
    var bossPos = new Packages.java.awt.Point(201, 80);
    
    var map = em.getChannelServer().getMapFactory().getMap(bossMapid);
    if (map.getMonsterById(bossMobid) != null) {
        em.schedule("start", timer + randomize);
        return;
    }
    
    var boss = Packages.server.life.MapleLifeFactory.getMonster(bossMobid);
    map.spawnMonsterOnGroundBelow(boss, bossPos);
    map.broadcastMessage(Packages.tools.MaplePacketCreator.serverNotice(6, bossMsg));
    
    em.schedule("start", timer + randomize);
}

// ---------- FILLER FUNCTIONS ----------

function dispose() {}

function setup(eim, leaderid) {}

function monsterValue(eim, mobid) {return 0;}

function disbandParty(eim, player) {}

function playerDisconnected(eim, player) {}

function playerEntry(eim, player) {}

function monsterKilled(mob, eim) {}

function scheduledTimeout(eim) {}

function afterSetup(eim) {}

function changedLeader(eim, leader) {}

function playerExit(eim, player) {}

function leftParty(eim, player) {}

function clearPQ(eim) {}

function allMonstersDead(eim) {}

function playerUnregistered(eim, player) {}

