package com.cuidadopet.domain

// Princípios ativos comuns no Brasil, para pessoas e pets (sem acento, minúsculas, uma palavra por item).
// Não é a lista oficial da DCB (Anvisa): é uma lista de referência para reconhecer nomes na receita.
// Para acrescentar um nome, inclua aqui.
internal object MedicationNamesList {

    val ALL: Set<String> = setOf(
        // Analgésicos, antitérmicos e anti-inflamatórios
        "acetaminofeno", "acetilsalicilico", "ambroxol", "bromoprida", "celecoxibe", "dexibuprofeno",
        "diclofenaco", "etoricoxibe", "flunixina", "indometacina", "lornoxicam", "mefenamico", "oxaprozina",
        "parecoxibe", "fentanila", "hidromorfona", "oxicodona", "petidina", "nalbufina", "tapentadol",
        "ketoprofeno", "ketorolaco", "maxicam", "tenoxicam", "cefalotina",

        // Corticoides
        "fluocinolona", "mometasona", "clobetasol", "deflazacorte", "cortisona", "fludrocortisona",

        // Antibióticos (penicilinas, cefalosporinas, macrolídeos, quinolonas, outros)
        "bacampicilina", "oxacilina", "dicloxacilina", "nafcilina", "piperacilina", "meropenem", "imipenem",
        "ertapenem", "cefuroxima", "cefotaxima", "cefepima", "cefixima", "cefpodoxima", "cefoperazona",
        "cefaclor", "cefotetana", "ceftazidima", "cefalexina", "espiramicina", "roxitromicina", "telitromicina",
        "norfloxacino", "ofloxacino", "moxifloxacino", "gatifloxacino", "pefloxacino", "danofloxacino",
        "difloxacino", "sarafloxacino", "orbifloxacino", "vancomicina", "teicoplanina", "linezolida",
        "tigeciclina", "minociclina", "tedizolida", "cloranfenicol", "colistina", "estreptomicina",
        "rifaximina", "isoniazida", "etambutol", "pirazinamida", "dapsona", "bacitracina", "mupirocina",
        "fusidico", "retapamulina", "sulfadoxina", "sulfassalazina", "sulfametazina", "fenazopiridina",
        "metenamina", "nitazoxanida", "tinidazol", "secnidazol", "furazolidona",

        // Antifúngicos e antivirais
        "anfotericina", "caspofungina", "micafungina", "voriconazol", "posaconazol", "isavuconazol",
        "flucitosina", "ciclopirox", "tioconazol", "econazol", "sertaconazol", "aciclovir", "valaciclovir",
        "famciclovir", "ganciclovir", "oseltamivir", "zidovudina", "lamivudina", "tenofovir", "efavirenz",
        "ribavirina", "sofosbuvir", "entecavir", "amantadina",

        // Antiparasitários e vermífugos (veterinários e humanos)
        "closantel", "clorsulon", "lufenuron", "spinosad", "imidacloprida", "dinotefuran", "pimetrozina",
        "diclorvos", "cipermetrina", "deltametrina", "bifentrina", "piriproxifeno", "metaflumizona",
        "tiabendazol", "oxantel", "emodepside", "triclabendazol", "clorquina", "cloroquina", "hidroxicloroquina",
        "mefloquina", "primaquina", "artemeter", "lumefantrina", "atovaquona", "proguanil", "quinina",
        "metronidazol", "benznidazol", "ivermectina",

        // Gastrointestinais
        "dexlansoprazol", "rabeprazol", "cimetidina", "nizatidina", "misoprostol", "bismuto", "pinavério",
        "pinaverio", "brometo", "otilonio", "trimebutina", "escopolamina", "pancreatina", "lactobacillus",
        "saccharomyces", "bifidobacterium", "racecadotrila", "ursodiol", "ursodesoxicolico", "colestiramina",
        "mesalazina", "budesonida", "prucaloprida", "linaclotida", "lubiprostona", "macrogol",
        "picossulfato", "polietilenoglicol", "racecadotril", "ondansetrona",
        "granisetrona", "dimenidrinato", "meclizina", "betaistina", "bromoprida", "trimebutina",

        // Cardiovasculares
        "bumetanida", "torsemida", "clortalidona", "indapamida", "manitol", "metolazona", "eplerenona",
        "amiodarona", "sotalol", "flecainida", "propafenona", "dronedarona", "mexiletina", "lidocaina",
        "adenosina", "nitroglicerina", "isossorbida", "hidralazina", "minoxidil", "metildopa", "clonidina",
        "doxazosina", "prazosina", "terazosina", "nifedipino", "felodipino", "verapamil", "nebivolol",
        "bisoprolol", "metoprolol", "pindolol", "labetalol", "sacubitril", "valsartana", "candesartana",
        "irbesartana", "olmesartana", "telmisartana", "azilsartana", "quinapril", "perindopril", "lisinopril",
        "fosinopril", "trandolapril", "moexipril", "cilazapril", "heparina", "enoxaparina", "dalteparina",
        "fondaparinux", "apixabana", "edoxabana", "ticagrelor", "prasugrel", "cilostazol", "dipiridamol",
        "rosuvastatina", "pravastatina", "lovastatina", "fluvastatina", "pitavastatina",
        "ezetimiba", "fenofibrato", "gemfibrozila", "bempedoico", "icosapent", "dobutamina", "dopamina",
        "milrinona", "levosimendana", "ivabradina", "ranolazina", "vericiguate", "digitoxina",
        "tadalafila", "vardenafila", "bosentana", "ambrisentana", "macitentana", "riociguate",

        // Metabolismo, diabetes e hormônios
        "sitagliptina", "vildagliptina", "linagliptina", "saxagliptina", "alogliptina", "empagliflozina",
        "dapagliflozina", "canagliflozina", "ertugliflozina", "liraglutida", "semaglutida", "dulaglutida",
        "exenatida", "tirzepatida", "pioglitazona", "rosiglitazona", "acarbose", "repaglinida", "nateglinida",
        "gliclazida", "glipizida", "tolbutamida", "clorpropamida", "insulina", "glargina", "detemir",
        "asparte", "lispro", "glulisina", "degludeca", "levotiroxina", "liotironina", "propiltiouracila",
        "carbimazol", "tiroxina", "estradiol", "estriol", "progesterona", "didrogesterona", "medroxiprogesterona",
        "noretisterona", "levonorgestrel", "etinilestradiol", "drospirenona", "desogestrel", "gestodeno",
        "norgestimato", "ciproterona", "clomifeno", "letrozol", "anastrozol", "exemestano", "tamoxifeno",
        "testosterona", "nandrolona", "oxandrolona", "finasterida", "dutasterida", "tansulosina", "alfuzosina",
        "silodosina", "oxibutinina", "tolterodina", "solifenacina", "darifenacina", "trospio", "mirabegrona",
        "vibegrona", "cabergolina", "bromocriptina", "octreotida", "lanreotida", "somatropina", "calcitonina",
        "alendronato", "risedronato", "ibandronato", "zoledronato", "denosumabe", "teriparatida", "raloxifeno",
        "paricalcitol", "calcitriol", "alfacalcidol", "cinacalcete", "sevelamer", "carbonato", "calcio",
        "magnesio", "potassio", "sodio", "fosforo", "zinco", "selenio", "iodo", "cobre",

        // Vitaminas e suplementos
        "tiamina", "riboflavina", "piridoxina", "niacina", "nicotinamida", "biotina", "acido", "ascorbico",
        "retinol", "vitamina", "tocoferol", "ergocalciferol", "calciferol", "complexo", "carnitina",
        "coenzima", "glutamina", "arginina", "taurina", "omega", "dha", "epa", "probiotico",

        // Sistema nervoso e comportamento
        "amissulprida", "aripiprazol", "asenapina", "clozapina", "haloperidol", "levomepromazina",
        "olanzapina", "paliperidona", "quetiapina", "risperidona", "ziprasidona", "lurasidona", "pimozida",
        "tioridazina", "clorpromazina", "flufenazina", "perfenazina", "lítio", "litio", "valproato",
        "divalproato", "lamotrigina", "topiramato", "zonisamida", "oxcarbazepina", "carbamazepina",
        "fenitoina", "etossuximida", "lacosamida", "brivaracetam", "perampanel", "rufinamida", "vigabatrina",
        "prebalina", "alprazolam", "bromazepam", "lorazepam", "oxazepam", "clordiazepoxido", "zolpidem",
        "zopiclona", "eszopiclona", "zaleplona", "buspirona", "hidroxizina", "bupropiona", "venlafaxina",
        "desvenlafaxina", "duloxetina", "milnaciprano", "citalopram", "escitalopram", "vortioxetina",
        "nortriptilina", "imipramina", "doxepina", "moclobemida", "selegilina", "rasagilina", "safinamida",
        "levodopa", "carbidopa", "benserazida", "entacapona", "pramipexol", "ropinirol", "biperideno",
        "triexifenidil", "donepezila", "rivastigmina", "galantamina", "memantina", "riluzol", "baclofeno",
        "tizanidina", "ciclobenzaprina", "metocarbamol", "carisoprodol", "dantroleno", "pridostigmina",
        "fisostigmina", "atomoxetina", "metilfenidato", "lisdexanfetamina", "anfetamina", "modafinila",
        "armodafinila", "cafeina", "naltrexona", "acamprosato", "dissulfiram", "varenicline", "nicotina",
        "xilazina", "tiletamina", "zolazepam", "cetamina", "propofol", "etomidato", "tiopental",
        "romifidina", "detomidina", "medetomidina", "diazepan", "azaperona", "maropitant", "gabapentin",

        // Anestésicos locais e analgésicos adicionais
        "articaina", "mepivacaina", "prilocaina", "ropivacaina", "levobupivacaina", "benzocaina",
        "tetracaina", "proparacaina", "oxibuprocaina", "meperidina", "pentazocina", "nalbufina",

        // Antialérgicos, respiratórios e oftalmológicos
        "bilastina", "fexofenadina", "rupatadina", "ebastina", "azelastina", "olopatadina", "epinastina",
        "ciclesonida", "flunisolida", "formoterol", "salmeterol", "vilanterol", "indacaterol", "tiotropio",
        "ipratropio", "aclidinio", "umeclidinio", "teofilina", "aminofilina", "roflumilaste", "acetilcisteina",
        "carbocisteina", "bromexina", "guaifenesina", "dextrometorfano", "codeina", "benzonatato", "levodropropizina",
        "pseudoefedrina", "fenilefrina", "oximetazolina", "xilometazolina", "tramazolina", "nafazolina",
        "tobramicina", "levofloxacino", "gatifloxacino", "moxifloxacino", "ciclosporina", "tacrolimo",
        "pilocarpina", "timolol", "latanoprosta", "bimatoprosta", "travoprost", "tafluprost", "brinzolamida",
        "dorzolamida", "brimonidina", "apraclonidina", "ciclopentolato", "tropicamida", "atropina",
        "cromoglicato", "lifitegrast", "carmelose", "hialuronato", "polivinilico", "tetrahidrozolina",

        // Dermatológicos
        "ácido", "acido", "adapaleno", "tretinoína", "tretinoina", "isotretinoina", "acitretina", "tacrolimus",
        "pimecrolimus", "crisarobina", "calcipotriol", "tazaroteno", "ivermectina", "enxofre", "resorcina",
        "undecilenico", "piritionato", "tioconazol", "bifonazol", "fenticonazol", "oxiconazol",

        // Hematológicos, imunológicos e outros
        "ferro", "eritropoetina", "epoetina", "darbepoetina", "filgrastim", "pegfilgrastim", "tranexamico",
        "aminocaproico", "protamina", "vitamina", "fitomenadiona", "desmopressina", "vasopressina",
        "azatioprina", "micofenolato", "metotrexato", "leflunomida", "hidroxiureia", "ciclofosfamida",
        "clorambucila", "melfalano", "busulfano", "mercaptopurina", "tioguanina", "vincristina",
        "vimblastina", "doxorrubicina", "epirubicina", "daunorrubicina", "idarubicina", "mitoxantrona",
        "cisplatina", "carboplatina", "oxaliplatina", "paclitaxel", "docetaxel", "cabazitaxel", "etoposide",
        "irinotecano", "topotecano", "gencitabina", "fluorouracil", "capecitabina", "citarabina", "fludarabina",
        "cladribina", "bortezomibe", "lenalidomida", "talidomida", "imatinibe", "dasatinibe", "nilotinibe",
        "sunitinibe", "sorafenibe", "pazopanibe", "erlotinibe", "gefitinibe", "lapatinibe", "tamoxifeno",
        "bicalutamida", "flutamida", "enzalutamida", "abiraterona", "megestrol", "ondansetrona",
        "dexrazoxano", "mesna", "leucovorina", "levocarnitina", "allopurinol", "febuxostate", "probenecida",
        "colchicina", "prednisona", "penicilamina", "trientina", "deferoxamina", "deferasirox", "quelante",

        // Antídotos e outros frequentes em emergência
        "naloxona", "flumazenil", "acetilcisteina", "carvao", "glucagon", "octreotida", "hidroxocobalamina",
        "pralidoxima", "atropina", "fomepizol", "dimercaprol", "edetato", "succinilcolina",
        "rocuronio", "vecuronio", "atracurio", "cisatracurio", "pancuronio", "neostigmina", "sugammadex",

        // Veterinários adicionais (pets)
        "meloxicam", "robenacoxibe", "grapiprant", "mavacoxibe", "firocoxibe", "tepoxalina", "tolfenamico",
        "carprofeno", "cetorolac", "buprenorfina", "metadona", "fentanil", "nalbufina", "dipirona", "clindamicina",
        "cefovecina", "pradofloxacino", "enrofloxacino", "amoxicilina", "clavulanato", "trimetoprim",
        "selamectina", "milbemicina", "emodepsida", "praziquantel", "epsiprantel", "fenbendazol", "oxibendazol",
        "tiamulina", "florfenicol", "tulatromicina", "gamitromicina", "tilmicosina", "tildipirosina",
        "marbofloxacina", "pradofloxacina", "cefquinoma", "ceftiofur", "oxitetraciclina", "tetracicilina",
        "pirimetamina", "toltrazurila", "nitroscanato", "lufenurom", "metoclopramida", "cimetidina",
        "sucralfato", "maropitanto", "vetmedin", "benazepril", "pimobendana", "espironolactona", "furosemida",
        "torasemida", "amlodipina", "atenolol", "sildenafila", "dexmedetomidina", "butorfanol", "tramadol",
        "trilostano", "mitotano", "ketamina", "bupivacaina", "xilocaina"
    )
}
