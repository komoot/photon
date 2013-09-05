package de.komoot.photon.importer;

import com.neovisionaries.i18n.CountryCode;
import de.komoot.photon.importer.model.I18nName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * stores country names in various languages
 *
 * @author christoph
 */
public class InternationalCountryName {
	private final static Logger LOGGER = LoggerFactory.getLogger(InternationalCountryName.class);
	private static Map<CountryCode, I18nName> countries = new HashMap<CountryCode, I18nName>();

	/**
	 * country names extracted from nominatim's database via SQL:
	 *
	 * SELECT country_code,
	 *     name->'name' as name,
	 *     coalesce(name->'name:de', name->'name') AS name_de,
	 *     coalesce(name->'name:en', name->'name') AS name_en,
	 *     coalesce(name->'name:fr', name->'name') AS name_fr,
	 *     coalesce(name->'name:it', name->'name') AS name_it
	 *     FROM country_name;
	 */
	static {
		add("gb", "United Kingdom", "Vereinigtes Königreich", "United Kingdom", "Royaume-Uni", "Gran Bretagna");
		add("us", "United States of America", "Vereinigte Staaten von Amerika", "United States of America", "États-Unis", "Stati Uniti d'America");
		add("de", "Deutschland", "Deutschland", "Germany", "Allemagne", "Germania");
		add("fr", "France", "Frankreich", "France", "France", "Francia");
		add("ch", "Switzerland", "Schweiz", "Switzerland", "Suisse", "Svizzera");
		add("ba", "Bosna i Hercegovina", "Bosnien und Herzegowina", "Bosnia and Herzegovina", "Bosnie-Herzégovine", "Bosnia-Erzegovina");
		add("fi", "Suomi", "Finnland", "Finland", "Finlande", "Finlandia");
		add("ge", "Georgia / საქართველო", "Georgien", "Georgia", "Géorgie", "Georgia");
		add("gr", "Ελλάδα", "Griechenland", "Greece", "Grèce", "Grecia");
		add("ma", "Maroc", "Marokko", "Morocco", "Maroc", "Marocco");
		add("sr", "Suriname", "Suriname", "Suriname", "Suriname", "Suriname");
		add("ar", "Argentina", "Argentinien", "Argentina", "Argentine", "Argentina");
		add("by", "Беларусь", "Weißrussland", "Belarus", "Biélorussie", "Bielorussia");
		add("ck", "Cook Islands", "Cookinseln", "Cook Islands", "Îles Cook", "Isole Cook");
		add("gs", "South Georgia and South Sandwich Islands", "Südgeorgien und die Südlichen Sandwichinseln", "South Georgia and South Sandwich Islands", "Géorgie du Sud-et-les Îles Sandwich du Sud", "Georgia del Sud e isole Sandwich meridionali");
		add("kp", "북조선", "Nordkorea", "North Korea", "Corée du Nord", "Corea del Nord");
		add("kr", "대한민국", "Südkorea", "South Korea", "Corée du Sud", "Corea del Sud");
		add("je", "Jersey", "Jersey", "Jersey", "Jersey", "Jersey");
		add("np", "Nepal", "Nepal", "Nepal", "Népal", "Nepal");
		add("tm", "Türkmenistan", "Turkmenistan", "Turkmenistan", "Turkménistan", "Turkmenistan");
		add("ye", "اليَمَن al-Yaman", "Jemen", "Yemen", "Yémen", "Yemen");
		add("gt", "Guatemala", "Guatemala", "Guatemala", "Guatemala", "Guatemala");
		add("an", "De Nederlandse Antillen", "Niederländische Antillen", "Netherlands Antilles", "Antilles néerlandaises", "Antille Olandesi");
		add("bh", "البحرين", "Bahrain", "Bahrain", "Bahreïn", "Bahrain");
		add("nl", "Nederland", "Niederlande", "The Netherlands", "Pays-Bas", "Paesi Bassi");
		add("cr", "Costa Rica", "Costa Rica", "Costa Rica", "Costa Rica", "Costa Rica");
		add("td", "Tchad / تشاد", "Tschad", "Chad", "Tchad", "Ciad");
		add("nr", "Nauru", "Nauru", "Nauru", "Nauru", "Nauru");
		add("lu", "Luxembourg", "Luxemburg", "Luxembourg", "Luxembourg", "Lussemburgo");
		add("ec", "Ecuador", "Ekuador", "Ecuador", "Équateur", "Ecuador");
		add("fk", "Falkland Islands", "Falklandinseln", "Falkland Islands", "Îles Malouines", "Falkland Islands");
		add("kg", "Kyrgyzstan", "Kirgisistan", "Kyrgyzstan", "Kirghizistan", "Kirghizistan");
		add("kz", "Kazakhstan", "Kasachstan", "Kazakhstan", "Kazakhstan", "Kazakistan");
		add("nf", "Norfolk Island", "Norfolkinsel", "Norfolk Island", "Île Norfolk", "Isola Norfolk");
		add("sv", "", "", "El Salvador", "Salvador", "El Salvador");
		add("tc", "Turks and Caicos Islands", "Turks- und Caicosinseln", "Turks and Caicos Islands", "Îles Turques-et-Caïques", "Turks e Caicos");
		add("gl", "Kalaallit Nunaat", "Grönland", "Greenland", "Groenland", "Groenlandia");
		add("cz", "Česká republika", "Tschechien", "Czech Republic", "République tchèque", "Cechia");
		add("kw", "Kuwait / الكويت", "Kuwait", "Kuwait", "Koweït", "Kuwait");
		add("in", "India", "Indien", "India", "Inde", "India");
		add("tf", "Terres australes et antarctiques françaises", "Französische Süd- und Antarktisgebiete", "French Southern Lands", "Terres australes et antarctiques françaises", "Terre Australi e Antartiche Francesi");
		add("co", "Colombia", "Kolumbien", "Colombia", "Colombie", "Colombia");
		add("er", "Eritrea", "Eritrea", "Eritrea", "Érythrée", "Eritrea");
		add("ro", "România", "Rumänien", "Romania", "Roumanie", "Romania");
		add("md", "Moldova", "Moldawien", "Moldova", "Moldavie", "Moldavia");
		add("tv", "Tuvalu", "Tuvalu", "Tuvalu", "Tuvalu", "Tuvalu");
		add("uz", "Uzbekistan", "Usbekistan", "Uzbekistan", "Ouzbékistan", "Uzbekistan");
		add("dk", "Danmark", "Dänemark", "Denmark", "Danemark", "Danimarca");
		add("ly", "Libya / ليبيا", "Libyen", "Libya", "Libye", "Libia");
		add("qa", "قطر Qatar", "Katar", "Qatar", "Qatar", "Qatar");
		add("sk", "Slovensko", "Slowakei", "Slovakia", "Slovaquie", "Slovacchia");
		add("cx", "Christmas Island", "Weihnachtsinsel", "Christmas Island", "Île Christmas", "Isola del Natale");
		add("nu", "Niue", "Niue", "Niue", "Niue", "Niue");
		add("tk", "Tokelau", "Tokelau", "Tokelau", "Tokelau", "Tokelau");
		add("me", "Crna Gora", "Montenegro", "Montenegro", "Monténégro", "Montenegro");
		add("aq", "Antarctica", "Antarctica", "Antarctica", "Antarctica", "Antarctica");
		add("as", "American Samoa", "American Samoa", "American Samoa", "American Samoa", "American Samoa");
		add("aw", "Aruba", "Aruba", "Aruba", "Aruba", "Aruba");
		add("ax", "Aland Islands", "Aland Islands", "Aland Islands", "Aland Islands", "Aland Islands");
		add("bv", "Bouvet Island", "Bouvet Island", "Bouvet Island", "Bouvet Island", "Bouvet Island");
		add("eh", "Western Sahara", "Western Sahara", "Western Sahara", "Western Sahara", "Western Sahara");
		add("gu", "Guam", "Guam", "Guam", "Guam", "Guam");
		add("hk", "Hong Kong", "Hong Kong", "Hong Kong", "Hong Kong", "Hong Kong");
		add("hm", "Heard Island and MaxDonald Islands", "Heard Island and MaxDonald Islands", "Heard Island and MaxDonald Islands", "Heard Island and MaxDonald Islands", "Heard Island and MaxDonald Islands");
		add("im", "Isle of Man", "Isle of Man", "Isle of Man", "Isle of Man", "Isle of Man");
		add("mo", "Macao", "Macao", "Macao", "Macao", "Macao");
		add("mp", "Northern Mariana Islands", "Northern Mariana Islands", "Northern Mariana Islands", "Northern Mariana Islands", "Northern Mariana Islands");
		add("pr", "Puerto Rico", "Puerto Rico", "Puerto Rico", "Puerto Rico", "Puerto Rico");
		add("ps", "Palestinian Territory", "Palestinian Territory", "Palestinian Territory", "Palestinian Territory", "Palestinian Territory");
		add("pw", "Palau", "Palau", "Palau", "Palau", "Palau");
		add("sh", "Saint Helena", "Saint Helena", "Saint Helena", "Saint Helena", "Saint Helena");
		add("sj", "Svalbard and Jan Mayen", "Svalbard and Jan Mayen", "Svalbard and Jan Mayen", "Svalbard and Jan Mayen", "Svalbard and Jan Mayen");
		add("um", "United States Minor Outlying Islands", "United States Minor Outlying Islands", "United States Minor Outlying Islands", "United States Minor Outlying Islands", "United States Minor Outlying Islands");
		add("vi", "United States Virgin Islands", "United States Virgin Islands", "United States Virgin Islands", "United States Virgin Islands", "United States Virgin Islands");
		add("yt", "Mayotte", "Mayotte", "Mayotte", "Mayotte", "Mayotte");
		add("sb", "Solomon Islands", "Solomon Islands", "Solomon Islands", "Solomon Islands", "Solomon Islands");
		add("pf", "Polynésie française", "Französisch-Polynesien", "French Polynesia", "Polynésie française", "Polinesia francese");
		add("mf", "Saint Martin", "Saint Martin", "Saint Martin", "Saint Martin", "Saint Martin");
		add("bl", "Saint Barthélemy", "Saint Barthélemy", "Saint Barthélemy", "Saint Barthélemy", "Saint Barthélemy");
		add("gh", "Ghana", "Ghana", "Ghana", "Ghana", "Ghana");
		add("my", "Malaysia", "Malaysia", "Malaysia", "Malaisie", "Malesia");
		add("it", "Italia", "Italien", "Italy", "Italie", "Italia");
		add("gw", "Guiné-Bissau", "Guinea-Bissau", "Guinea-Bissau", "Guinée-Bissau", "Guinea-Bissau");
		add("al", "Shqipëria", "Albanien", "Albania", "Albanie", "Albania");
		add("fo", "Føroyar/Færøerne", "Färöer", "Faroe Islands", "Îles Féroé", "Isole Fær Øer");
		add("jp", "日本 (Japan)", "Japan", "Japan", "Japon", "Giappone");
		add("gq", "", "Äquatorialguinea", "Equatorial Guinea", "Guinée équatoriale", "Guinea Equatoriale");
		add("io", "British Indian Ocean Territory", "Britisches Territorium im Indischen Ozean", "British Indian Ocean Territory", "Territoire britannique de l'océan Indien", "Territorio britannico dell'oceano Indiano");
		add("pk", "پاکستان", "Pakistan", "Pakistan", "Pakistan", "Pakistan");
		add("be", "België - Belgique - Belgien", "Belgien", "Belgium", "Belgique", "Belgio");
		add("eg", "Egypt / مصر", "Ägypten", "Egypt", "Égypte", "Egitto");
		add("jo", "Jordan / الأُرْدُن", "Jordanien", "Jordan", "Jordanie", "Giordania");
		add("tn", "تونس", "Tunesien", "Tunisia", "Tunisie", "Tunisia");
		add("dz", "الجزائر", "Algerien", "Algeria", "Algérie", "Algeria");
		add("tw", "Taiwan", "Taiwan", "Taiwan", "Taïwan", "Taiwan");
		add("ph", "Philippines", "Philippinen", "Philippines", "Philippines", "Filippine");
		add("nz", "New Zealand", "Neuseeland", "New Zealand", "Nouvelle-Zélande", "Nuova Zelanda");
		add("ht", "Haiti", "Haiti", "Haiti", "Haïti", "Haiti");
		add("af", "Afghanistan", "Afghanistan", "Afghanistan", "Afghanistan", "Afghanistan");
		add("es", "España", "Spanien", "Spain", "Espagne", "Spagna");
		add("th", "ประเทศไทย", "Thailand", "Thailand", "Thaïlande", "Thailandia");
		add("am", "Armenia", "Armenien", "Armenia", "Arménie", "Armenia");
		add("cu", "Cuba", "Kuba", "Cuba", "Cuba", "Cuba");
		add("pt", "Portugal", "Portugal", "Portugal", "Portugal", "Portogallo");
		add("ad", "Andorra", "Andorra", "Andorra", "Andorre", "Andorra");
		add("si", "Slovenija", "Slowenien", "Slovenia", "Slovénie", "Slovenia");
		add("hn", "Honduras", "Honduras", "Honduras", "Honduras", "Honduras");
		add("do", "República Dominicana", "Dominikanische Republik", "Dominican Republic", "République Dominicaine", "Repubblica Dominicana");
		add("ky", "Cayman Islands", "Kaimaninseln", "Cayman Islands", "Îles Caïmans", "Isole Cayman");
		add("dj", "جيبوتي,Djibouti", "Dschibuti", "Djibouti", "Djibouti", "Gibuti");
		add("hu", "Magyarország", "Ungarn", "Hungary", "Hongrie", "Ungheria");
		add("gg", "Guernsey", "Guernsey", "Guernsey", "Guernesey", "Guernsey");
		add("ie", "Ireland", "Irland", "Ireland", "Irlande", "Irlanda");
		add("km", "Comores,ﺍﻟﻘﻤﺮي,Komori", "Komoren", "Comoros", "Comores", "Comore");
		add("pe", "Peru", "Peru", "Peru", "Pérou", "Perù");
		add("sa", "Saudi Arabia / السعودية", "Saudi-Arabien", "Saudi Arabia", "Arabie saoudite", "Arabia Saudita");
		add("st", "São Tomé e Príncipe", "São Tomé und Príncipe", "São Tomé and Príncipe", "Sao Tomé-et-Principe", "São Tomé e Príncipe");
		add("rs", "Србија (Serbia)", "Serbien", "Serbia", "Serbie", "Serbia");
		add("no", "Norge", "Norwegen", "Norway", "Norvège", "Norvegia");
		add("bi", "Burundi", "Burundi", "Burundi", "Burundi", "Burundi");
		add("il", "ישראל", "Israel", "Israel", "Israël", "Israele");
		add("kn", "Saint Kitts and Nevis", "St. Kitts und Nevis", "Saint Kitts and Nevis", "Saint-Christophe-et-Niévès", "Saint Kitts e Nevis");
		add("lb", "لبنان  Lebanon", "Libanon", "Lebanon", "Liban", "Libano");
		add("bt", "Bhutan", "Bhutan", "Bhutan", "Bhoutan", "Bhutan");
		add("lt", "Lietuva", "Litauen", "Lithuania", "Lituanie", "Lituania");
		add("mk", "Македонија", "Mazedonien", "Macedonia", "Macédoine", "Macedonia");
		add("ao", "Angola", "Angola", "Angola", "Angola", "Angola");
		add("pg", "Papua New Guinea", "Papua-Neuguinea", "Papua New Guinea", "Papouasie-Nouvelle-Guinée", "Papua Nuova Guinea");
		add("sd", "السودان ‎al-Sūdān", "Sudan", "Sudan", "Soudan", "Sudan");
		add("ms", "Montserrat", "Montserrat", "Montserrat", "Montserrat", "Montserrat");
		add("vn", "Việt Nam", "Vietnam", "Vietnam", "Viêt Nam", "Vietnam");
		add("za", "South Africa", "Südafrika", "South Africa", "Afrique du Sud", "Sudafrica");
		add("sc", "Seychelles", "Seychellen", "Seychelles", "Seychelles", "Seychelles");
		add("ir", "ایران", "Iran", "Iran", "Iran", "Iran");
		add("tr", "Türkiye", "Türkei", "Turkey", "Turquie", "Turchia");
		add("sz", "Swaziland", "Swasiland", "Swaziland", "Swaziland", "Swaziland");
		add("ae", "United Arab Emirates", "Vereinigte Arabische Emirate", "United Arab Emirates", "Emirats Arabes Unis", "Emirati Arabi Uniti");
		add("bn", "Brunei Darussalam", "Brunei Darussalam", "Brunei Darussalam", "Brunei", "Brunei");
		add("cl", "Chile", "Chile", "Chile", "Chili", "Cile");
		add("cv", "Cabo Verde", "Kap Verde", "Cape Verde", "Cap-Vert", "Capo Verde");
		add("et", "Ethiopia", "Äthiopien", "Ethiopia", "Éthiopie", "Etiopia");
		add("mz", "Mozambique", "Mosambik", "Mozambique", "Mozambique", "Mozambico");
		add("hr", "Hrvatska", "Kroatien", "Croatia", "Croatie", "Croazia");
		add("py", "Paraguay", "Paraguay", "Paraguay", "Paraguay", "Paraguay");
		add("lk", "Sri Lanka", "Sri Lanka", "Sri Lanka", "Sri Lanka", "Sri Lanka");
		add("mv", "Maldives", "Malediven", "Maldives", "Maldives", "Maldive");
		add("mw", "Malawi", "Malawi", "Malawi", "Malawi", "Malawi");
		add("na", "Namibia", "Namibia", "Namibia", "Namibie", "Namibia");
		add("rw", "Rwanda", "Ruanda", "Rwanda", "Rwanda", "Ruanda");
		add("sy", "Sūriyya سوريا", "Syrien", "Syria", "Syrie", "Siria");
		add("mh", "Marshall Islands", "Marshallinseln", "Marshall Islands", "Îles Marshall", "Isole Marshall");
		add("va", "Città del Vaticano", "Vatikanstadt", "Vatican City", "Vatican", "Città del Vaticano");
		add("ve", "Venezuela", "Venezuela", "Venezuela", "Venezuela", "Venezuela");
		add("vg", "British Virgin Islands", "Britische Jungferninseln", "British Virgin Islands", "Îles Vierges britanniques", "Isole Vergini Britanniche");
		add("id", "Indonesia", "Indonesien", "Indonesia", "Indonésie", "Indonesia");
		add("se", "Sverige", "Schweden", "Sweden", "Suède", "Svezia");
		add("pn", "Pitcairn", "Pitcairn", "Pitcairn", "Îles Pitcairn", "Isole Pitcairn");
		add("az", "Azerbaijan", "Aserbaidschan", "Azerbaijan", "Azerbaïdjan", "Azerbaigian");
		add("cy", "Κύπρος", "Zypern", "Cyprus", "Chypre", "Cipro");
		add("sg", "Singapore", "Singapur", "Singapore", "Singapour", "Singapore");
		add("vu", "Vanuatu", "Vanuatu", "Vanuatu", "Vanuatu", "Vanuatu");
		add("cn", "China 中国", "China", "China", "Chine", "Cina");
		add("cc", "Cocos (Keeling) Islands", "Kokosinseln", "Cocos (Keeling) Islands", "Îles Cocos", "Isole Cocos e Keeling");
		add("bo", "Bolivia", "Bolivien", "Bolivia", "Bolivie", "Bolivia");
		add("br", "Brasil", "Brasilien", "Brazil", "Brésil", "Brasile");
		add("bw", "Botswana", "Botsuana", "Botswana", "Botswana", "Botswana");
		add("la", "Laos", "Laos", "Laos", "Laos", "Laos");
		add("ee", "Eesti", "Estland", "Estonia", "Estonie", "Estonia");
		add("ke", "Kenya", "Kenia", "Kenya", "Kenya", "Kenya");
		add("tj", "Tajikistan", "Tadschikistan", "Tajikistan", "Tadjikistan", "Tagikistan");
		add("tz", "Tanzania", "Tansania", "Tanzania", "Tanzanie", "Tanzania");
		add("ws", "Samoa", "Samoa", "Samoa", "Samoa", "Samoa");
		add("is", "Ísland", "Island", "Iceland", "Islande", "Islanda");
		add("ru", "Россия", "Russland", "Russia", "Russie", "Россия");
		add("ls", "Lesotho", "Lesotho", "Lesotho", "Lesotho", "Lesotho");
		add("om", "سلطنة عُمان Oman", "Oman", "Oman", "Oman", "Oman");
		add("gi", "Gibraltar", "Gibraltar", "Gibraltar", "Gibraltar", "Gibilterra");
		add("lc", "Saint Lucia", "St. Lucia", "Saint Lucia", "Sainte-Lucie", "Santa Lucia");
		add("au", "Australia", "Australien", "Australia", "Australie", "Australia");
		add("bg", "България", "Bulgarien", "Bulgaria", "Bulgarie", "Bulgaria");
		add("cm", "Cameroon,Cameroun", "Kamerun", "Cameroon", "Cameroun", "Camerun");
		add("gd", "Grenada", "Grenada", "Grenada", "Grenade", "Grenada");
		add("iq", "Iraq", "Irak", "Iraq", "Irak", "Iraq");
		add("mm", "Myanmar", "Myanmar", "Myanmar", "Birmanie", "Birmania");
		add("mr", "موريتانيا", "Mauretanien", "Mauritania", "Mauritanie", "Mauritania");
		add("mu", "Mauritius", "Mauritius", "Mauritius", "Maurice", "Mauritius");
		add("ni", "Nicaragua", "Nicaragua", "Nicaragua", "Nicaragua", "Nicaragua");
		add("pa", "Panama", "Panama", "Panama", "Panama", "Panamá");
		add("sm", "San Marino", "San Marino", "San Marino", "Saint-Marin", "San Marino");
		add("so", "Somalia / الصومال", "Somalia", "Somalia", "Somalie", "Somalia");
		add("ug", "Uganda", "Uganda", "Uganda", "Ouganda", "Uganda");
		add("bd", "Bangladesh", "Bangladesch", "Bangladesh", "Bangladesh", "Bangladesh");
		add("kh", "Cambodia", "Kambodscha", "Cambodia", "Cambodge", "Cambogia");
		add("tl", "Timor-Leste, Timor Lorosa'e", "Osttimor", "East Timor", "Timor Oriental", "Timor Est");
		add("lv", "Latvija", "Lettland", "Latvia", "Lettonie", "Lettonia");
		add("mg", "Madagascar", "Madagaskar", "Madagascar", "Madagascar", "Madagascar");
		add("mt", "Malta", "Malta", "Malta", "Malte", "Malta");
		add("mx", "México", "Mexiko", "Mexico", "Mexique", "Messico");
		add("mn", "Монгол Улс", "Mongolei", "Mongolia", "Mongolie", "Mongolia");
		add("pl", "Polska", "Polen", "Poland", "Pologne", "Polonia");
		add("vc", "Saint Vincent and the Grenadines", "Saint Vincent und die Grenadinen", "Saint Vincent and the Grenadines", "Saint-Vincent-et-les Grenadines", "Saint Vincent e Grenadine");
		add("ua", "Україна", "Ukraine", "Ukraine", "Ukraine", "Ucraina");
		add("uy", "Uruguay", "Uruguay", "Uruguay", "Uruguay", "Uruguay");
		add("ai", "Anguilla", "Anguilla", "Anguilla", "Anguilla", "Anguilla");
		add("bm", "Bermuda", "Bermuda", "Bermuda", "Bermudes", "Bermuda");
		add("ag", "Antigua and Barbuda", "Antigua und Barbuda", "Antigua and Barbuda", "Antigua-et-Barbuda", "Antigua e Barbuda");
		add("bb", "Barbados", "Barbados", "Barbados", "Barbade", "Barbados");
		add("bs", "Bahamas", "Bahamas", "Bahamas", "Bahamas", "Bahamas");
		add("bz", "Belize", "Belize", "Belize", "Belize", "Belize");
		add("dm", "Dominica", "Dominica", "Dominica", "Dominique", "Dominìca");
		add("zm", "Zambia", "Sambia", "Zambia", "Zambie", "Zambia");
		add("fj", "Fiji", "Fidschi", "Fiji", "Fidji", "Figi");
		add("gm", "The Gambia", "Gambia", "The Gambia", "Gambie", "Gambia");
		add("gy", "Guyana", "Guyana", "Guyana", "Guyana", "Guyana");
		add("jm", "", "Jamaika", "", "Jamaïque", "Giamaica");
		add("ki", "Kiribati", "Kiribati", "Kiribati", "Kiribati", "Kiribati");
		add("lr", "Liberia", "Liberia", "Liberia", "Liberia", "Liberia");
		add("fm", "Micronesia", "Mikronesien", "Micronesia", "Micronésie", "Micronesia");
		add("ng", "Nigeria", "Nigeria", "Nigeria", "Nigeria", "Nigeria");
		add("sl", "Sierra Leone", "Sierra Leone", "Sierra Leone", "Sierra Leone", "Sierra Leone");
		add("to", "Tonga", "Tonga", "Tonga", "Tonga", "Tonga");
		add("tt", "Trinidad and Tobago", "Trinidad und Tobago", "Trinidad and Tobago", "Trinité-et-Tobago", "Trinidad e Tobago");
		add("mq", "Martinique", "Martinique", "Martinique", "Martinique", "Martinica");
		add("zw", "Zimbabwe", "Simbabwe", "Zimbabwe", "Zimbabwe", "Zimbabwe");
		add("bj", "Bénin", "Benin", "Benin", "Bénin", "Benin");
		add("bf", "Burkina Faso", "Burkina Faso", "Burkina Faso", "Burkina Faso", "Burkina Faso");
		add("ne", "Niger", "Niger", "Niger", "Niger", "Niger");
		add("cf", "Centrafrique", "Zentralafrikanische Republik", "Central African Republic", "Centrafrique", "Repubblica Centrafricana");
		add("ga", "Gabon", "Gabun", "Gabon", "Gabon", "Gabon");
		add("ci", "Côte d'Ivoire", "Elfenbeinküste", "Côte d'Ivoire", "Côte d'Ivoire", "Costa d'Avorio");
		add("cd", "République Démocratique du Congo", "Demokratische Republik Kongo", "Democratic Republic of the Congo", "République démocratique du Congo", "Repubblica Democratica del Congo");
		add("cg", "République du Congo", "Republik Kongo", "Republic of the Congo", "République du Congo", "Repubblica del Congo");
		add("gf", "Guyane Française", "Französisch-Guayana", "French Guiana", "Guyane française", "Guyana francese");
		add("gp", "Guadeloupe", "Guadeloupe", "Guadeloupe", "Guadeloupe", "Guadalupa");
		add("nc", "Nouvelle-Calédonie", "Neukaledonien", "New Caledonia", "Nouvelle-Calédonie", "Nuova Caledonia");
		add("re", "Réunion", "Réunion", "Réunion", "La Réunion", "Riunione");
		add("pm", "Saint-Pierre-et-Miquelon", "Saint-Pierre und Miquelon", "Saint Pierre and Miquelon", "Saint-Pierre-et-Miquelon", "Saint-Pierre-et-Miquelon");
		add("sn", "Sénégal", "Senegal", "Senegal", "Sénégal", "Senegal");
		add("wf", "Wallis-et-Futuna", "Wallis und Futuna", "Wallis and Futuna Islands", "Wallis-et-Futuna", "Wallis e Futuna");
		add("gn", "Guinée", "Guinea", "Guinea", "Guinée", "Guinea");
		add("ml", "Mali", "Mali", "Mali", "Mali", "Mali");
		add("mc", "Monaco", "Monaco", "Monaco", "Monaco", "Monaco");
		add("tg", "Togo", "Togo", "Togo", "Togo", "Togo");
		add("ca", "Canada", "Kanada", "Canada", "Canada", "Canada");
		add("at", "Österreich", "Österreich", "Austria", "Autriche", "Austria");
		add("li", "Liechtenstein", "Liechtenstein", "Liechtenstein", "Liechtenstein", "Liechtenstein");
		add("ss", "South Sudan", "Südsudan", "South Sudan", "Sud-Soudan", "South Sudan");
		add("cw", "Curaçao", "Curaçao", "Curaçao", "Curaçao", "Curaçao");
		add("sx", "Sint Maarten", "Sint Maarten", "Sint Maarten", "Sint Maarten", "Sint Maarten");
	}

	private static void add(String countryCode, String name, String de, String en, String fr, String it) {
		CountryCode country = CountryCode.getByCode(countryCode);
		I18nName i18nName = new I18nName(name, de, en, fr, it);
		countries.put(country, i18nName);
	}

	/**
	 * the translations for a country
	 *
	 * @param country
	 * @return can be null if country not registered
	 */
	public static I18nName get(CountryCode country) {
		I18nName name = countries.get(country);
		if(name == null) {
			if(country != null) {
				LOGGER.warn(String.format("translations for country [%s] could not be found.", country));
			}
		}
		return name;
	}
}
