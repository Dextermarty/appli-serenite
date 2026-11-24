package be.dexter.serenite

/** Questionnaire d'auto-évaluation validé. */
data class Questionnaire(
    val id: String,
    val name: String,
    val short: String,
    val intro: String,
    val answers: List<String>,
    val values: List<Int>,
    val items: List<String>,
    val mean: Boolean, // score = moyenne (BAT) ou somme (PHQ, GAD)
    val max: Float,
    val bands: List<Pair<Float, String>>, // seuil minimal → libellé, par ordre croissant
    val source: String,
    val subscales: List<Pair<String, List<Int>>> = emptyList(),
) {
    fun score(ans: List<Int>): Float = if (mean) ans.sum().toFloat() / ans.size else ans.sum().toFloat()
    fun band(score: Float): Int = bands.indexOfLast { score >= it.first }.coerceAtLeast(0)
}

data class Theme(
    val id: String,
    val title: String,
    val emoji: String,
    val desc: String,
    val programs: List<String>,
    val tools: List<Pair<String, Route>>, // libellé → écran
    val tests: List<String>,
    val habits: List<String>,
)

data class PlanSection(val id: String, val title: String, val hint: String)

object Library {

    // ---------------- Questionnaires ----------------

    val bat12 = Questionnaire(
        "bat12", "Épuisement professionnel (BAT-12)", "Burn-out",
        "Les énoncés suivants portent sur ton vécu au travail. Indique à quelle fréquence chacun s'applique à toi.",
        listOf("Jamais", "Rarement", "Parfois", "Souvent", "Toujours"), listOf(1, 2, 3, 4, 5),
        listOf(
            "Au travail, je me sens épuisé(e) mentalement.",
            "Après une journée au travail, il m'est difficile de retrouver mon énergie.",
            "Au travail, je me sens épuisé(e) physiquement.",
            "J'ai du mal à être enthousiaste à propos de mon travail.",
            "Je ressens une profonde aversion pour mon travail.",
            "Je ne pense pas que les autres accordent de l'importance à mon travail.",
            "Au travail, j'ai du mal à rester attentif(ve).",
            "J'oublie des choses et je suis distrait(e) au travail.",
            "Je fais des erreurs dans mon travail car mon esprit est ailleurs.",
            "Au travail, je me sens incapable de contrôler mes émotions.",
            "Je ne me reconnais pas dans mes réactions émotionnelles au travail.",
            "Au travail, il m'arrive de réagir avec excès sans en avoir l'intention.",
        ),
        mean = true, max = 5f,
        bands = listOf(0f to "Pas de risque particulier", 2.54f to "Risque de burn-out", 2.96f to "Burn-out sévère probable"),
        source = "Burnout Assessment Tool, version courte (Schaufeli, De Witte & Desart, KU Leuven). Seuils belges (Flandre) : 2,54 risque, 2,96 sévère (Schaufeli et al., 2023, seuils préliminaires).",
        subscales = listOf(
            "Épuisement" to listOf(0, 1, 2),
            "Distance mentale" to listOf(3, 4, 5),
            "Difficultés cognitives" to listOf(6, 7, 8),
            "Difficultés émotionnelles" to listOf(9, 10, 11),
        ),
    )

    private val freq = listOf("Jamais", "Plusieurs jours", "Plus de la moitié des jours", "Presque tous les jours")

