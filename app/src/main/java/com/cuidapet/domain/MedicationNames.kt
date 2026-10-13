package com.cuidadopet.domain

import java.text.Normalizer
import java.util.Locale

// Reconhece se um nome extraído da receita parece ser de medicamento.
// Usa uma lista de princípios ativos comuns (humanos e pets) e sufixos típicos de classes farmacológicas (convenção INN).
// Não é lista oficial: o que não for reconhecido continua aparecendo, mas desmarcado, para o tutor confirmar.
object MedicationNames {

    // Sufixos de classes farmacológicas (sem acento, minúsculas)
    private val suffixes = listOf(
        "cilina", "cillin", "micina", "ciclina", "floxacina", "oxacina",
        "azol", "prazol", "olol", "pril", "sartana", "statina", "epam", "dipina", "dipino", "mab"
    )

    // Princípios ativos comuns (sem acento, minúsculas)
    private val known = setOf(
        // Analgésicos e anti-inflamatórios
        "dipirona", "metamizol", "paracetamol", "ibuprofeno", "diclofenaco", "meloxicam", "carprofeno",
        "firocoxibe", "cetoprofeno", "nimesulida", "piroxicam", "naproxeno", "cetorolaco", "tramadol",
        "codeina", "morfina", "gabapentina", "pregabalina", "amitriptilina", "aspirina", "butorfanol",
        "buprenorfina", "metadona",
        // Corticoides
        "prednisona", "prednisolona", "dexametasona", "betametasona", "metilprednisolona", "hidrocortisona",
        "triancinolona", "budesonida", "beclometasona", "fluticasona",
        // Antibióticos
        "amoxicilina", "clavulanato", "ampicilina", "penicilina", "benzilpenicilina", "cefalexina",
        "cefadroxila", "ceftriaxona", "cefazolina", "cefovecina", "enrofloxacino", "marbofloxacino",
        "ciprofloxacino", "levofloxacino", "azitromicina", "claritromicina", "eritromicina", "doxiciclina",
        "tetraciclina", "oxitetraciclina", "clindamicina", "lincomicina", "sulfametoxazol", "trimetoprima",
        "sulfadiazina", "metronidazol", "nitrofurantoina", "gentamicina", "amicacina", "neomicina",
        "polimixina", "fosfomicina", "tobramicina", "rifampicina", "tilosina", "tiamulina",
        // Antifúngicos
        "cetoconazol", "fluconazol", "itraconazol", "griseofulvina", "miconazol", "clotrimazol",
        "nistatina", "terbinafina",
        // Antiparasitários e vermífugos
        "ivermectina", "albendazol", "mebendazol", "praziquantel", "pirantel", "fenbendazol", "febantel",
        "levamisol", "selamectina", "moxidectina", "fipronil", "sarolaner", "afoxolaner", "permetrina",
        "nitenpiram", "toltrazuril", "sulfaquinoxalina", "niclosamida", "piperazina", "milbemicina",
        // Gastrointestinais
        "omeprazol", "pantoprazol", "esomeprazol", "lansoprazol", "ranitidina", "famotidina",
        "metoclopramida", "ondansetrona", "maropitant", "sucralfato", "bisacodil", "lactulose", "lactulona",
        "loperamida", "domperidona", "simeticona", "hioscina", "butilbrometo", "dimeticona",
        // Cardiovasculares e renais
        "furosemida", "espironolactona", "hidroclorotiazida", "enalapril", "benazepril", "captopril",
        "ramipril", "losartana", "atenolol", "propranolol", "carvedilol", "pimobendan", "digoxina",
        "amlodipino", "diltiazem", "clopidogrel", "varfarina", "rivaroxabana", "dabigatrana",
        "atorvastatina", "sinvastatina", "sildenafil", "pentoxifilina",
        // Neurológicos e comportamento
        "fenobarbital", "fenobarbitona", "levetiracetam", "diazepam", "midazolam", "clonazepam",
        "fluoxetina", "sertralina", "paroxetina", "trazodona", "clomipramina", "mirtazapina",
        "dexmedetomidina", "acepromazina", "melatonina",
        // Hormônios e metabolismo
        "levotiroxina", "metimazol", "insulina", "cabergolina", "desmopressina", "alopurinol",
        "colchicina", "metformina", "glibenclamida", "glimepirida",
        // Anti-histamínicos e respiratórios
        "loratadina", "desloratadina", "cetirizina", "dexclorfeniramina", "prometazina", "salbutamol",
        "montelucaste", "difenidramina", "hidroxizina", "clorfeniramina",
        // Vitaminas e suplementos
        "colecalciferol", "cianocobalamina", "fitomenadiona", "folico", "ferroso",
        "vitamina", "clorexidina", "lidocaina", "bupivacaina"
    )

    // Lista base + lista ampliada, normalizadas (sem acento) para casar com o texto da receita
    private val knownNormalized: Set<String> by lazy {
        (known + MedicationNamesList.ALL + MedicationNamesCatalog.ALL).map { normalize(it) }.toSet()
    }

    fun isLikelyMedication(name: String): Boolean {
        val normalized = normalize(name)
        if (normalized.isEmpty()) return false
        // Um nome composto ("Amoxicilina + Clavulanato") basta uma parte reconhecida
        return normalized.split(Regex("[\\s+/,-]+")).filter { it.isNotEmpty() }.any { token ->
            token in knownNormalized || (token.length >= 6 && suffixes.any { token.endsWith(it) })
        }
    }

    private fun normalize(name: String): String =
        Normalizer.normalize(name.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .trim()
}
