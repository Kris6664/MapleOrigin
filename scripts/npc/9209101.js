/*vote point exchange npc
Exchanges votepoints for white scrolls dragon weapons and reverse weapons.
@@author shadowzzz*/

var status = 0;
var points = [3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 1, 1, 1, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1];
var items = [1022082, 
/*Starts at 1, all the ITCG Equips */          1082223, 1082230, 1032048, 1002675, 1002676, 1072344, 1402045, 1472064, 1422028, 2070016, 1102206, 1092052, 1102145, 1442057, 2070018, 2330005,
/*Starts at 17, all the dragon weapons */      1302086, 1312038, 1322061, 1332075, 1332076, 1372045, 1382059, 1402047, 1412034, 1422038, 1432049, 1442067, 1452059, 1462051, 1472071, 1482024, 1492025,
/*Starts at 34, all the scrolls */             2049100, 2340000, 2049003,
/*Starts at 37, Warrior Empress Weapon*/       2290084, 2290085, 2290010, 2290011, 2290022, 2290023,2290060, 2290061, 2290032, 2290033,
/*Starts at 47, Bowman Empress Weapon*/        2290030, 2290031,
/*Starts at 49, Theif Empress Weapon*/         2290050, 2290051,
/*Starts at 51, Mage Empress Weapon */         2290090, 2290091,
/*Starts at 53, Pirate Empress Weaoon */       2290074, 2290075,
/*Starts at 55, Warrior Empress Gear */        2290136, 2290137, 2290012, 2290013, 2290096,
/*Starts at 60, Bowman Empress Gear*/          2290125, 1102277, 1052316, 1072487, 1082297,
/*Starts at 65, Thief Empress Gear */          1003175, 1102278, 1052317, 1072488, 1082298,
/*Starts at 70, Pirate Empress Gear*/          1003176, 1102279, 1052318, 1072489, 1082299,
/*Starts at 75, Mage Empress Gear*/            1003173, 1102276, 1052315,
/*Starts at 78, VIP Weapoons */                1302147, 1312062, 1322090, 1332120, 1332125, 1372078, 1382099, 1402090, 1412062, 1422063, 1432081, 1442111, 1452106, 1462091, 1472117, 1482079, 1492079,
/*Starts at 95, 60% Scrolls */                 2040914, 2040919, 2044301, 2044401, 2044501, 2044601, 2044701, 2044801, 2044901, 2044201, 2044101, 2044001, 2043001, 2043101, 2043201, 2043801, 2043701, 2043301];
var leaf = [4000313];

function start() {
    cm.sendSimple("Hi! I can exchange #v4000313# for various items to help you in your adventures in Maple Origin! You may obtain these items as rewards for clearing Zakum, Horntail, The Boss, Pink Bean, and Von Leon! What would u like to buy? #b\r\n#L6# Buy 40 Event Trophies for 1 Golden Maple Leaf #b\r\n#L0# Buy some scrolls for 1 Golden Maple Leaf #b\r\n#L1# Buy ITCG Equips for 3 Golden Maple Leaf #b\r\n#L2# Buy VIP Weapons for 10 Golden Maple Leaf#b\r\n#L5# Buy Skill Books for 15 Golden Maple Leaf#b\r\n#L7# Buy Vega's Spells for Golden Maple Leaves");
}

