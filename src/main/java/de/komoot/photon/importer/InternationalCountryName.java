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
         *     coalesce(name->'name:es', name->'name') AS name_es,
         *     coalesce(name->'name:pt', name->'name') AS name_pt,
         *     coalesce(name->'name:ja', name->'name') AS name_ja,
         *     coalesce(name->'name:zh', name->'name') AS name_zh
	 *     FROM country_name;
	 */
	static {
            add("us", "United States of America", "Vereinigte Staaten von Amerika", "United States of America", "États-Unis", "Stati Uniti d'America", "Estados Unidos", "Estados Unidos", "アメリカ合衆国", "美国");
            add("de", "Deutschland", "Deutschland", "Germany", "Allemagne", "Germania", "Alemania", "Alemanha", "ドイツ", "德国");
            add("fr", "France", "Frankreich", "France", "France", "Francia", "Francia", "França", "フランス", "法国");
            add("ch", "Switzerland", "Schweiz", "Switzerland", "Suisse", "Svizzera", "Suiza", "Suíça", "スイス", "瑞士");
            add("ba", "Bosna i Hercegovina", "Bosnien und Herzegowina", "Bosnia and Herzegovina", "Bosnie-Herzégovine", "Bosnia-Erzegovina", "Bosnia y Herzegovina", "Bósnia e Herzegovina", "ボスニア・ヘルツェゴビナ", "波斯尼亚和黑塞哥维纳");
            add("fi", "Suomi", "Finnland", "Finland", "Finlande", "Finlandia", "Finlandia", "Finlândia", "フィンランド", "芬兰");
            add("ge", "Georgia / საქართველო", "Georgien", "Georgia", "Géorgie", "Georgia", "Georgia", "Geórgia", "グルジア", "乔治亚");
            add("gr", "Ελλάδα", "Griechenland", "Greece", "Grèce", "Grecia", "Grecia", "Grécia", "ギリシャ", "希腊");
            add("ma", "Maroc", "Marokko", "Morocco", "Maroc", "Marocco", "Marruecos", "Marrocos", "モロッコ", "摩洛哥");
            add("sr", "Suriname", "Suriname", "Suriname", "Suriname", "Suriname", "Surinam", "Suriname", "スリナム", "苏里南");
            add("ar", "Argentina", "Argentinien", "Argentina", "Argentine", "Argentina", "Argentina", "Argentina", "アルゼンチン", "阿根廷");
            add("by", "Беларусь", "Weißrussland", "Belarus", "Biélorussie", "Bielorussia", "Bielorrusia", "Bielorrússia", "ベラルーシ", "白俄罗斯");
            add("ck", "Cook Islands", "Cookinseln", "Cook Islands", "Îles Cook", "Isole Cook", "Islas Cook", "Cook Islands", "Cook Islands", "库克群岛");
            add("gs", "South Georgia and South Sandwich Islands", "Südgeorgien und die Südlichen Sandwichinseln", "South Georgia and South Sandwich Islands", "Géorgie du Sud-et-les Îles Sandwich du Sud", "Georgia del Sud e isole Sandwich meridionali", "Islas Georgias del Sur y Sandwich del Sur", "Ilhas Geórgia do Sul e Sandwich do Sul", "サウスジョージア・サウスサンドウィッチ諸島", "南喬治亞島與南桑威奇群島");
            add("kp", "북조선", "Nordkorea", "North Korea", "Corée du Nord", "Corea del Nord", "Corea del Norte", "Coreia do Norte", "北朝鮮", "朝鲜");
            add("kr", "대한민국", "Südkorea", "South Korea", "Corée du Sud", "Corea del Sud", "Corea del Sur", "Coreia do Sul", "韓国", "韩国");
            add("je", "Jersey", "Jersey", "Jersey", "Jersey", "Jersey", "Jersey", "Jersey", "Jersey", "泽西岛");
            add("np", "Nepal", "Nepal", "Nepal", "Népal", "Nepal", "Nepal", "Nepal", "ネパール", "尼泊尔");
            add("tm", "Türkmenistan", "Turkmenistan", "Turkmenistan", "Turkménistan", "Turkmenistan", "Turkmenistán", "Turcomenistão", "トルクメニスタン", "土库曼斯坦");
            add("ye", "اليَمَن al-Yaman", "Jemen", "Yemen", "Yémen", "Yemen", "Yemen", "Iêmen", "イエメン", "也门");
            add("gt", "Guatemala", "Guatemala", "Guatemala", "Guatemala", "Guatemala", "Guatemala", "Guatemala", "グアテマラ", "危地马拉");
            add("an", "De Nederlandse Antillen", "Niederländische Antillen", "Netherlands Antilles", "Antilles néerlandaises", "Antille Olandesi", "Antillas Neerlandesas;Antillas Holandesas;Indias Occidentales Holandesas", "Antilhas Holandesas", "オランダ領アンティル", "荷属安的列斯");
            add("bh", "البحرين", "Bahrain", "Bahrain", "Bahreïn", "Bahrain", "Bahréin", "Bahrein", "バーレーン", "巴林");
            add("nl", "Nederland", "Niederlande", "The Netherlands", "Pays-Bas", "Paesi Bassi", "Países Bajos", "Países Baixos", "オランダ", "荷兰");
            add("cr", "Costa Rica", "Costa Rica", "Costa Rica", "Costa Rica", "Costa Rica", "Costa Rica", "Costa Rica", "コスタリカ", "哥斯大黎加");
            add("td", "Tchad / تشاد", "Tschad", "Chad", "Tchad", "Ciad", "Chad", "Chade", "Tchad / تشاد", "乍得");
            add("nr", "Nauru", "Nauru", "Nauru", "Nauru", "Nauru", "Nauru", "Nauru", "Nauru", "瑙魯");
            add("lu", "Luxembourg", "Luxemburg", "Luxembourg", "Luxembourg", "Lussemburgo", "Luxemburgo", "Luxembourg", "ルクセンブルク", "卢森堡");
            add("ec", "Ecuador", "Ekuador", "Ecuador", "Équateur", "Ecuador", "Ecuador", "Equador", "エクアドル", "厄瓜多尔");
            add("fk", "Falkland Islands", "Falklandinseln", "Falkland Islands", "Îles Malouines", "Falkland Islands", "Islas Malvinas", "Falkland Islands", "Falkland Islands", "福克兰群岛");
            add("kg", "Kyrgyzstan", "Kirgisistan", "Kyrgyzstan", "Kirghizistan", "Kirghizistan", "Kirguistán", "Kyrgyzstan", "Kyrgyzstan", "吉尔吉斯斯坦");
            add("kz", "Kazakhstan", "Kasachstan", "Kazakhstan", "Kazakhstan", "Kazakistan", "Kazajistán", "Kazakhstan", "カザフスタン", "哈萨克斯坦");
            add("nf", "Norfolk Island", "Norfolkinsel", "Norfolk Island", "Île Norfolk", "Isola Norfolk", "Isla Norfolk", "Norfolk Island", "Norfolk Island", "诺福克岛");
            add("sv", "", "", "El Salvador", "Salvador", "El Salvador", "", "", "エルサルバドル", "萨尔瓦多");
            add("tc", "Turks and Caicos Islands", "Turks- und Caicosinseln", "Turks and Caicos Islands", "Îles Turques-et-Caïques", "Turks e Caicos", "Islas Turcas y Caicos", "Turks e Caicos", "タークス・カイコス諸島", "特克斯和凱科斯群島");
            add("gl", "Kalaallit Nunaat", "Grönland", "Greenland", "Groenland", "Groenlandia", "Groenlandia", "Gronelândia", "グリーンランド", "格陵兰岛");
            add("cz", "Česká republika", "Tschechien", "Czech Republic", "République tchèque", "Cechia", "República Checa", "República Checa", "チェコ", "捷克");
            add("kw", "Kuwait / الكويت", "Kuwait", "Kuwait", "Koweït", "Kuwait", "Kuwait / الكويت", "Kuwait", "クウェート", "科威特");
            add("in", "India", "Indien", "India", "Inde", "India", "India", "India", "インド", "印度");
            add("tf", "Terres australes et antarctiques françaises", "Französische Süd- und Antarktisgebiete", "French Southern Lands", "Terres australes et antarctiques françaises", "Terre Australi e Antartiche Francesi", "Tierras Australes y Antárticas Francesas", "Terras Austrais e Antárticas Francesas", "フランス領南方・南極地域", "法属南部领地");
            add("co", "Colombia", "Kolumbien", "Colombia", "Colombie", "Colombia", "Colombia", "Colômbia", "コロンビア", "哥伦比亚");
            add("er", "Eritrea", "Eritrea", "Eritrea", "Érythrée", "Eritrea", "Eritrea", "Eritreia;Eritréia", "エリトリア", "厄立特里亚");
            add("ro", "România", "Rumänien", "Romania", "Roumanie", "Romania", "Rumania", "România", "ルーマニア", "罗马尼亚");
            add("md", "Moldova", "Moldawien", "Moldova", "Moldavie", "Moldavia", "Moldavia", "Moldova", "モルドバ", "摩尔多瓦");
            add("tv", "Tuvalu", "Tuvalu", "Tuvalu", "Tuvalu", "Tuvalu", "Tuvalu", "Tuvalu", "ツバル", "吐瓦鲁");
            add("uz", "Uzbekistan", "Usbekistan", "Uzbekistan", "Ouzbékistan", "Uzbekistan", "Uzbekistán", "Uzbequistão", "ウズベキスタン", "乌兹别克斯坦");
            add("dk", "Danmark", "Dänemark", "Denmark", "Danemark", "Danimarca", "Dinamarca", "Dinamarca", "デンマーク", "丹麦");
            add("ly", "Libya / ليبيا", "Libyen", "Libya", "Libye", "Libia", "Libia", "Líbia", "リビア", "利比亚");
            add("qa", "قطر Qatar", "Katar", "Qatar", "Qatar", "Qatar", "Qatar", "Qatar", "カタール", "卡塔尔");
            add("sk", "Slovensko", "Slowakei", "Slovakia", "Slovaquie", "Slovacchia", "Eslovaquia", "Eslováquia", "Slovensko", "斯洛伐克");
            add("cx", "Christmas Island", "Weihnachtsinsel", "Christmas Island", "Île Christmas", "Isola del Natale", "Isla de Navidad", "Ilha Christmas", "クリスマス島", "圣诞岛");
            add("nu", "Niue", "Niue", "Niue", "Niue", "Niue", "Niue", "Niue", "Niue", "紐埃");
            add("tk", "Tokelau", "Tokelau", "Tokelau", "Tokelau", "Tokelau", "Tokelau", "Tokelau", "Tokelau", "托克劳");
            add("me", "Crna Gora", "Montenegro", "Montenegro", "Monténégro", "Montenegro", "Montenegro", "Montenegro", "モンテネグロ", "黑山");
            add("aq", "Antarctica", "Antarctica", "Antarctica", "Antarctica", "Antarctica", "Antarctica", "Antarctica", "Antarctica", "Antarctica");
            add("as", "American Samoa", "American Samoa", "American Samoa", "American Samoa", "American Samoa", "American Samoa", "American Samoa", "American Samoa", "American Samoa");
            add("aw", "Aruba", "Aruba", "Aruba", "Aruba", "Aruba", "Aruba", "Aruba", "Aruba", "Aruba");
            add("ax", "Aland Islands", "Aland Islands", "Aland Islands", "Aland Islands", "Aland Islands", "Aland Islands", "Aland Islands", "Aland Islands", "Aland Islands");
            add("bv", "Bouvet Island", "Bouvet Island", "Bouvet Island", "Bouvet Island", "Bouvet Island", "Bouvet Island", "Bouvet Island", "Bouvet Island", "Bouvet Island");
            add("eh", "Western Sahara", "Western Sahara", "Western Sahara", "Western Sahara", "Western Sahara", "Western Sahara", "Western Sahara", "Western Sahara", "Western Sahara");
            add("gu", "Guam", "Guam", "Guam", "Guam", "Guam", "Guam", "Guam", "Guam", "Guam");
            add("hk", "Hong Kong", "Hong Kong", "Hong Kong", "Hong Kong", "Hong Kong", "Hong Kong", "Hong Kong", "Hong Kong", "Hong Kong");
            add("hm", "Heard Island and MaxDonald Islands", "Heard Island and MaxDonald Islands", "Heard Island and MaxDonald Islands", "Heard Island and MaxDonald Islands", "Heard Island and MaxDonald Islands", "Heard Island and MaxDonald Islands", "Heard Island and MaxDonald Islands", "Heard Island and MaxDonald Islands", "Heard Island and MaxDonald Islands");
            add("im", "Isle of Man", "Isle of Man", "Isle of Man", "Isle of Man", "Isle of Man", "Isle of Man", "Isle of Man", "Isle of Man", "Isle of Man");
            add("mo", "Macao", "Macao", "Macao", "Macao", "Macao", "Macao", "Macao", "Macao", "Macao");
            add("mp", "Northern Mariana Islands", "Northern Mariana Islands", "Northern Mariana Islands", "Northern Mariana Islands", "Northern Mariana Islands", "Northern Mariana Islands", "Northern Mariana Islands", "Northern Mariana Islands", "Northern Mariana Islands");
            add("pr", "Puerto Rico", "Puerto Rico", "Puerto Rico", "Puerto Rico", "Puerto Rico", "Puerto Rico", "Puerto Rico", "Puerto Rico", "Puerto Rico");
            add("ps", "Palestinian Territory", "Palestinian Territory", "Palestinian Territory", "Palestinian Territory", "Palestinian Territory", "Palestinian Territory", "Palestinian Territory", "Palestinian Territory", "Palestinian Territory");
            add("pw", "Palau", "Palau", "Palau", "Palau", "Palau", "Palau", "Palau", "Palau", "Palau");
            add("sh", "Saint Helena", "Saint Helena", "Saint Helena", "Saint Helena", "Saint Helena", "Saint Helena", "Saint Helena", "Saint Helena", "Saint Helena");
            add("sj", "Svalbard and Jan Mayen", "Svalbard and Jan Mayen", "Svalbard and Jan Mayen", "Svalbard and Jan Mayen", "Svalbard and Jan Mayen", "Svalbard and Jan Mayen", "Svalbard and Jan Mayen", "Svalbard and Jan Mayen", "Svalbard and Jan Mayen");
            add("um", "United States Minor Outlying Islands", "United States Minor Outlying Islands", "United States Minor Outlying Islands", "United States Minor Outlying Islands", "United States Minor Outlying Islands", "United States Minor Outlying Islands", "United States Minor Outlying Islands", "United States Minor Outlying Islands", "United States Minor Outlying Islands");
            add("vi", "United States Virgin Islands", "United States Virgin Islands", "United States Virgin Islands", "United States Virgin Islands", "United States Virgin Islands", "United States Virgin Islands", "United States Virgin Islands", "United States Virgin Islands", "United States Virgin Islands");
            add("yt", "Mayotte", "Mayotte", "Mayotte", "Mayotte", "Mayotte", "Mayotte", "Mayotte", "Mayotte", "Mayotte");
            add("sb", "Solomon Islands", "Solomon Islands", "Solomon Islands", "Solomon Islands", "Solomon Islands", "Solomon Islands", "Solomon Islands", "Solomon Islands", "Solomon Islands");
            add("pf", "Polynésie française", "Französisch-Polynesien", "French Polynesia", "Polynésie française", "Polinesia francese", "Polinesia Francesa", "Polinésia Francesa", "フランス領ポリネシア", "法属波利尼西亚");
            add("mf", "Saint Martin", "Saint Martin", "Saint Martin", "Saint Martin", "Saint Martin", "Saint Martin", "Saint Martin", "Saint Martin", "Saint Martin");
            add("bl", "Saint Barthélemy", "Saint Barthélemy", "Saint Barthélemy", "Saint Barthélemy", "Saint Barthélemy", "Saint Barthélemy", "Saint Barthélemy", "Saint Barthélemy", "Saint Barthélemy");
            add("gh", "Ghana", "Ghana", "Ghana", "Ghana", "Ghana", "Ghana", "Gana", "ガーナ", "加纳");
            add("my", "Malaysia", "Malaysia", "Malaysia", "Malaisie", "Malesia", "Malasia", "Malaysia", "マレーシア", "马来西亚");
            add("it", "Italia", "Italien", "Italy", "Italie", "Italia", "Italia", "Italia", "イタリア", "意大利");
            add("gw", "Guiné-Bissau", "Guinea-Bissau", "Guinea-Bissau", "Guinée-Bissau", "Guinea-Bissau", "Guinea-Bissau", "Guiné-Bissau", "Guiné-Bissau", "几内亚比绍");
            add("al", "Shqipëria", "Albanien", "Albania", "Albanie", "Albania", "Albania", "Albânia", "アルバニア", "阿尔巴尼亚");
            add("fo", "Føroyar/Færøerne", "Färöer", "Faroe Islands", "Îles Féroé", "Isole Fær Øer", "Islas Feroe", "Ilhas Feroe", "フェロー諸島", "法罗群岛");
            add("jp", "日本 (Japan)", "Japan", "Japan", "Japon", "Giappone", "Japón", "Japão", "日本", "日本");
            add("gq", "", "Äquatorialguinea", "Equatorial Guinea", "Guinée équatoriale", "Guinea Equatoriale", "Guinea Ecuatorial", "Guiné Equatorial", "赤道ギニア", "赤道几内亚");
            add("io", "British Indian Ocean Territory", "Britisches Territorium im Indischen Ozean", "British Indian Ocean Territory", "Territoire britannique de l'océan Indien", "Territorio britannico dell'oceano Indiano", "Territorio Británico del Océano Índico", "Território Britânico do Oceano Índico", "イギリス領インド洋地域", "英属印度洋领地");
            add("pk", "پاکستان", "Pakistan", "Pakistan", "Pakistan", "Pakistan", "Pakistán", "Paquistão", "パキスタン", "巴基斯坦");
            add("be", "België - Belgique - Belgien", "Belgien", "Belgium", "Belgique", "Belgio", "Bélgica", "Bélgica", "ベルギー", "比利时");
            add("eg", "Egypt / مصر", "Ägypten", "Egypt", "Égypte", "Egitto", "Egipto", "Egito", "エジプト", "埃及");
            add("jo", "Jordan / الأُرْدُن", "Jordanien", "Jordan", "Jordanie", "Giordania", "Jordania", "Jordânia", "ヨルダン", "约旦");
            add("tn", "تونس", "Tunesien", "Tunisia", "Tunisie", "Tunisia", "Túnez", "Tunísia", "チュニジア", "突尼西亚");
            add("dz", "الجزائر", "Algerien", "Algeria", "Algérie", "Algeria", "Argelia", "Argélia", "アルジェリア", "阿尔及利亚");
            add("tw", "Taiwan", "Taiwan", "Taiwan", "Taïwan", "Taiwan", "Taiwan", "Taiwan; Formosa", "台湾", "台湾");
            add("ph", "Philippines", "Philippinen", "Philippines", "Philippines", "Filippine", "Filipinas", "Philippines", "フィリピン", "菲律宾");
            add("nz", "New Zealand", "Neuseeland", "New Zealand", "Nouvelle-Zélande", "Nuova Zelanda", "Nueva Zelanda", "New Zealand", "ニュージーランド", "新西兰");
            add("ht", "Haiti", "Haiti", "Haiti", "Haïti", "Haiti", "Haití", "Haiti", "ハイチ", "海地");
            add("af", "Afghanistan", "Afghanistan", "Afghanistan", "Afghanistan", "Afghanistan", "Afganistán", "Afeganistão", "アフガニスタン", "阿富汗");
            add("es", "España", "Spanien", "Spain", "Espagne", "Spagna", "España", "Espanha", "スペイン", "西班牙");
            add("th", "ประเทศไทย", "Thailand", "Thailand", "Thaïlande", "Thailandia", "Tailandia", "Tailândia", "タイ王国", "泰国");
            add("am", "Armenia", "Armenien", "Armenia", "Arménie", "Armenia", "Armenia", "Armênia", "アルメニア", "亚美尼亚");
            add("cu", "Cuba", "Kuba", "Cuba", "Cuba", "Cuba", "Cuba", "Cuba", "キューバ", "古巴");
            add("pt", "Portugal", "Portugal", "Portugal", "Portugal", "Portogallo", "Portugal", "Portugal", "ポルトガル", "葡萄牙");
            add("ad", "Andorra", "Andorra", "Andorra", "Andorre", "Andorra", "Andorra", "Andorra", "アンドラ", "安道尔");
            add("si", "Slovenija", "Slowenien", "Slovenia", "Slovénie", "Slovenia", "Eslovenia", "Eslovênia", "Slovenija", "斯洛文尼亚");
            add("hn", "Honduras", "Honduras", "Honduras", "Honduras", "Honduras", "Honduras", "Honduras", "ホンジュラス", "洪都拉斯");
            add("do", "República Dominicana", "Dominikanische Republik", "Dominican Republic", "République Dominicaine", "Repubblica Dominicana", "República Dominicana", "República Dominicana", "República Dominicana", "多明尼加共和國");
            add("ky", "Cayman Islands", "Kaimaninseln", "Cayman Islands", "Îles Caïmans", "Isole Cayman", "Islas Caimán", "Ilhas Cayman", "Cayman Islands", "开曼群岛");
            add("dj", "جيبوتي;Djibouti", "Dschibuti", "Djibouti", "Djibouti", "Gibuti", "Yibuti", "Djibuti", "ジブチ", "吉布提");
            add("hu", "Magyarország", "Ungarn", "Hungary", "Hongrie", "Ungheria", "Hungría", "Magyarország", "ハンガリー", "匈牙利");
            add("gg", "Guernsey", "Guernsey", "Guernsey", "Guernesey", "Guernsey", "Guernsey", "Guernsey", "Guernsey", "格恩西岛");
            add("ie", "Ireland", "Irland", "Ireland", "Irlande", "Irlanda", "Irlanda", "Ireland", "アイルランド", "爱尔兰");
            add("km", "Comores;ﺍﻟﻘﻤﺮي;Komori", "Komoren", "Comoros", "Comores", "Comore", "Comoras", "Comores", "コモロ", "科摩洛");
            add("pe", "Peru", "Peru", "Peru", "Pérou", "Perù", "Perú", "Peru", "ペルー", "秘鲁");
            add("sa", "Saudi Arabia / السعودية", "Saudi-Arabien", "Saudi Arabia", "Arabie saoudite", "Arabia Saudita", "Arabia Saudita", "Arábia Saudita", "サウジアラビア", "沙特阿拉伯");
            add("st", "São Tomé e Príncipe", "São Tomé und Príncipe", "São Tomé and Príncipe", "Sao Tomé-et-Principe", "São Tomé e Príncipe", "Santo Tomé y Príncipe", "São Tomé e Príncipe", "São Tomé e Príncipe", "圣多美和普林西比");
            add("rs", "Србија (Serbia)", "Serbien", "Serbia", "Serbie", "Serbia", "Serbia", "Sérvia", "セルビア", "塞尔维亚");
            add("no", "Norge", "Norwegen", "Norway", "Norvège", "Norvegia", "Noruega", "Norge", "ノルウェー", "挪威");
            add("bi", "Burundi", "Burundi", "Burundi", "Burundi", "Burundi", "Burundi", "Burundi", "ブルンジ", "布隆迪");
            add("il", "ישראל", "Israel", "Israel", "Israël", "Israele", "Israel", "Israel", "イスラエル", "以色列");
            add("kn", "Saint Kitts and Nevis", "St. Kitts und Nevis", "Saint Kitts and Nevis", "Saint-Christophe-et-Niévès", "Saint Kitts e Nevis", "San Cristóbal y Nieves", "São Cristóvão e Nevis", "Saint Kitts and Nevis", "圣基茨和尼维斯");
            add("lb", "لبنان  Lebanon", "Libanon", "Lebanon", "Liban", "Libano", "Líbano", "Líbano", "レバノン", "黎巴嫩");
            add("bt", "Bhutan", "Bhutan", "Bhutan", "Bhoutan", "Bhutan", "Bután", "Butão", "ブータン", "不丹");
            add("lt", "Lietuva", "Litauen", "Lithuania", "Lituanie", "Lituania", "Lituania", "Lietuva", "リトアニア", "立陶宛");
            add("mk", "Македонија", "Mazedonien", "Macedonia", "Macédoine", "Macedonia", "Macedonia", "Macedônia", "マケドニア", "马其顿");
            add("ao", "Angola", "Angola", "Angola", "Angola", "Angola", "Angola", "Angola", "アンゴラ", "安哥拉");
            add("pg", "Papua New Guinea", "Papua-Neuguinea", "Papua New Guinea", "Papouasie-Nouvelle-Guinée", "Papua Nuova Guinea", "Papúa Nueva Guinea", "Papua New Guinea", "Papua New Guinea", "巴布亚新几内亚");
            add("sd", "السودان ‎al-Sūdān", "Sudan", "Sudan", "Soudan", "Sudan", "Sudán", "Sudão", "スーダン", "苏丹");
            add("ms", "Montserrat", "Montserrat", "Montserrat", "Montserrat", "Montserrat", "Montserrat", "Montserrat", "Montserrat", "蒙特塞拉特");
            add("vn", "Việt Nam", "Vietnam", "Vietnam", "Viêt Nam", "Vietnam", "Vietnam", "Vietnã", "ベトナム", "越南");
            add("za", "South Africa", "Südafrika", "South Africa", "Afrique du Sud", "Sudafrica", "Sudáfrica", "África do Sul", "南アフリカ", "南非");
            add("sc", "Seychelles", "Seychellen", "Seychelles", "Seychelles", "Seychelles", "Seychelles", "Seychelles", "Seychelles", "塞舌尔群岛");
            add("ir", "ایران", "Iran", "Iran", "Iran", "Iran", "Irán", "Irã", "イラン", "伊朗");
            add("tr", "Türkiye", "Türkei", "Turkey", "Turquie", "Turchia", "Turquía", "Turquia", "トルコ", "土耳其");
            add("sz", "Swaziland", "Swasiland", "Swaziland", "Swaziland", "Swaziland", "Suazilandia", "Suazilândia", "スワジランド", "斯威士兰");
            add("ae", "United Arab Emirates", "Vereinigte Arabische Emirate", "United Arab Emirates", "Emirats Arabes Unis", "Emirati Arabi Uniti", "Emiratos Árabes Unidos", "Emirados Árabes Unidos", "アラブ首長国連邦", "阿拉伯联合酋长国");
            add("bn", "Brunei Darussalam", "Brunei Darussalam", "Brunei Darussalam", "Brunei", "Brunei", "Brunéi", "Brunei", "Brunei Darussalam", "文莱");
            add("cl", "Chile", "Chile", "Chile", "Chili", "Cile", "Chile", "Chile", "チリ", "智利");
            add("cv", "Cabo Verde", "Kap Verde", "Cape Verde", "Cap-Vert", "Capo Verde", "Cabo Verde", "Cabo Verde", "Cabo Verde", "佛得角");
            add("et", "Ethiopia", "Äthiopien", "Ethiopia", "Éthiopie", "Etiopia", "Etiopía", "Etiópia", "エチオピア", "埃塞俄比亚");
            add("mz", "Mozambique", "Mosambik", "Mozambique", "Mozambique", "Mozambico", "Mozambique", "Moçambique", "Mozambique", "莫桑比克");
            add("hr", "Hrvatska", "Kroatien", "Croatia", "Croatie", "Croazia", "Croacia", "Croácia", "クロアチア", "克罗地亚");
            add("py", "Paraguay", "Paraguay", "Paraguay", "Paraguay", "Paraguay", "Paraguay", "Paraguay", "パラグアイ", "巴拉圭");
            add("lk", "Sri Lanka", "Sri Lanka", "Sri Lanka", "Sri Lanka", "Sri Lanka", "Sri Lanka", "Sri Lanka", "スリランカ", "斯里兰卡");
            add("mv", "Maldives", "Malediven", "Maldives", "Maldives", "Maldive", "Maldivas", "Maldives", "Maldives", "马尔代夫");
            add("mw", "Malawi", "Malawi", "Malawi", "Malawi", "Malawi", "Malaui", "Malawi", "マラウイ", "马拉维");
            add("na", "Namibia", "Namibia", "Namibia", "Namibie", "Namibia", "Namibia", "Namibia", "Namibia", "纳米比亚");
            add("rw", "Rwanda", "Ruanda", "Rwanda", "Rwanda", "Ruanda", "Ruanda", "Rwanda", "ルワンダ", "卢旺达");
            add("sy", "Sūriyya سوريا", "Syrien", "Syria", "Syrie", "Siria", "Siria", "Síria", "シリア", "叙利亚");
            add("mh", "Marshall Islands", "Marshallinseln", "Marshall Islands", "Îles Marshall", "Isole Marshall", "Islas Marshall", "Ilhas Marshall", "Marshall Islands", "马绍尔群岛");
            add("va", "Città del Vaticano", "Vatikanstadt", "Vatican City", "Vatican", "Città del Vaticano", "Ciudad del Vaticano", "Cidade do Vaticano", "Città del Vaticano", "梵蒂冈");
            add("ve", "Venezuela", "Venezuela", "Venezuela", "Venezuela", "Venezuela", "Venezuela", "Venezuela", "ベネズエラ", "委内瑞拉");
            add("vg", "British Virgin Islands", "Britische Jungferninseln", "British Virgin Islands", "Îles Vierges britanniques", "Isole Vergini Britanniche", "Islas Vírgenes Británicas", "British Virgin Islands", "イギリス領ヴァージン諸島", "英属维尔京群岛");
            add("id", "Indonesia", "Indonesien", "Indonesia", "Indonésie", "Indonesia", "Indonesia", "Indonesia", "インドネシア", "印度尼西亚");
            add("se", "Sverige", "Schweden", "Sweden", "Suède", "Svezia", "Suecia", "Suécia", "スウェーデン", "瑞典");
            add("pn", "Pitcairn", "Pitcairn", "Pitcairn", "Îles Pitcairn", "Isole Pitcairn", "Islas Pitcairn", "Pitcairn", "Pitcairn", "皮特凯恩群岛");
            add("az", "Azerbaijan", "Aserbaidschan", "Azerbaijan", "Azerbaïdjan", "Azerbaigian", "Azerbaiyán", "Azerbaijão", "アゼルバイジャン", "阿塞拜疆");
            add("cy", "Κύπρος", "Zypern", "Cyprus", "Chypre", "Cipro", "Chipre", "Chipre", "キプロス", "塞浦路斯");
            add("sg", "Singapore", "Singapur", "Singapore", "Singapour", "Singapore", "Singapur", "Singapore", "Singapore", "新加坡");
            add("vu", "Vanuatu", "Vanuatu", "Vanuatu", "Vanuatu", "Vanuatu", "Vanuatu", "Vanuatu", "バヌアツ", "瓦努阿图");
            add("cn", "China 中国", "China", "China", "Chine", "Cina", "China", "China", "中国", "中国");
            add("cc", "Cocos (Keeling) Islands", "Kokosinseln", "Cocos (Keeling) Islands", "Îles Cocos", "Isole Cocos e Keeling", "Islas Cocos (Keeling)", "Cocos (Keeling) Islands", "Cocos (Keeling) Islands", "科科斯（基林）群島");
            add("bo", "Bolivia", "Bolivien", "Bolivia", "Bolivie", "Bolivia", "Bolivia", "Bolívia", "Bolivia", "玻利维亚");
            add("br", "Brasil", "Brasilien", "Brazil", "Brésil", "Brasile", "Brasil", "Brasil", "ブラジル", "巴西");
            add("bw", "Botswana", "Botsuana", "Botswana", "Botswana", "Botswana", "Botsuana", "Botswana", "ボツワナ", "博茨瓦纳");
            add("la", "Laos", "Laos", "Laos", "Laos", "Laos", "Laos", "Laos", "ラオス", "老挝");
            add("ee", "Eesti", "Estland", "Estonia", "Estonie", "Estonia", "Estonia", "Eesti", "エストニア", "爱沙尼亚");
            add("ke", "Kenya", "Kenia", "Kenya", "Kenya", "Kenya", "Kenia", "Kenya", "ケニア", "肯尼亚");
            add("tj", "Tajikistan", "Tadschikistan", "Tajikistan", "Tadjikistan", "Tagikistan", "Tayikistán", "Tadjiquistão", "Tajikistan", "塔吉克斯坦");
            add("tz", "Tanzania", "Tansania", "Tanzania", "Tanzanie", "Tanzania", "Tanzania", "Tanzânia", "タンザニア", "坦桑尼亚");
            add("ws", "Samoa", "Samoa", "Samoa", "Samoa", "Samoa", "Samoa", "Samoa", "Samoa", "萨摩亚");
            add("is", "Ísland", "Island", "Iceland", "Islande", "Islanda", "Islandia", "Ísland", "アイスランド", "冰岛");
            add("ru", "Россия", "Russland", "Russia", "Russie", "Россия", "Rusia", "Rússia", "ロシア", "俄罗斯");
            add("ls", "Lesotho", "Lesotho", "Lesotho", "Lesotho", "Lesotho", "Lesoto", "Lesotho", "レソト", "莱索托");
            add("om", "سلطنة عُمان Oman", "Oman", "Oman", "Oman", "Oman", "Omán", "Omã", "オマーン", "阿曼");
            add("gi", "Gibraltar", "Gibraltar", "Gibraltar", "Gibraltar", "Gibilterra", "Gibraltar", "Gibraltar", "Gibraltar", "直布罗陀");
            add("lc", "Saint Lucia", "St. Lucia", "Saint Lucia", "Sainte-Lucie", "Santa Lucia", "Santa Lucía", "Santa Lúcia", "セントルシア", "圣卢西亚岛");
            add("au", "Australia", "Australien", "Australia", "Australie", "Australia", "Australia", "Austrália", "オーストラリア", "澳大利亚");
            add("bg", "България", "Bulgarien", "Bulgaria", "Bulgarie", "Bulgaria", "Bulgaria", "Bulgária", "ブルガリア", "保加利亚");
            add("cm", "Cameroon;Cameroun", "Kamerun", "Cameroon", "Cameroun", "Camerun", "Camerún", "Camarões", "カメルーン", "喀麦隆");
            add("gd", "Grenada", "Grenada", "Grenada", "Grenade", "Grenada", "Granada", "Grenada", "グレナダ", "格林纳达");
            add("iq", "Iraq", "Irak", "Iraq", "Irak", "Iraq", "Iraq", "Iraq", "イラク", "伊拉克");
            add("mm", "Myanmar", "Myanmar", "Myanmar", "Birmanie", "Birmania", "Myanmar", "Myanmar", "Myanmar", "缅甸");
            add("mr", "موريتانيا", "Mauretanien", "Mauritania", "Mauritanie", "Mauritania", "Mauritania", "Mauritânia", "モーリタニア", "毛里塔尼亚");
            add("mu", "Mauritius", "Mauritius", "Mauritius", "Maurice", "Mauritius", "Mauricio", "Mauritius", "Mauritius", "毛里求斯");
            add("ni", "Nicaragua", "Nicaragua", "Nicaragua", "Nicaragua", "Nicaragua", "Nicaragua", "Nicaragua", "ニカラグア", "尼加拉瓜");
            add("pa", "Panama", "Panama", "Panama", "Panama", "Panamá", "Panamá", "Panama", "パナマ", "巴拿马");
            add("sm", "San Marino", "San Marino", "San Marino", "Saint-Marin", "San Marino", "San Marino", "San Marino", "San Marino", "圣马力诺");
            add("so", "Somalia / الصومال", "Somalia", "Somalia", "Somalie", "Somalia", "Somalia", "Somália", "ソマリア", "索马里");
            add("ug", "Uganda", "Uganda", "Uganda", "Ouganda", "Uganda", "Uganda", "Uganda", "ウガンダ", "乌干达");
            add("bd", "Bangladesh", "Bangladesch", "Bangladesh", "Bangladesh", "Bangladesh", "Bangladesh", "Bangladesh", "バングラデシュ", "孟加拉国");
            add("kh", "Cambodia", "Kambodscha", "Cambodia", "Cambodge", "Cambogia", "Cambodia", "Camboja", "カンボジア", "高棉");
            add("tl", "Timor-Leste; Timor Lorosa'e", "Osttimor", "East Timor", "Timor Oriental", "Timor Est", "Timor-Leste; Timor Lorosa'e", "Timor-Leste", "Timor-Leste; Timor Lorosa'e", "东帝汶");
            add("lv", "Latvija", "Lettland", "Latvia", "Lettonie", "Lettonia", "Letonia", "Latvija", "ラトビア", "拉脱维亚");
            add("mg", "Madagascar", "Madagaskar", "Madagascar", "Madagascar", "Madagascar", "Madagascar", "Madagascar", "マダガスカル", "马达加斯加");
            add("mt", "Malta", "Malta", "Malta", "Malte", "Malta", "Malta", "Malta", "マルタ", "马尔他");
            add("mx", "México", "Mexiko", "Mexico", "Mexique", "Messico", "México", "México", "メキシコ", "墨西哥");
            add("mn", "Монгол Улс", "Mongolei", "Mongolia", "Mongolie", "Mongolia", "Mongolia", "Mongólia", "モンゴル", "蒙古国");
            add("pl", "Polska", "Polen", "Poland", "Pologne", "Polonia", "Polonia", "Polska", "ポーランド", "波兰");
            add("vc", "Saint Vincent and the Grenadines", "Saint Vincent und die Grenadinen", "Saint Vincent and the Grenadines", "Saint-Vincent-et-les Grenadines", "Saint Vincent e Grenadine", "San Vicente y las Granadinas", "São Vicente e Granadinas", "セントビンセント及びグレナディーン諸島", "圣文森特和格林纳丁斯");
            add("ua", "Україна", "Ukraine", "Ukraine", "Ukraine", "Ucraina", "Ucrania", "Ucrânia", "ウクライナ", "乌克兰");
            add("uy", "Uruguay", "Uruguay", "Uruguay", "Uruguay", "Uruguay", "Uruguay", "Uruguai", "ウルグアイ", "乌拉圭");
            add("ai", "Anguilla", "Anguilla", "Anguilla", "Anguilla", "Anguilla", "Anguila", "Anguilla", "Anguilla", "安圭拉");
            add("bm", "Bermuda", "Bermuda", "Bermuda", "Bermudes", "Bermuda", "Bermuda", "Bermuda", "Bermuda", "百慕大");
            add("ag", "Antigua and Barbuda", "Antigua und Barbuda", "Antigua and Barbuda", "Antigua-et-Barbuda", "Antigua e Barbuda", "Antigua y Barbuda", "Antígua e Barbuda", "Antigua and Barbuda", "安提瓜和巴布达");
            add("bb", "Barbados", "Barbados", "Barbados", "Barbade", "Barbados", "Barbados", "Barbados", "バルバドス", "巴巴多斯");
            add("bs", "Bahamas", "Bahamas", "Bahamas", "Bahamas", "Bahamas", "Bahamas", "Bahamas", "Bahamas", "巴哈马");
            add("bz", "Belize", "Belize", "Belize", "Belize", "Belize", "Belice", "Belize", "ベリーズ", "伯利兹");
            add("dm", "Dominica", "Dominica", "Dominica", "Dominique", "Dominìca", "Dominica", "Dominica", "ドミニカ", "多明尼加");
            add("zm", "Zambia", "Sambia", "Zambia", "Zambie", "Zambia", "Zambia", "Zâmbia", "ザンビア", "赞比亚");
            add("fj", "Fiji", "Fidschi", "Fiji", "Fidji", "Figi", "Fiyi", "Fiji", "フィジー", "斐济岛");
            add("gm", "The Gambia", "Gambia", "The Gambia", "Gambie", "Gambia", "Gambia", "Gâmbia", "ガンビア", "冈比亚");
            add("gy", "Guyana", "Guyana", "Guyana", "Guyana", "Guyana", "Guyana", "Guiana", "ガイアナ", "圭亚那");
            add("jm", "", "Jamaika", "", "Jamaïque", "Giamaica", "", "", "ジャマイカ", "牙买加");
            add("ki", "Kiribati", "Kiribati", "Kiribati", "Kiribati", "Kiribati", "Kiribati", "Kiribati", "Kiribati", "基里巴斯");
            add("lr", "Liberia", "Liberia", "Liberia", "Liberia", "Liberia", "Liberia", "Liberia", "Liberia", "利比里亚");
            add("fm", "Micronesia", "Mikronesien", "Micronesia", "Micronésie", "Micronesia", "Micronesia", "Micronesia", "Micronesia", "密克罗尼西亚");
            add("ng", "Nigeria", "Nigeria", "Nigeria", "Nigeria", "Nigeria", "Nigeria", "Nigeria", "ナイジェリア", "尼日利亚");
            add("sl", "Sierra Leone", "Sierra Leone", "Sierra Leone", "Sierra Leone", "Sierra Leone", "Sierra Leona", "Serra Leoa", "Sierra Leone", "塞拉利昂");
            add("to", "Tonga", "Tonga", "Tonga", "Tonga", "Tonga", "Tonga", "Tonga", "Tonga", "汤加群岛");
            add("tt", "Trinidad and Tobago", "Trinidad und Tobago", "Trinidad and Tobago", "Trinité-et-Tobago", "Trinidad e Tobago", "Trinidad y Tobago", "Trinidad e Tobago", "トリニダード・トバゴ", "特立尼达和多巴哥");
            add("mq", "Martinique", "Martinique", "Martinique", "Martinique", "Martinica", "Martinica", "Martinique", "Martinique", "馬提尼克");
            add("zw", "Zimbabwe", "Simbabwe", "Zimbabwe", "Zimbabwe", "Zimbabwe", "Zimbabue", "Zimbábue", "ジンバブエ", "津巴布韦");
            add("bj", "Bénin", "Benin", "Benin", "Bénin", "Benin", "Benín", "Benin", "ベナン", "贝宁");
            add("bf", "Burkina Faso", "Burkina Faso", "Burkina Faso", "Burkina Faso", "Burkina Faso", "Burkina Faso", "Burkina Faso", "ブルキナファソ", "布基纳法索");
            add("ne", "Niger", "Niger", "Niger", "Niger", "Niger", "Níger", "Niger", "ニジェール", "尼日尔");
            add("cf", "Centrafrique", "Zentralafrikanische Republik", "Central African Republic", "Centrafrique", "Repubblica Centrafricana", "República Centroafricana", "República Centro-Africana", "Centrafrique", "中非");
            add("ga", "Gabon", "Gabun", "Gabon", "Gabon", "Gabon", "Gabón", "Gabão", "ガボン", "加蓬");
            add("ci", "Côte d'Ivoire", "Elfenbeinküste", "Côte d'Ivoire", "Côte d'Ivoire", "Costa d'Avorio", "Costa de Marfil", "Costa do Marfim", "コートジボワール", "科特迪瓦");
            add("cd", "République Démocratique du Congo", "Demokratische Republik Kongo", "Democratic Republic of the Congo", "République démocratique du Congo", "Repubblica Democratica del Congo", "República Democrática del Congo", "République Démocratique du Congo", "République Démocratique du Congo", "刚果民主共和国");
            add("cg", "République du Congo", "Republik Kongo", "Republic of the Congo", "République du Congo", "Repubblica del Congo", "República del Congo", "République du Congo", "République du Congo", "刚果共和国");
            add("gf", "Guyane Française", "Französisch-Guayana", "French Guiana", "Guyane française", "Guyana francese", "Guayana Francesa", "Guyane Française", "Guyane Française", "法属圭亚那");
            add("gp", "Guadeloupe", "Guadeloupe", "Guadeloupe", "Guadeloupe", "Guadalupa", "Guadalupe", "Guadeloupe", "Guadeloupe", "瓜德罗普");
            add("nc", "Nouvelle-Calédonie", "Neukaledonien", "New Caledonia", "Nouvelle-Calédonie", "Nuova Caledonia", "Nueva Caledonia", "Nouvelle-Calédonie", "Nouvelle-Calédonie", "新喀里多尼亚");
            add("re", "Réunion", "Réunion", "Réunion", "La Réunion", "Riunione", "La Reunión", "Réunion", "Réunion", "留尼汪");
            add("pm", "Saint-Pierre-et-Miquelon", "Saint-Pierre und Miquelon", "Saint Pierre and Miquelon", "Saint-Pierre-et-Miquelon", "Saint-Pierre-et-Miquelon", "San Pedro y Miguelón", "Saint-Pierre-et-Miquelon", "Saint-Pierre-et-Miquelon", "Saint-Pierre-et-Miquelon");
            add("sn", "Sénégal", "Senegal", "Senegal", "Sénégal", "Senegal", "Senegal", "Senegal", "セネガル", "塞内加尔");
            add("wf", "Wallis-et-Futuna", "Wallis und Futuna", "Wallis and Futuna Islands", "Wallis-et-Futuna", "Wallis e Futuna", "Wallis y Futuna", "Wallis e Futuna", "ウォリス・フツナ", "瓦利斯和富图纳群岛");
            add("gn", "Guinée", "Guinea", "Guinea", "Guinée", "Guinea", "Guinea", "Guiné", "Guinée", "几内亚");
            add("ml", "Mali", "Mali", "Mali", "Mali", "Mali", "Malí", "Mali", "マリ", "马里");
            add("mc", "Monaco", "Monaco", "Monaco", "Monaco", "Monaco", "Mónaco", "Monaco", "モナコ", "摩纳哥");
            add("tg", "Togo", "Togo", "Togo", "Togo", "Togo", "Togo", "Togo", "Togo", "多哥");
            add("ca", "Canada", "Kanada", "Canada", "Canada", "Canada", "Canadá", "Canadá", "カナダ", "加拿大");
            add("at", "Österreich", "Österreich", "Austria", "Autriche", "Austria", "Austria", "Österreich", "オーストリア", "奥地利");
            add("li", "Liechtenstein", "Liechtenstein", "Liechtenstein", "Liechtenstein", "Liechtenstein", "Liechtenstein", "Liechtenstein", "リヒテンシュタイン", "列支敦士登");
            add("ss", "South Sudan", "Südsudan", "South Sudan", "Sud-Soudan", "South Sudan", "Sudán del Sur", "South Sudan", "South Sudan", "南蘇丹");
            add("cw", "Curaçao", "Curaçao", "Curaçao", "Curaçao", "Curaçao", "Curazao", "Curaçao", "Curaçao", "Curaçao");
            add("sx", "Sint Maarten", "Sint Maarten", "Sint Maarten", "Sint Maarten", "Sint Maarten", "Sint Maarten", "Sint Maarten", "Sint Maarten", "Sint Maarten");
	}

    private static void add(String countryCode, String name, String de, String en, String fr, String it, String es, String pt, String ja, String zh) {
		CountryCode country = CountryCode.getByCode(countryCode);
		I18nName i18nName = new I18nName(name, de, en, fr, it, es, pt, ja, zh);
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