    val phq9 = Questionnaire(
        "phq9", "Humeur (PHQ-9)", "Humeur",
        "Au cours des 14 derniers jours, à quelle fréquence les problèmes suivants t'ont-ils perturbé ?",
        freq, listOf(0, 1, 2, 3),
        listOf(
            "Peu d'intérêt ou de plaisir à faire les choses",
            "Être triste, déprimé(e) ou désespéré(e)",
            "Difficultés à s'endormir ou à rester endormi(e), ou dormir trop",
            "Se sentir fatigué(e) ou manquer d'énergie",
            "Avoir peu d'appétit ou manger trop",
            "Avoir une mauvaise opinion de soi-même, ou avoir le sentiment d'être nul(le), ou d'avoir déçu sa famille ou s'être déçu(e) soi-même",
            "Avoir du mal à se concentrer, par exemple pour lire le journal ou regarder la télévision",
            "Bouger ou parler si lentement que les autres auraient pu le remarquer. Ou au contraire, être si agité(e) que vous avez eu plus de mal que d'habitude à tenir en place",
            "Penser qu'il vaudrait mieux mourir ou envisager de vous faire du mal d'une manière ou d'une autre",
        ),
        mean = false, max = 27f,
        bands = listOf(0f to "Minimal", 5f to "Léger", 10f to "Modéré", 15f to "Modérément sévère", 20f to "Sévère"),
        source = "Patient Health Questionnaire (Spitzer, Williams, Kroenke et coll., bourse Pfizer). Libre de reproduction.",
    )

    val gad7 = Questionnaire(
        "gad7", "Anxiété (GAD-7)", "Anxiété",
        "Au cours des 14 derniers jours, à quelle fréquence les problèmes suivants t'ont-ils perturbé ?",
        freq, listOf(0, 1, 2, 3),
        listOf(
            "Un sentiment de nervosité, d'anxiété ou de tension",
            "Une incapacité à arrêter de s'inquiéter ou à maîtriser ses inquiétudes",
            "Une inquiétude excessive à propos de différentes choses",
            "Des difficultés à se détendre",
            "Une agitation telle qu'il est difficile de tenir en place",
            "Une tendance à être facilement contrarié(e) ou irritable",
            "Un sentiment de peur, comme si quelque chose de terrible risquait de se produire",
        ),
        mean = false, max = 21f,
        bands = listOf(0f to "Minimal", 5f to "Léger", 10f to "Modéré", 15f to "Sévère"),
        source = "Generalized Anxiety Disorder-7 (Spitzer, Williams, Kroenke et coll., bourse Pfizer), version française pour la Belgique. Libre de reproduction.",
    )

    val tests = listOf(bat12, phq9, gad7)
    fun test(id: String) = tests.firstOrNull { it.id == id }

    // ---------------- Plan anti-rechute ----------------

    val planSections = listOf(
        PlanSection("signaux", "Mes signaux d'alerte précoces",
            "Ce qui change chez moi quand ça commence à déraper : sommeil, irritabilité, cynisme, oublis, tensions physiques, besoin de travailler le soir…"),
        PlanSection("declencheurs", "Mes situations à risque",
            "Périodes, tâches, relations ou rythmes qui me poussent au-delà de mes limites."),
        PlanSection("ressources", "Ce qui me recharge vraiment",
            "Activités, lieux, personnes qui me font récupérer (pas seulement me distraire)."),
        PlanSection("limites", "Mes limites non négociables",
            "Ex. : pas de mails après une heure fixe, pause de midi réelle, pas plus de X heures sup' par semaine."),
        PlanSection("actions", "Ce que je fais dès les premiers signaux",
            "Actions concrètes, petites et immédiates : en parler, alléger l'agenda, reposer une limite, reprendre un exercice."),
        PlanSection("personnes", "Les personnes à prévenir",
            "Proches, collègue de confiance, médecin traitant, médecin du travail, psychologue… avec leurs coordonnées."),
        PlanSection("stop", "Quand je consulte sans attendre",
            "Seuils clairs : ex. 2 semaines de mauvais sommeil, score BAT dans le rouge, pleurs fréquents, idées noires → je prends rendez-vous."),
    )

    // ---------------- Exercices guidés supplémentaires ----------------

    val pause3 = listOf(
        GuidedStep("S'arrêter", "Redresse-toi, pose les pieds au sol. Demande-toi : qu'est-ce qui se passe en moi, maintenant ? Pensées, émotions, sensations : observe-les sans les changer.", 50),
        GuidedStep("Se rassembler", "Ramène toute ton attention sur la respiration, au niveau du ventre. Suis chaque inspiration et chaque expiration.", 60),
        GuidedStep("S'élargir", "Élargis l'attention à tout le corps, à la posture, à l'espace autour de toi. Puis reprends tes activités avec cette présence un peu plus large.", 60),
    )

