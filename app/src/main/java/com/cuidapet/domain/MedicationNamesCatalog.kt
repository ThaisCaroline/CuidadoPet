package com.cuidadopet.domain

// Princípios ativos extraídos do catálogo da Cliquefarma (índice por letra, primeira página de cada letra).
// Só os nomes base, sem sais (ex.: "Besilato de Anlodipino" → "anlodipino"). Sem acento, minúsculas.
// Não é lista oficial da DCB; é uma fonte de apoio para reconhecer nomes na receita.
internal object MedicationNamesCatalog {

    val ALL: Set<String> = setOf(
        // A
        "abatacepte", "abemaciclibe", "acalabrutinibe", "acebrofilina", "aceclofenaco", "abiraterona",
        "bazedoxifeno", "busserrelina", "cetrorrelix", "ciproterona", "clormadinona", "clostebol",
        "degarelix", "fludrocortisona", "fluormetolona", "ganirrelix", "glatiramer", "gosserrelina",
        "icatibanto", "leuprorrelina", "nomegestrol", "medroxiprogesterona",

        // B
        "bacitracina", "baclofeno", "baricitinibe", "belimumabe", "bemiparina", "benfotiamina",
        "benralizumabe", "benzbromarona", "benzilpenicilina", "alogliptina", "rizatriptana", "oxomemazina",
        "guaifenesina", "benzocaina", "clioquinol", "prednisolona", "cetilpiridinio", "dequalinio",
        "triclosana", "tirotricina", "benzoilmetronidazol", "benzalconio", "nistatina",
        "anlodipino", "benazepril", "bisoprolol", "cisatracurio", "levanlodipino",

        // C
        "cabazitaxel", "cabergolina", "cafeina", "carisoprodol", "orfenadrina", "dipirona", "paracetamol",
        "ciclobenzaprina", "metoclopramida", "dihidroergotamina", "clorfeniramina", "calcipotriol",
        "calcitriol", "calendula", "mebendazol", "cambendazol", "canabidiol", "tetraidrocanabinol",
        "canagliflozina", "canaquinumabe", "candesartana", "felodipino", "hidroclorotiazida",
        "procaina", "cloranfenicol",

        // D
        "dacarbazina", "daclatasvir", "dalteparina", "danazol", "dantroleno", "dapagliflozina",
        "saxagliptina", "daptomicina", "darolutamida", "darunavir", "dasatinibe", "nandrolona",
        "testosterona", "deferasirox", "deferiprona", "defibrotida", "deflazacorte", "delafloxacino",
        "deltafolitropina", "deltametrina", "denosumabe", "desloratadina", "pseudoefedrina", "desogestrel",
        "etinilestradiol", "desonida", "gentamicina", "desoximetasona", "deutetrabenazina", "dexametasona",
        "clemastina", "neomicina", "tobramicina", "dexlansoprazol", "dexpantenol", "dextrana", "hipromelose",
        "dextrometorfano", "diacereina", "pasireotida", "diazepam", "diazoxido",

        // E
        "ebastina", "edoxabana", "efavirenz", "efedrina", "teofilina", "elbasvir", "grazoprevir",
        "elotuzumabe", "elranatamabe", "eltrombopague", "pirvinio", "empagliflozina", "linagliptina",
        "noretisterona", "estradiol", "enoxaparina", "entacapona", "entecavir", "escina", "nintedanibe",
        "esomeprazol", "naproxeno", "espiramicina", "espironolactona", "furosemida", "estavudina",
        "estazolam", "eritromicina", "estreptoquinase", "estriol", "estrogenios", "eszopiclona", "loteprednol",

        // F
        "famotidina", "fampridina", "fanciclovir", "fenilbutazona", "fenitoina", "fenobarbital",
        "fenofibrato", "fenoximetilpenicilina", "fentanila", "ferripolimaltose", "ferrocarbonila",
        "fibrinogenio", "filgrastim", "finasterida", "doxazosina", "fitomenadiona", "fluconazol",
        "fludroxicortida", "flumazenil", "flunitrazepam", "fluocinolona", "hidroquinona", "tretinoina",
        "fluoruracila", "flurazepam", "flurbiprofeno", "flutamida", "flutrimazol", "fluvastatina",
        "folinato", "fondaparinux", "fosamprenavir", "fosaprepitanto", "clindamicina", "codeina",

        // G
        "gabapentina", "galcanezumabe", "galsulfase", "ganciclovir", "gatifloxacino", "gefitinibe",
        "genfibrozila", "gestodeno", "givosirana", "glecaprevir", "pibrentasvir", "glibenclamida",
        "gliclazida", "glimepirida", "glipizida", "glucagon", "golimumabe", "gonadotropina",
        "griseofulvina", "guselcumabe",

        // I
        "ibandronato", "ibrutinibe", "ibuprofeno", "idarucizumabe", "ifosfamida", "iloprosta",
        "imiquimode", "indacaterol", "indapamida", "perindopril", "indometacina", "infliximabe", "insulina",
        "inotersena", "inotuzumabe", "iodopovidona", "ipilimumabe", "irbesartana", "isoniazida",
        "isotretinoina", "itraconazol", "ivabradina", "ivacaftor",

        // L
        "lacidipino", "lacosamida", "lactulose", "lamivudina", "zidovudina", "lamotrigina", "lanadelumabe",
        "lansoprazol", "laronidase", "larotrectinibe", "latanoprosta", "timolol", "ledipasvir", "sofosbuvir",
        "leflunomida", "lenalidomida", "lesinurade", "letermovir", "letrozol", "levetiracetam",
        "levodropropizina", "levofloxacino", "cabozantinibe", "levomepromazina", "levonorgestrel",
        "levotiroxina", "lidocaina", "tetracaina", "limeciclina", "linezolida", "lipegfilgrastim",
        "liraglutida", "lisinato", "lisinopril", "lixisenatida", "lomustina", "lopinavir", "ritonavir",

        // M
        "magaldrato", "simeticona", "sunitinibe", "asenapina", "carbinoxamina", "dexbronfeniramina",
        "dexclorfeniramina", "enalapril", "fluvoxamina", "midazolam", "trimebutina", "manitol", "maraviroque",
        "tiabendazol", "ingenol", "mecobalamina", "melfalana", "meloxicam", "menotropina", "mepolizumabe",
        "mercaptopurina", "meropenem", "mesalazina", "codergocrina", "dabrafenibe", "desferroxamina",
        "gemifloxacino", "imatinibe", "lenvatinibe", "osimertinibe", "rasagilina", "safinamida", "mesna",

        // N
        "nafazolina", "sumatriptana", "natalizumabe", "nateglinida", "nepafenaco", "nicergolina", "nicotina",
        "nifedipino", "nimesulida", "pantoprazol", "nimodipino", "nimotuzumabe", "nirsevimabe", "nitazoxanida",
        "nitisinona", "butoconazol", "fenticonazol", "isoconazol", "miconazol", "oxiconazol", "nitrazepam",
        "nitrendipino", "nitrofural", "nitrofurantoina", "nitroglicerina", "nitroprusseto", "nivolumabe",
        "norfloxacino",

        // O
        "obinutuzumabe", "ocitocina", "ocrelizumabe", "ofatumumabe", "ofloxacino", "olanzapina", "olaparibe",
        "olmesartana", "omalizumabe", "ombitasvir", "dasabuvir", "veruprevir", "omeprazol", "onasemnogeno",
        "orlistate", "escitalopram", "oxaliplatina", "oxcarbazepina", "oximetolona",

        // P
        "paclitaxel", "palbociclibe", "paliperidona", "palivizumabe", "pamidronato", "imipramina",
        "pancreatina", "panitumumabe", "paracalcitol", "parecoxibe", "pegcetacoplana", "pegvisomanto",
        "pembrolizumabe", "pemetrexede", "penciclovir", "penicilamina", "pentoxifilina", "perampanel",
        "periciazina", "permetrina", "pertuzumabe", "trastuzumabe", "picossulfato",
        "piridoxina", "pimecrolimo", "pimozida",

        // R
        "rabeprazol", "racecadotrila", "silimarina", "raltegravir", "ramelteona", "ramipril",
        "ranibizumabe", "ranolazina", "ravulizumabe", "regorafenibe", "repaglinida",
        "retapamulina", "ribavirina", "rifamicina", "rifampicina", "rifaximina", "rilmenidina", "riluzol",
        "riociguate", "risanquizumabe", "risdiplam", "risedronato", "risperidona", "rituximabe",
        "rivaroxabana", "rivastigmina", "roflumilaste", "romiplostim", "romosozumabe", "rosuvastatina",
        "rotigotina", "rufinamida",

        // S
        "boulardii", "saccharomyces", "sacubitril", "valsartana", "secnidazol",
        "tinidazol", "tioconazol", "selexipague", "secuquinumabe", "semaglutida", "sinvastatina", "sirolimo",
        "sofosbuvir", "velpatasvir", "voxilaprevir", "simeprevir", "somatropina", "desvenlafaxina",
        "solifenacina", "sucralfato", "sugamadex", "sulbutiamina", "trolamina", "sulfadiazina",
        "sulfametoxazol", "trimetoprima", "sulfassalazina",

        // T
        "tacrolimo", "tadalafila", "tafamidis", "tafluprosta", "brimonidina", "tolterodina",
        "vareniclina", "teclozana", "teduglutida", "tegaserode", "teicoplanina", "telmisartana",
        "temozolomida", "teniposideo", "tenofovir", "tenoxicam", "teriflunomida", "teriparatida", "tiamazol",
        "tianeptina", "tianfenicol", "tibolona", "ticagrelor", "tigeciclina", "timomodulina", "tiocolchicosideo",
        "tioguanina", "tipiracila", "trifluridina", "tipranavir", "tirzepatida", "tocilizumabe", "tolcapona",

        // V
        "diflucortolona", "valganciclovir", "valproato", "vandetanibe", "varfarina", "vedolizumabe",
        "vemurafenibe", "venetoclax", "verteporfina", "vigabatrina", "vildagliptina", "vimpocetina",
        "vismodegibe", "vonoprazana", "voretigeno", "voriconazol"
    )
}
