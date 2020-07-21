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
//By Moogra

importPackage(Packages.server.expeditions);

function start() {
    cm.sendYesNo("Beep... beep... you can make your escape to a safer place through me. Beep... beep... would you like to leave this place?");
}

function action(mode, type, selection) {
    if (mode < 1)
        cm.dispose();
        
    else if (cm.getEventInstance() && !cm.getEventInstance().isEventCleared()) {
        cm.warp(220080000);
        cm.dispose();
    } else {
        if (cm.reachedRewardLimit(MapleExpeditionType.PAPULATUS)) {
            cm.warp(220080000);
            cm.dispose();
        } else if (!cm.getPlayer().getInventory(Packages.client.inventory.MapleInventoryType.ETC).isFull(1)) {
            cm.warp(220080000);
            cm.gainItem(4000038, 25);
            cm.dispose();
        } else {
            cm.sendOk("Please make space in your inventory");
            cm.dispose();
        }
    }
}
