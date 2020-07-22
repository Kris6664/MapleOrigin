/*
    This file is part of the HeavenMS MapleStory Server, commands OdinMS-based
    Copyleft (L) 2016 - 2018 RonanLana

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

/*
   @Author: Arthur L - Refactored command content into modules
*/
package client.command.commands.gm0;

import client.MapleCharacter;
import client.command.Command;
import client.MapleClient;
import server.MapleItemInformationProvider;
import server.life.MapleMonsterInformationProvider;
import server.life.MonsterDropEntry;
import tools.MaplePacketCreator;
import tools.Pair;

import java.util.Iterator;
import java.util.List;

public class WhatDropsFromCommand extends Command {
    {
        setDescription("Displays all dropped items from a specific monster");
    }

    @Override
    public void execute(MapleClient c, String[] params) {
        MapleCharacter player = c.getPlayer();
        if (params.length < 1) {
            player.dropMessage(5, "Please do @whatdropsfrom <monster name>");
            return;
        }
        String monsterName = player.getLastCommandMessage();
        String output = "";
        int limit = 10;
        Iterator<Pair<Integer, String>> listIterator = MapleMonsterInformationProvider.getMobsIDsFromName(monsterName).iterator();
        for (int i = 0; i < limit; i++) {
            if(listIterator.hasNext()) {
                Pair<Integer, String> data = listIterator.next();
                int mobId = data.getLeft();
                String mobName = data.getRight();
                List<MonsterDropEntry> drops = MapleMonsterInformationProvider.getInstance().retrieveDrop(mobId);
                if (!drops.isEmpty()) {
                    output += mobName + " drops the following items:\r\n\r\n#e";
                    for (MonsterDropEntry drop : MapleMonsterInformationProvider.getInstance().retrieveDrop(mobId)) {
                        try {
                            String name = MapleItemInformationProvider.getInstance().getName(drop.itemId);
                            if (name == null || name.equals("null") || drop.chance == 0) {
                                continue;
                            }
                            float chance = Math.max(1000000 / drop.chance / (!MapleMonsterInformationProvider.getInstance().isBoss(mobId) ? player.getDropRate() : player.getBossDropRate()), 1);
                            output += "#v" + drop.itemId + "# #z" + drop.itemId + "# (1/" + (int) chance + ")";
                            if (!drop.shouldStack || drop.Minimum != drop.Maximum) {
                                if (drop.Minimum != drop.Maximum)
                                    output += " [" + drop.Minimum + "-" + drop.Maximum + "]";
                                else
                                    output += " [x" + drop.Minimum + "]";

                            }
                            output += "\r\n";
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            continue;
                        }
                    }
                    output += "\r\n";
                } else
                    i--;
            }
        }
        
        c.getAbstractPlayerInteraction().npcTalk(9010000, output);
    }
}
