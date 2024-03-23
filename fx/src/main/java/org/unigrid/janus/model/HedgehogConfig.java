/*
	The Janus Wallet
	Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

	This program is free software: you can redistribute it and/or modify it under the terms of the
	addended GNU Affero General Public License as published by the Free Software Foundation, version 3
	of the License (see COPYING and COPYING.addendum).

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
	even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU Affero General Public License for more details.

	You should have received an addended copy of the GNU Affero General Public License with this program.
	If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.model;

import lombok.Getter;

public class HedgehogConfig {

	public enum startMode {
		MAIN_NET, TEST_NET, DEV_NET;
	}

//	@Getter
//	private String testnetRestPort = "--restport=39886";
//	@Getter
//	private String testnetP2pPort = "--netport=39999";
//	@Getter
//	private String testnetPublicKey = "--network-keys="
//		+ "119253202bba883a61ca2309307e6b8bb4bf11f7e52939c9b47cea5a2f8c2f8a74171b0c525942c1879eb028a1b31c46ffc"
//		+ "9b509de735532b207c1e89131b6129b1113dffdb2b5eaf4df2e5fe02513c5f209bfe3581122a6c7326dcb4f47f96925ac44"
//		+ "e53191d5cd4be9079fc953cac6199077d5038d6049c8969010bc2bdaa364bd6e,"
//		+ "1f5659d41cbf666d38a01702ec6101ec13240d939e637a150229c313d4d6345500f6f0e750fd3229ef43f716b1af12ed17e"
//		+ "b786d3e414d2db5dd9ac98eadda6be711ca6b6cca33d8370a7f71826d59352b2abd1997c4af7193f14aa67f2970f0daca4f"
//		+ "843d280e8eead0dd0207e0e931871427e9e6f8ed49e99a695d9c08675ff658fa,1f32a8aa10de71982fffe69407a3fe8347"
//		+ "bf7f95e21b58ba69a8c02a65f345d168868cabd3a94beb124989d59a571e715980592c4653f380a96e9c59aa1d70860aa12"
//		+ "07192d0bd08e6accf9e81841bfba1f93f34b533acc216af4ca5d6f4438339e2e273068a5f5cfb682c9c7bd0c6a4d0d5fcfd"
//		+ "69948781fee95e89c53f0755edd46e";
//
//	@Getter
//	private String devnetRestPort = "--restport=40001";
//	@Getter
//	private String devnetP2pPort = "--netport=40000";
//	@Getter
//	private String devnetPublicKey = "--network-keys="
//		+ "14c7221118dbc6899b33130d17755ab8dee570238c0ad05d0c0a0b68ef4b5"
//		+ "9cdd5a7e6eeca6c4e5005f890ea55f86b91013689aae2f40e909b8f146f04d7713d7781922754ac36fc1ba32844f496dd49"
//		+ "7745e7c88067ff1d7d34735bb501ecfbf7f263037f2f53289a9f5c79723e5dbbf9009c03fba44f80dfbac1755d27fbbe446c"
//		+ "7e,"
//		+ "1dc5a8e09ec9e6551f26ef7fd74e285a7b98b3fd445dfc7104f986ee0cdbe56f049cf8629f05a0de0d33f52d2855a2beab2"
//		+ "63ac8adfc939a618a4f633d1a1cf89711493cff5f6c552fd907066ef190fa5fa6ad568015b9773e92ac36d98cec5071aa83"
//		+ "dd70b6ee9603a76c62330624bb86adc4aaab56a856bd70ccc7ae3f21ef69523c,"
//		+ "11e75547b4ae735d5fc416cae42591fb8fba9339bd66c74ccaabf1583f6fde37fd6dc4cc43fbf71d0ad3e503c92ed0dad46"
//		+ "224d089d588404348c5251f1ed921c511714e893c69bd3df26f9e00453978a8bba7d7b7e343af178d54d6c2652b20d604d1"
//		+ "e5a1f4c5bb3a232bdfc025ca6588e78321d5ac679c67ebb67ec9532a44b253b8";
//
//	@Getter
//	private String testnetConnectionAddress = "149.102.147.45:39999";
//
//	@Getter
//	private String devnetConnectionAddress = "173.212.208.212:40000";
		@Getter
	private String testnetRestPort = "--restport=39886";
	@Getter
	private String testnetP2pPort = "--netport=39999";
	@Getter
	private String testnetPublicKey = "--network-keys="
		+ "119253202bba883a61ca2309307e6b8bb4bf11f7e52939c9b47cea5a2f8c2f8a74171b0c525942c1879eb028a1b31c46ffc"
		+ "9b509de735532b207c1e89131b6129b1113dffdb2b5eaf4df2e5fe02513c5f209bfe3581122a6c7326dcb4f47f96925ac44"
		+ "e53191d5cd4be9079fc953cac6199077d5038d6049c8969010bc2bdaa364bd6e,"
		+ "1f5659d41cbf666d38a01702ec6101ec13240d939e637a150229c313d4d6345500f6f0e750fd3229ef43f716b1af12ed17e"
		+ "b786d3e414d2db5dd9ac98eadda6be711ca6b6cca33d8370a7f71826d59352b2abd1997c4af7193f14aa67f2970f0daca4f"
		+ "843d280e8eead0dd0207e0e931871427e9e6f8ed49e99a695d9c08675ff658fa,1f32a8aa10de71982fffe69407a3fe8347"
		+ "bf7f95e21b58ba69a8c02a65f345d168868cabd3a94beb124989d59a571e715980592c4653f380a96e9c59aa1d70860aa12"
		+ "07192d0bd08e6accf9e81841bfba1f93f34b533acc216af4ca5d6f4438339e2e273068a5f5cfb682c9c7bd0c6a4d0d5fcfd"
		+ "69948781fee95e89c53f0755edd46e";

	@Getter
	private String devnetRestPort = "--restport=39886";
	@Getter
	private String devnetP2pPort = "--netport=39999";
	@Getter
	private String devnetPublicKey = "--network-keys="
		+ "119253202bba883a61ca2309307e6b8bb4bf11f7e52939c9b47cea5a2f8c2f8a74171b0c525942c1879eb028a1b31c46ffc"
		+ "9b509de735532b207c1e89131b6129b1113dffdb2b5eaf4df2e5fe02513c5f209bfe3581122a6c7326dcb4f47f96925ac44"
		+ "e53191d5cd4be9079fc953cac6199077d5038d6049c8969010bc2bdaa364bd6e,"
		+ "1f5659d41cbf666d38a01702ec6101ec13240d939e637a150229c313d4d6345500f6f0e750fd3229ef43f716b1af12ed17e"
		+ "b786d3e414d2db5dd9ac98eadda6be711ca6b6cca33d8370a7f71826d59352b2abd1997c4af7193f14aa67f2970f0daca4f"
		+ "843d280e8eead0dd0207e0e931871427e9e6f8ed49e99a695d9c08675ff658fa,1f32a8aa10de71982fffe69407a3fe8347"
		+ "bf7f95e21b58ba69a8c02a65f345d168868cabd3a94beb124989d59a571e715980592c4653f380a96e9c59aa1d70860aa12"
		+ "07192d0bd08e6accf9e81841bfba1f93f34b533acc216af4ca5d6f4438339e2e273068a5f5cfb682c9c7bd0c6a4d0d5fcfd"
		+ "69948781fee95e89c53f0755edd46e";

	@Getter
	private String testnetConnectionAddress = "149.102.147.45:39999";

	@Getter
	private String devnetConnectionAddress = "149.102.147.45:39999";
}
