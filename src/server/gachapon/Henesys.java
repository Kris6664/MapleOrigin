package server.gachapon;

/**
*
* @author Alan (SharpAceX) - gachapon source classes stub
* @author Ronan - parsed MapleSEA loots
* 
* MapleSEA-like loots thanks to AyumiLove, src: https://ayumilovemaple.wordpress.com/maplestory-gachapon-guide/
*/

public class Henesys extends GachaponItems {

	@Override
	public int[] getCommonItems() {
		return new int [] {
                
                        /* Scroll */
                        2040001, 2041002, 2040702, 2043802, 2040402, 2043702, 2044813,

                        /* Useable Drops */
                        2000004, 2000005, 2020012, 2030007,

                        /* Common equipment */
                        1432009, 1302022, 1322021, 1302026, 1442017, 1082147, 1102043, 1322026, 1442016, 1402012, 1322025, 1322027, 1302027,
                        1312012, 1062000, 1332020, 1302028, 1372002, 1002033, 1092022, 1302021, 1322009, 1322024, 1082148, 1002012, 1322012,
                        1322022, 1002020, 1302013, 1082146, 1442014, 1002096, 1302017, 1442012,

                        /* Warrior equipment */
                        1092011, 1092014, 1302003, 1432001, 1312011, 1002088, 1041020, 1322015, 1442004, 1422008, 1302056, 1432000, 1442005,

                        /* Magician equipment */
                        1382001, 1041053, 1041029, 1050053, 1051032, 1050073, 1061036, 1002253, 1002034, 1051025, 1050067, 1051052, 1002072,
                        1002144, 1051054, 1050069, 1372007, 1050056, 1050074, 1002254, 1002274, 1002218, 1051055, 1382010, 1002246, 1050039,
                        1382007, 1372000, 1002013, 1050072, 1002036, 1002244, 1372008, 1382008, 1382011, 1092021, 1051034, 1050047, 1040019,
                        1041031, 1051033, 1002153, 1002252, 1051024, 1051053, 1050068, 1382003, 1382006, 1050055, 1051031, 1050025, 1002155,
                        1002245, 1372001,

                        /* Bowman equipment */
                        1452004, 1452023, 1060057, 1432001, 1040071, 1002137, 1462009, 1452017, 1040025, 1041027, 1452005, 1452007, 1061057,

                        /* Thief equipment */
                        1472006, 1472019, 1060084, 1472028, 1472004, 1002179, 1082074, 1472029, 1040100, 1332015, 1432001, 1040097, 1060071,
                        1472007, 1472002, 1051009, 1041044, 1041003, 1332016, 1472020, 1332003,
                        
                        /* Pirate equipment */
                        1002622, 1082204, 1082213, 1082198, 1002631, 1052122, 1482012, 1052131, 1482007, 1482004, 1072318, 1492007

                };
	}

	@Override
	public int[] getUncommonItems() {
		return new int[] {2040805, 1102041, 1102042, 1442018};
	}

	@Override
	public int[] getRareItems() {
		return new int[] {};
	}

}