    val safePlace = listOf(
        GuidedStep("Installation", "Installe-toi confortablement, ferme les yeux si tu veux. Laisse trois respirations un peu plus longues te poser.", 30),
        GuidedStep("Choisir un lieu", "Imagine un lieu où tu te sens en sécurité et apaisé : réel ou inventé. Un bord de mer, une forêt, une pièce aimée…", 40),
        GuidedStep("Voir", "Regarde autour de toi dans ce lieu. Les couleurs, la lumière, les formes, ce qui est proche et ce qui est loin.", 45),
        GuidedStep("Entendre et sentir", "Écoute les sons de ce lieu. Remarque la température, l'air sur la peau, les odeurs.", 45),
        GuidedStep("S'installer", "Trouve l'endroit précis où tu te poses. Remarque la sensation de calme dans ton corps : où la sens-tu le plus ?", 50),
        GuidedStep("Un ancrage", "Choisis un geste discret (pouce contre index, par exemple) et fais-le maintenant, en associant ce geste au calme de ce lieu.", 30),
        GuidedStep("Retour", "Sache que tu peux revenir ici quand tu veux. Reprends doucement contact avec la pièce, bouge les doigts, ouvre les yeux.", 30),
    )

    val sleepRelax = listOf(
        GuidedStep("Se poser", "Allongé, laisse le poids du corps s'enfoncer dans le matelas. Rien à réussir, rien à forcer.", 30),
        GuidedStep("Ralentir le souffle", "Inspire par le nez, expire lentement par la bouche, comme pour souffler sur une bougie sans l'éteindre. Encore quelques fois.", 50),
        GuidedStep("Lourdeur", "Imagine tes jambes devenir lourdes et chaudes, puis tes bras, puis ton dos. Chaque expiration ajoute un peu de lourdeur.", 60),
        GuidedStep("Descendre", "Imagine un escalier doux de dix marches. À chaque expiration, tu descends d'une marche, un peu plus loin dans le calme.", 70),
        GuidedStep("Les pensées passent", "Si des pensées viennent, imagine-les comme des nuages qui traversent le ciel. Tu les laisses passer et tu reviens au souffle.", 60),
        GuidedStep("Laisser venir le sommeil", "Tu n'as plus rien à faire. Reste dans cette lourdeur tranquille, et laisse le sommeil venir quand il voudra.", 60),
    )

    val decompression = listOf(
        GuidedStep("Fin de journée", "Ta journée de travail est terminée. Prends un instant pour marquer cette frontière, avant de passer à la suite.", 20),
        GuidedStep("Ce qui est fait", "Pense à trois choses que tu as faites aujourd'hui, même petites. Elles comptent.", 40),
        GuidedStep("Ce qui reste", "Ce qui n'est pas fini attendra demain. Si une tâche tourne dans ta tête, note-la mentalement pour demain, puis pose-la.", 40),
        GuidedStep("Relâcher", "Hausse les épaules vers les oreilles en inspirant, laisse-les tomber en expirant. Trois fois.", 30),
        GuidedStep("Passer à autre chose", "Choisis ce que tu vas faire maintenant pour toi : marcher, appeler quelqu'un, cuisiner, ne rien faire. Le travail reste au travail.", 30),
    )

    // ---------------- Habitudes ----------------

    val habitIdeas = listOf(
        "🚶" to "Marcher 20 minutes",
        "🥪" to "Vraie pause de midi, sans écran",
        "📵" to "Pas de mails pro après 19 h",
        "🌬️" to "5 minutes de respiration",
        "🛏️" to "Au lit avant 23 h",
        "💧" to "Boire assez d'eau",
        "☀️" to "Lumière du jour le matin",
        "📝" to "Noter 3 gratitudes",
        "🧘" to "Pause de 3 minutes au travail",
        "🎵" to "Un moment rien que pour moi",
        "🗣️" to "Parler à quelqu'un qui compte",
        "🚪" to "Rituel de fin de journée",
    )