function action (m,t,s) {
    if (m < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        sel = s;
        if (s == 0) {
            var selStr = "#e#kScroll shop:#n #r1#b #e#z4000313##n#k\r\nFun Fact: Mitochondria is not actually the powerhouse of the cell #b";
            var scrolls = [2049100, 2340000, 2049003].concat(items.slice(95, 113));
            for (var i = 0; i < scrolls.length; i++) {
                if (scrolls[i] != 2340000)
                    selStr += "\r\n#L" + (i < 3 ? i + 34 : i + 92) + "##v" + scrolls[i] + "##e#z" + scrolls[i] + (i > 2 ?"# x3#n" : "##n");
            }
            cm.sendSimple(selStr);
        } else if (s == 1) {
            var selStr = "#e#kiTCG shop:#n #r3#b #e#z4000313##n#k\r\nFun Fact: Munz Likes Bunz #b";
            var pageItems = items.slice(1, 17);
            for (var i = 0; i < pageItems.length; i++) {
                if (pageItems[i] != 2070018)
                    selStr += "\r\n#L" + (i + 1) + "##v" + pageItems[i] + "##e#z" + pageItems[i] + "##n";
            }
            cm.sendSimple(selStr);
        } else if (s == 2) {
            var selStr = "#e#kVIP Weapon shop:#n #r10#b #e#z4000313##n#k\r\nFun Fact: For 1m free nx #bCLICK HERE #b";
            var pageItems = items.slice(78, 95);
            for (var i = 0; i < pageItems.length; i++)
                selStr += "\r\n#L" + (i + 78) + "##v" + pageItems[i] + "##e#z" + pageItems[i] + "##n";
            cm.sendSimple(selStr);
        } else if (s == 3) {
            var selStr = "Fun Fact: The original MapleOrigin used to be called ProjectNanp because Jay has fat fingers #b";
            var pageItems = items.slice(115, 130); // TODO: add to items array and adjust slice range
            for (var i = 0; i < pageItems.length; i++)
                selStr += "\r\n#L" + (i + 53) + "##v" + pageItems[i] + "##e#z" + pageItems[i] + "##n";
            cm.sendSimple(selStr);
            // cm.sendSimple("Fun Fact: The original MapleOrigin used to be called ProjectNanp because Jay has fat fingers #b\r\n#L53##v1003172# Lionheart Battle Helm #b\r\n#L54##v1102275# Lionheart Battle Cape #b\r\n#L55##v1052314# Lionheart Battle Mail #b\r\n#L56# #v1072485#Lionheart Battle Boots #b\r\n#L57# #v1082295#Lionheart Battle Bracers #b\r\n#L58##v1003174# Falcon Wing Sentinel Cap #b\r\n#L59##v1102277# Falcon Wing Sentinel Cape #b\r\n#L60# #v1052316#Falcon Wing Sentinel Suit #b\r\n#L61# #v1072487#Falcon Wing Sentinel Boots #b\r\n#L62##v1082297# Falcon Wing Sentinel Gloves #b\r\n#L63##v1003175# Raven Horn Chaser Hat #b\r\n#L64# #v1102278#Raven Horn Chaser Cape #b\r\n#L65# #v1052317#Raven Horn Chaser Armor #b\r\n#L66# #v1072488#Raven Horn Chaser Boots #b\r\n#L67# #v1082298#Raven Horn Chaser Gloves #b\r\n#L68##v1003176# Shark Tooth Skipper Hat #b\r\n#L69##v1102279# Shark tooth Skipper Cape #b\r\n#L70##v1052318# Shark Tooth Skipper Coat #b\r\n#L71##v1072489# Shark Tooth Skipper Boots #b\r\n#L72##v1082299# Shark Tooth Skipper Gloves #b\r\n#L73# #v1003173#Dragon Tail Mage Sallet #b\r\n#L74# #v1102276#Dragon Tail Mage Cape #b\r\n#L75# #v1052315#Dragon Tail Mage Robe #b\r\n#L76# #v1072486#Dragon Tail Mage Shoes #b\r\n#L77##v1082296# Dragon Tail Mage Gloves");
        } else if (s == 4) {
            var selStr = "Fun Fact: The original MapleOrigin used to be called ProjectNanp because Jay has fat fingers #b";
            var pageItems = items.slice(131, 145); // TODO: add to items array and adjust slice range
            for (var i = 0; i < pageItems.length; i++)
                selStr += "\r\n#L" + (i + 53) + "##v" + pageItems[i] + "##e#z" + pageItems[i] + "##n";
            cm.sendSimple(selStr);
            // cm.sendSimple("Fun Fact: For 1m free nx #bCLICK HERE  #b\r\n#L37# #v1302152#Lionheart Cuttlas #b\r\n#L38# #v1312065#LionHeart Champion Axe #b\r\n#L39# #v1322096#Lionheart Battle Hammer #b\r\n#L40# #v1402095#Lionheart Battle Scimitar #b\r\n#L41# #v1412065#Lionheart Battle Axe #b\r\n#L42# #v1422066#Lionheart Blast Maul #b\r\n#L43# #v1432086#Lionheart Fuscina #b\r\n#L44##v1442116# Lionheart Partisan #b\r\n#L45# #v1452111#Falcon Wing Composite Bow #b\r\n#L46# #v1462099#Falcon Wing Heavy Cross Bow #b\r\n#L47##v1332130# Raven Horn Baselard #b\r\n#L48# #v1472122#Raven Horn Metal Fist #b\r\n#L49# #v1372084#Dragon Tail Arc Wand #b\r\n#L50# #v1382104#Dragon Tail War Staff #b\r\n#L51# #v1482084#Shark Tooth Wild Talon #b\r\n#L52# #v1492085#Shark Tooth Sharpshooter #b\r\n");
        }
		else if (s == 5) {
		    var selStr = "#e#kSkill Book shop:#n #r15#b #e#z4000313##n#k\r\nFun Fact: For 1m free nx #bCLICK HERE #b";
            var pageItems = items.slice(37, 61);
            for (var i = 0; i < pageItems.length; i++) {
                if (pageItems[i] != 2290096 && pageItems[i] != 2290125)
                    selStr += "\r\n#L" + (i + 37) + "##v" + pageItems[i] + "##e#z" + pageItems[i] + "##n";
            }
            cm.sendSimple(selStr);
            // cm.sendSimple("Fun Fact: For 1m free nx #bCLICK HERE  #b\r\n#L37# #v2290084#Triple Throw 20 #b\r\n#L38# #v2290085#Triple Throw 30 #b\r\n#L39# #v2290010#Brandish 20 #b\r\n#L40# #v2290011#Brandish 30 #b\r\n#L41# #v2290022#Berserk 20 #b\r\n#L42# #v2290023#Berserk 30 #b\r\n#L43# #v2290060#Hurricane 20 #b\r\n#L44##v2290061#Hurricane 30 #b\r\n#L45# #v2290032#Chain Lightning 20 #b\r\n#L46# #v2290033#Chain Lightning 30 #b\r\n#L47##v2290030#Paralyze 20 #b\r\n#L48# #v2290031#Paralyze 30 #b\r\n#L49# #v2290050#Angel Ray 20 #b\r\n#L50# #v2290051#Angel Ray 30 #b\r\n#L51# #v2290090#Boomerang Step 20 #b\r\n#L52# #v2290091#Boomerang Step 30 #b\r\n#L53# #v2290074#Snipe 20 #b\r\n#L54# #v2290074# Snipe 30 #b\r\n#L55# #v2290136#Combo Tempest 20 #b\r\n#L56# #v2290137#Combo Tempest 30 #b\r\n#L57# #v2290012#Blast 20 #b\r\n#L58# #v2290013#Blast 30 #b\r\n#L59# #v2290096#Maple Warrior 20 #b\r\n#L60# #v2290125#Maple Warrior 30 #b\r\n");
        } else if (s == 6) {
            var selStr = "#kWould you like to trade #r1#k #b#e#z4000313##n#k for #r40#k #b#e#z4000038##n#k?";
            cm.sendYesNo(selStr);
            status++;
        } else if (s == 7) {
            var selStr = "#e#kVega's Scroll shop:#n#b\r\n";

            selStr += "#L" + 5610000 + "##v" + 5610000 + "##e#z" + 5610000 + "##n (#r3 GML#b)";
            selStr += "\r\n#L" + 5610001 + "##v" +5610001 + "##e#z" + 5610001 + "##n (#r2 GML#b)";
            cm.sendSimple(selStr);
        }
    } else if (status == 2) {
        if (sel == 100) {
            if (cm.haveItem(leaf) >= 6) {
                if(!cm.getPlayer().getInventory(Packages.client.inventory.MapleInventoryType.EQUIP).isFull(1)) {
                   // var currentRewardPoints = cm.getPlayer().getRewardPoints();
                    //cm.getPlayer().setRewardPoints(currentRewardPoints - 6);
					cm.gainItem(4000313, -6)
                    cm.gainItem(2340000, 5);
                }
                else{
                    cm.sendOk("Please make sure you have enough space to hold these items!");
                }
            } else {
                cm.sendOk(" You don't have 6 vote points. ");
            }
        } else {
            if (cm.haveItem(4000313) >= 1) {
               if(!cm.getPlayer().getInventory(Packages.client.inventory.MapleInventoryType.EQUIP).isFull(0)) {
                   // var currentRewardPoints = cm.getPlayer().getRewardPoints();
                   // cm.getPlayer().setRewardPoints(currentRewardPoints - points[s]);
				   //cm.gainItem(leaf, - points[s]);
                    if (items[s] == 2049100 || items[s] == 2340000 || items[s] == 2049003) {
                        if(!cm.getPlayer().getInventory(Packages.client.inventory.MapleInventoryType.USE).isFull(0)) {
                            if(cm.haveItem(leaf, 1)) {
                                cm.gainItem(leaf, -1);
                                cm.gainItem(items[s], 1);
                            } else {
                                cm.sendOk("Sorry, you don't have enough leafs!");
                            }
                        } else {
                            cm.sendOk("Please make sure you have at least 1 slots empty in your inventory");
                        }
                    }
                    else if(items[s] == 2044301 || items[s] == 2044401 || items[s] == 2044501 || items[s] == 2044601 || items[s] == 2044701 || items[s] == 2044801 || items[s] == 2044901 || items[s] == 2044201 || items[s] == 2044101 || items[s] == 2044001 || items[s] == 2043001 || items[s] == 2043101 || items[s] == 2043201 || items[s] == 2043801 || items[s] == 2043701 || items[s] == 2043301 || items[s] == 2040914 || items[s] == 2040919){
                        if(!cm.getPlayer().getInventory(Packages.client.inventory.MapleInventoryType.USE).isFull(0)) {
                            if(cm.haveItem(leaf, 1)) {
                                cm.gainItem(leaf, -1);
                                cm.gainItem(items[s], 3);
                            } else {
                                cm.sendOk("Sorry, you don't have enough leafs!");
                            }
                        } else {
                            cm.sendOk("Please make sure you have at least 1 slots empty in your inventory");
                        }
                    }
					else if(items[s] == 2290084 || items[s] == 2290085 || items[s] == 2290010 || items[s] == 2290011 || items[s] == 2290022 || items[s] == 2290023 || items[s] == 2290032 || items[s] == 2290033 || items[s] == 2290030 || items[s] == 2290031 || items[s] == 2290050 || items[s] == 2290051 || items[s] == 2290090 || items[s] == 2290091 || items[s] == 2290074 || items[s] == 2290075 || items[s] == 2290136 || items[s] == 2290137 || items[s] == 2290012 || items[s] == 2290013 || items[s] == 2290096 || items[s] == 2290125){
                        if(!cm.getPlayer().getInventory(Packages.client.inventory.MapleInventoryType.USE).isFull(0)) {
                            if(cm.haveItem(leaf, 15)) {
                                cm.gainItem(leaf, -15);
                                cm.gainItem(items[s], 1);
                            } else {
                                cm.sendOk("Sorry, you don't have enough leafs!");
                            }
                        } else {
                            cm.sendOk("Please make sure you have at least 1 slots empty in your inventory");
                        }
                    }
                    else if(items[s] == 1302147 || items[s] == 1312062 || items[s] == 1322090 || items[s] == 1332120 || items[s] == 1332125 || items[s] == 1372078 || items[s] == 1382099 || items[s] == 1402090 || items[s] == 1412062 || items[s] == 1422063 || items[s] == 1432081 || items[s] == 1442111 || items[s] == 1452106 || items[s] == 1462091 || items[s] == 1472117 || items[s] == 1482079 || items[s] == 1492079){
                        if(cm.haveItem(leaf, 10)) {
                            cm.gainItem(leaf, -10);
                            cm.gainItem(items[s], 1);
                       } else {
                            cm.sendOk("Sorry, you don't have enough leafs!");
                        }
                    }
					else if(items[s] == 1082223 || items[s] == 1082230 || items[s] == 1032048 || items[s] == 1002675 || items[s] == 1002676 || items[s] == 1072344 || items[s] == 1402045 || items[s] == 1472064 || items[s] == 1422028 || items[s] == 2070016 || items[s] == 1102206 || items[s] == 1092052 || items[s] == 1102145 || items[s] == 1442057 || items[s] == 2070018 || items[s] == 1002553 || items[s] == 2330005){
                        if (items[s] == 2330005 && cm.getPlayer().getInventory(Packages.client.inventory.MapleInventoryType.USE).isFull(0)) {
                            cm.sendOk("Please make sure you have at least 1 empty use slots.");
                        } else {
                            if(cm.haveItem(leaf, 3)) {
                                cm.gainItem(leaf, -3);
                                cm.gainItem(items[s], 1);
                            } else{
                                cm.sendOk("Sorry, you don't have enough leafs!");
                            }
                        }
                    }
                    else if (s == 5610000 || s == 5610001) {
                        if (items[s] == 2330005 && cm.getPlayer().getInventory(Packages.client.inventory.MapleInventoryType.CASH).isFull(0)) {
                            cm.sendOk("Please make sure you have at least 1 empty cash slots.");
                        } else {
                            if (s == 5610000) {
                                if(cm.haveItem(leaf, 3)) {
                                    cm.gainItem(leaf, -3);
                                    cm.gainItem(s, 1);
                                } else {
                                    cm.sendOk("Sorry, you don't have enough leafs!");
                                }
                            } else if (s == 5610001) {
                                if(cm.haveItem(leaf, 2)) {
                                  cm.gainItem(leaf, -2);
                                  cm.gainItem(s, 1);
                              } else {
                                  cm.sendOk("Sorry, you don't have enough leafs!");
                              }
                            }
                        }
                    }

					/*else if(items[s] == 2290084 || items[s] == 2290085 || items[s] == 2290010 || items[s] == 2290011 || items[s] == 2290022 || items[s] == 2290023 || items[s] == 2290032 || items[s] == 2290033 || items[s] == 2290030 || items[s] == 2290031 || items[s] == 2290050 || items[s] == 2290051 || items[s] == 2290090 || items[s] == 2290091 || items[s] == 2290074 || items[s] == 2290075 || items[s] == 2290136 || items[s] == 2290137 || items[s] == 2290012 || items[s] == 2290013 || items[s] == 2290096 || items[s] == 2290125){
                        cm.gainItem(leaf, -15);
						cm.gainItem(items[s], 1);
                    }*/
					
                }
                else {
                    cm.sendOk("Please make sure you have at least 1 empty slots in both equip and etc.");
                }
            } else {
                var pts = points[s];
                if (!pts)
                    if (s == 5610000)
                        pts = 3;
                    if (s == 5610001)
                        pts = 2;

                cm.sendOk(" You don't have " + pts + " Golden Maple Leafs. ");
            }
        }
        cm.dispose();
    } else if (status == 3) {
        if (cm.getPlayer().getInventory(Packages.client.inventory.MapleInventoryType.ETC).isFull(1)) {
            cm.sendOk("Please make sure you have at least 1 empty slots in etc.");
        } else {
            cm.gainItem(leaf, -1);
            cm.gainItem(4000038, 40);
            cm.sendOk("Enjoy your trophies!");
        }
        cm.dispose();
    }
}