    // ---------------- Objectifs et thèmes ----------------

    val objectives = listOf(
        "burnout" to "Prévenir une rechute de burn-out",
        "stress" to "Mieux gérer le stress",
        "energie" to "Retrouver de l'énergie",
        "sommeil" to "Mieux dormir",
        "anxiete" to "Apprivoiser l'anxiété",
        "confiance" to "Être plus bienveillant avec moi",
    )

    val themes = listOf(
        Theme("burnout", "Burn-out", "🔋", "Repérer les signaux, protéger son énergie, prévenir la rechute.",
            listOf("burnout", "stress"),
            listOf("Mon plan anti-rechute" to Route("plan"), "Décompression de fin de journée" to Route("decompression"),
                "Pause 3 minutes" to Route("pause3"), "Activités ressources" to Route("activity")),
            listOf("bat12", "phq9"),
            listOf("Rituel de fin de journée", "Pas de mails pro après 19 h", "Vraie pause de midi, sans écran", "Marcher 20 minutes")),
        Theme("stress", "Stress", "🌿", "Faire redescendre la pression et récupérer.",
            listOf("stress"),
            listOf("Cohérence cardiaque" to Route("breath", "coherence"), "Relaxation progressive" to Route("pmr"),
                "Fiche de pensée" to Route("thought"), "Pause 3 minutes" to Route("pause3")),
            listOf("gad7"),
            listOf("5 minutes de respiration", "Marcher 20 minutes")),
        Theme("energie", "Énergie", "⚡", "Comprendre ce qui vide et ce qui recharge.",
            listOf("burnout"),
            listOf("Activités ressources" to Route("activity"), "Respiration dynamisante" to Route("breath", "eveil"),
                "Lieu ressource" to Route("safeplace")),
            listOf("bat12"),
            listOf("Lumière du jour le matin", "Marcher 20 minutes", "Boire assez d'eau")),
        Theme("sommeil", "Sommeil", "🌙", "Préparer des nuits plus sereines.",
            listOf("sommeil"),
            listOf("Détente avant le sommeil" to Route("sleeprelax"), "Respiration 4-7-8" to Route("breath", "478"),
                "Scan corporel" to Route("bodyscan")),
            listOf("phq9"),
            listOf("Au lit avant 23 h", "Lumière du jour le matin")),
        Theme("anxiete", "Anxiété", "🌊", "Sortir du cercle des inquiétudes.",
            listOf("anxiete"),
            listOf("Ancrage 5-4-3-2-1" to Route("grounding"), "Respiration carrée" to Route("breath", "carree"),
                "Pièges de la pensée" to Route("distortions"), "Lieu ressource" to Route("safeplace")),
            listOf("gad7"),
            listOf("5 minutes de respiration", "Parler à quelqu'un qui compte")),
        Theme("confiance", "Bienveillance", "☀️", "Adoucir la voix critique intérieure.",
            listOf("confiance"),
            listOf("Fiche de pensée" to Route("thought"), "Trois gratitudes" to Route("journal", "g:")),
            listOf("phq9"),
            listOf("Noter 3 gratitudes", "Un moment rien que pour moi")),
    )

    fun theme(id: String) = themes.firstOrNull { it.id == id }

    // ---------------- Parcours burn-out ----------------

    val burnoutProgram = Program("burnout", "Prévenir la rechute de burn-out", "10 jours pour repérer les signaux et protéger ton énergie", "🔋", listOf(
        ProgramDay("Faire le point, sans juger",
            "Un burn-out ne vient pas d'une faiblesse : c'est un épuisement qui s'installe quand les exigences dépassent longtemps les ressources, sans récupération suffisante. Avoir déjà traversé un épisode rend plus attentif aux signaux… et c'est un atout. Première étape : mesurer où tu en es, pour avoir un point de comparaison dans les semaines qui viennent.",
            "Remplis le questionnaire d'épuisement professionnel (BAT-12). Si le résultat est orange ou rouge, prends rendez-vous chez ton médecin.", ToolLink("test", "bat12")),
        ProgramDay("Tes signaux d'alerte",
            "Avant la rechute, il y a presque toujours des signaux : sommeil qui se dégrade, irritabilité, cynisme, oublis, besoin de « rattraper » le soir, plaisirs abandonnés. Ils sont personnels. Ceux de ton épisode précédent sont précieux : ce sont souvent les mêmes qui reviennent.",
            "Commence ton plan anti-rechute par la section « Mes signaux d'alerte précoces ».", ToolLink("plan")),
        ProgramDay("Le compte en banque d'énergie",
            "Chaque activité retire ou dépose de l'énergie. Le problème n'est pas d'avoir des journées chargées, mais un solde négatif jour après jour. Noter chaque soir ton énergie et ta charge ressentie fait apparaître la tendance — et une tendance se corrige plus facilement qu'une crise.",
            "Fais ton check-in et renseigne les curseurs Énergie et Charge.", ToolLink("mood")),
        ProgramDay("Récupérer vraiment",
            "La recherche sur la récupération montre quatre leviers : se détacher mentalement du travail, se détendre, faire des activités qui donnent un sentiment de maîtrise (sport, bricolage, apprendre), et garder du contrôle sur son temps libre. Regarder des écrans en pensant au travail ne recharge pas.",
            "Planifie une activité qui te recharge vraiment pour demain.", ToolLink("activity")),
        ProgramDay("La frontière du soir",
            "Le détachement psychologique est l'un des meilleurs protecteurs contre l'épuisement. Un rituel de fin de journée — même de 3 minutes — marque la frontière : le travail reste au travail. Les notifications professionnelles coupées le soir aident beaucoup.",
            "Fais l'exercice « Décompression de fin de journée ».", ToolLink("decompression")),
        ProgramDay("Les pensées qui poussent à en faire trop",
            "« Je dois tout faire parfaitement », « Si je ne le fais pas, personne ne le fera », « Dire non, c'est décevoir » : ces règles intérieures alimentent la surcharge. Les examiner ne veut pas dire devenir négligent, mais choisir où mettre son énergie.",
            "Remplis une fiche de pensée sur une situation où tu t'es surinvesti.", ToolLink("thought")),
        ProgramDay("Poser des limites",
            "Une limite claire protège ta santé et la qualité de ton travail. Elle se formule simplement : constater, dire ce que tu peux faire, proposer une alternative. Ex. : « Je ne pourrai pas le faire pour demain ; je peux le terminer jeudi, ou alléger autre chose si c'est prioritaire. »",
            "Note dans ton plan tes limites non négociables.", ToolLink("plan")),
        ProgramDay("De petites habitudes protectrices",
            "La prévention tient dans de petites habitudes répétées plutôt que dans de grandes résolutions : une vraie pause de midi, une marche, une heure de déconnexion. Choisis-en deux ou trois, pas plus, et suis-les sans viser la perfection.",
            "Ajoute 2 ou 3 habitudes protectrices.", ToolLink("habits")),
        ProgramDay("Ne pas rester seul",
            "Le burn-out isole : on se replie, on cache la fatigue. En parler tôt — à un proche, à ton médecin, au médecin du travail ou à un psychologue — permet d'ajuster avant l'effondrement. Demander de l'aide est une compétence, pas un aveu d'échec.",
            "Complète la section « Les personnes à prévenir » de ton plan.", ToolLink("plan")),
        ProgramDay("Ton plan, prêt à servir",
            "Relis ton plan anti-rechute en entier. Le jour où des signaux reviennent, tu n'auras pas à réfléchir : tu suivras ce que tu as décidé à froid. Refais le BAT-12 toutes les 2 à 4 semaines pour suivre l'évolution, et consulte sans attendre si tu atteins les seuils que tu t'es fixés.",
            "Relis et complète ton plan anti-rechute.", ToolLink("plan")),
    ))
}
