package be.dexter.serenite

/** Lien d'une leçon vers un outil. kind : breath, thought, journal, grounding, pmr, bodyscan, activity, mood, distortions. */
data class ToolLink(val kind: String, val arg: String = "")

data class Distortion(val id: String, val name: String, val desc: String, val example: String, val question: String)

/** Rythme respiratoire en secondes. Une phase à 0 est sautée. */
data class BreathPattern(
    val id: String, val name: String, val desc: String,
    val inhale: Int, val hold1: Int, val exhale: Int, val hold2: Int,
    val defaultMinutes: Int,
)

data class GuidedStep(val title: String, val text: String, val seconds: Int)

data class ProgramDay(val title: String, val lesson: String, val action: String, val link: ToolLink? = null)

data class Program(val id: String, val title: String, val subtitle: String, val emoji: String, val days: List<ProgramDay>)

object Content {

    val moodLabels = listOf("Très mal", "Pas bien", "Moyen", "Bien", "Très bien")
    val moodEmojis = listOf("😣", "😕", "😐", "🙂", "😄")

    val positiveEmotions = listOf(
        "Joie", "Calme", "Gratitude", "Fierté", "Espoir", "Motivation", "Amour", "Soulagement", "Curiosité", "Confiance",
    )
    val negativeEmotions = listOf(
        "Fatigue", "Stress", "Anxiété", "Tristesse", "Colère", "Frustration", "Irritation", "Culpabilité",
        "Honte", "Solitude", "Ennui", "Découragement", "Peur", "Jalousie", "Submergé",
    )
    val emotions = positiveEmotions + negativeEmotions

    val factors = listOf(
        "Sommeil", "Travail", "Sport", "Famille", "Couple", "Amis", "Santé", "Alimentation",
        "Écrans", "Météo", "Argent", "Transport", "Loisirs", "Nature", "Solitude",
    )

    val distortions = listOf(
        Distortion("tout-rien", "Tout ou rien",
            "Voir les choses en noir ou blanc, sans nuance : soit c'est parfait, soit c'est un échec.",
            "« Si je ne termine pas tout aujourd'hui, ma journée est fichue. »",
            "Où se situe la situation sur une échelle de 0 à 100, plutôt qu'à l'un des deux extrêmes ?"),
        Distortion("generalisation", "Généralisation excessive",
            "Tirer une règle générale d'un seul événement (« toujours », « jamais », « tout le monde »).",
            "« J'ai raté cet entretien, je rate toujours tout. »",
            "Combien de fois exactement ? Est-ce vraiment « toujours » ? Ai-je des contre-exemples ?"),
        Distortion("filtre", "Filtre mental",
            "Ne retenir qu'un détail négatif et ignorer tout le reste.",
            "« Une remarque critique dans l'évaluation : elle est mauvaise. »",
            "Qu'est-ce que je laisse de côté ? Quels éléments positifs ou neutres font aussi partie du tableau ?"),
        Distortion("disqualif", "Disqualification du positif",
            "Rejeter les expériences positives en considérant qu'elles « ne comptent pas ».",
            "« Il m'a félicité, mais il dit ça à tout le monde. »",
            "Si un ami vivait la même réussite, la considérerais-je comme sans valeur ?"),
        Distortion("lecture", "Lecture de pensée",
            "Supposer savoir ce que les autres pensent, sans preuve.",
            "« Il ne m'a pas répondu, il doit m'en vouloir. »",
            "Quelles autres explications sont possibles ? Comment pourrais-je le vérifier ?"),
        Distortion("voyance", "Erreur de voyance",
            "Prédire que les choses vont mal tourner et le tenir pour acquis.",
            "« Ça va forcément mal se passer demain. »",
            "Quelles sont les autres issues possibles ? Mes prédictions passées se sont-elles toujours réalisées ?"),
        Distortion("catastrophe", "Dramatisation",
            "Imaginer le pire scénario et le juger insupportable.",
            "« Si je fais une erreur, je vais perdre mon poste et tout s'écroulera. »",
            "Quel est le scénario le plus probable ? Et si le pire arrivait, comment ferais-je face ?"),
        Distortion("loupe", "Maximisation / minimisation",
            "Grossir ses défauts ou erreurs, réduire ses qualités ou réussites.",
            "« Cette faute de frappe est impardonnable ; mon projet abouti, n'importe qui l'aurait fait. »",
            "Quelle importance cela aura-t-il dans une semaine ? dans un an ?"),
        Distortion("emotionnel", "Raisonnement émotionnel",
            "Prendre ses émotions pour des faits : « je le ressens, donc c'est vrai ».",
            "« Je me sens nul, donc je suis nul. »",
            "Quels faits, indépendants de mon ressenti, soutiennent ou contredisent cette idée ?"),
        Distortion("devrais", "Les « je dois »",
            "Se fixer des règles rigides (« je dois », « il faut », « je devrais ») dont le non-respect culpabilise.",
            "« Je devrais toujours être disponible pour tout le monde. »",
            "Puis-je remplacer « je dois » par « je préférerais » ? Que se passe-t-il réellement si la règle n'est pas respectée ?"),
        Distortion("etiquette", "Étiquetage",
            "Se coller (ou coller aux autres) une étiquette globale à partir d'un comportement.",
            "« J'ai oublié ce rendez-vous : je suis un incapable. »",
            "Puis-je décrire le comportement précis plutôt que juger la personne entière ?"),
        Distortion("perso", "Personnalisation",
            "Se croire responsable d'événements qui dépendent en grande partie d'autres facteurs.",
            "« Si l'équipe est tendue, c'est à cause de moi. »",
            "Quels autres facteurs ont joué ? Quelle part revient réellement à moi ?"),
    )

    fun distortion(id: String) = distortions.firstOrNull { it.id == id }

    val breathPatterns = listOf(
        BreathPattern("coherence", "Cohérence cardiaque",
            "6 respirations par minute : 5 s d'inspiration, 5 s d'expiration. Idéalement 5 minutes, jusqu'à 3 fois par jour.",
            5, 0, 5, 0, 5),
        BreathPattern("carree", "Respiration carrée",
            "4 temps égaux : inspirer, retenir, expirer, retenir. Aide à se recentrer avant une situation tendue.",
            4, 4, 4, 4, 3),
        BreathPattern("478", "Respiration 4-7-8",
            "Inspirer 4 s, retenir 7 s, expirer lentement 8 s. Utile le soir pour préparer l'endormissement.",
            4, 7, 8, 0, 2),
        BreathPattern("apaisante", "Expiration longue",
            "Inspirer 4 s, expirer 6 s. Une expiration plus longue que l'inspiration favorise le relâchement.",
            4, 0, 6, 0, 4),
        BreathPattern("eveil", "Respiration dynamisante",
            "Inspirer 4 s, retenir 2 s, expirer 4 s. Pour se remettre en mouvement sans s'agiter.",
            4, 2, 4, 0, 2),
    )

    fun breath(id: String) = breathPatterns.firstOrNull { it.id == id } ?: breathPatterns.first()

    val grounding = listOf(
        GuidedStep("5 choses que tu vois", "Regarde autour de toi et nomme, à voix basse ou dans ta tête, cinq choses que tu vois. Prends le temps de détailler leurs couleurs et leurs formes.", 45),
        GuidedStep("4 choses que tu touches", "Remarque quatre sensations de contact : tes pieds sur le sol, le tissu de tes vêtements, la température de tes mains, un objet à portée.", 40),
        GuidedStep("3 choses que tu entends", "Écoute trois sons, proches ou lointains. Ne les juge pas, contente-toi de les remarquer.", 35),
        GuidedStep("2 choses que tu sens", "Repère deux odeurs, même légères. S'il n'y en a pas, imagine deux odeurs que tu aimes.", 30),
        GuidedStep("1 chose que tu goûtes", "Porte ton attention sur le goût dans ta bouche, ou prends une gorgée d'eau et observe-la.", 20),
        GuidedStep("Retour", "Prends une respiration lente. Remarque comment tu te sens maintenant, ici, dans cet endroit.", 20),
    )

    val pmr: List<GuidedStep> = run {
        val groups = listOf(
            "Mains et avant-bras" to "Serre les poings fort",
            "Bras" to "Plie les coudes et contracte les biceps",
            "Front" to "Hausse les sourcils le plus haut possible",
            "Yeux et joues" to "Plisse les yeux et le nez",
            "Mâchoire" to "Serre les dents, sans forcer à l'excès",
            "Épaules" to "Remonte les épaules vers les oreilles",
            "Ventre" to "Contracte les abdominaux comme pour encaisser un choc",
            "Cuisses" to "Serre les cuisses l'une contre l'autre",
            "Mollets et pieds" to "Tire les pointes de pieds vers toi",
        )
        listOf(GuidedStep("Installation", "Assieds-toi ou allonge-toi confortablement. Ferme les yeux si tu le souhaites. Pour chaque zone : contracte environ 5 secondes, puis relâche d'un coup et observe la détente.", 20)) +
            groups.flatMap { (zone, how) ->
                listOf(
                    GuidedStep("$zone — contracte", "$how… maintiens la tension.", 6),
                    GuidedStep("$zone — relâche", "Relâche tout d'un coup. Observe la différence entre tension et détente, laisse la chaleur s'installer.", 15),
                )
            } +
            GuidedStep("Fin", "Parcours ton corps du regard intérieur. Si une zone est encore tendue, relâche-la à l'expiration. Reprends doucement contact avec la pièce.", 25)
    }

    val bodyScan = listOf(
        GuidedStep("Installation", "Allonge-toi ou assieds-toi, le dos soutenu. Laisse ta respiration suivre son rythme naturel.", 30),
        GuidedStep("Pieds", "Porte ton attention sur tes pieds : orteils, plante, talons. Remarque les sensations sans chercher à les changer.", 40),
        GuidedStep("Jambes", "Remonte vers les mollets, les genoux, les cuisses. Lourdeur, chaleur, picotements… ou rien de particulier : tout est correct.", 40),
        GuidedStep("Bassin et bas du dos", "Sens les points d'appui, le poids du corps qui repose.", 40),
        GuidedStep("Ventre et poitrine", "Observe le mouvement de la respiration qui soulève et relâche le ventre et la poitrine.", 45),
        GuidedStep("Mains et bras", "Descends le long des bras jusqu'au bout des doigts. Laisse les mains devenir lourdes.", 40),
        GuidedStep("Épaules et nuque", "Zone souvent chargée de tension : à chaque expiration, laisse-la descendre un peu.", 40),
        GuidedStep("Visage", "Mâchoire, langue, joues, yeux, front. Desserre ce qui peut l'être.", 40),
        GuidedStep("Corps entier", "Élargis l'attention au corps tout entier, qui respire. Reste ainsi quelques instants avant de reprendre tes activités.", 45),
    )

    val journalPrompts = listOf(
        "Qu'est-ce qui t'a fait du bien aujourd'hui, même un peu ?",
        "Quelle pensée est revenue le plus souvent aujourd'hui ? Est-elle utile ?",
        "Qu'aimerais-tu dire à la version de toi d'il y a un an ?",
        "Quelle situation t'a mis sous tension récemment ? Qu'est-ce qui était sous ton contrôle ?",
        "De quoi es-tu fier cette semaine ?",
        "Qu'est-ce que tu remets à plus tard ? Quel serait le plus petit premier pas ?",
        "Quand t'es-tu senti le plus calme récemment ? Que faisais-tu ?",
        "Qu'est-ce qui te prend de l'énergie en ce moment ? Qu'est-ce qui t'en donne ?",
        "Quelle règle t'imposes-tu qui pourrait être assouplie ?",
        "Si un ami vivait ta situation actuelle, que lui conseillerais-tu ?",
        "Quelle petite chose pourrais-tu faire demain pour prendre soin de toi ?",
        "Quelles sont tes trois valeurs les plus importantes ? Les as-tu vécues aujourd'hui ?",
        "Qu'as-tu appris sur toi récemment ?",
        "Quelle inquiétude pourrais-tu déposer ici pour ne plus la porter ce soir ?",
        "Qui a compté pour toi cette semaine, et pourquoi ?",
        "Qu'est-ce qui s'est mieux passé que prévu ?",
        "Quelle limite as-tu besoin de poser en ce moment ?",
        "Décris un moment agréable de la journée avec tous tes sens.",
        "Qu'est-ce qui t'inquiète le plus, et quelle est la probabilité réelle que cela arrive ?",
        "Qu'aimerais-tu ressentir davantage dans ta vie en ce moment ?",
    )

    fun promptOfDay(epochDay: Long) = journalPrompts[Math.floorMod(epochDay, journalPrompts.size.toLong()).toInt()]

    val tips = listOf(
        "Une émotion est une vague : elle monte, atteint un pic, puis redescend. Tu n'as pas besoin de la combattre pour qu'elle passe.",
        "Une pensée n'est pas un fait. Tu peux la remarquer sans y croire automatiquement.",
        "Le plus petit pas compte : 5 minutes d'une tâche valent mieux que zéro minute parfaite.",
        "Quand tout s'emballe, ralentis d'abord l'expiration. Le corps suit, puis la tête.",
        "Nommer une émotion (« je me sens anxieux ») suffit souvent à en baisser un peu l'intensité.",
        "Le mouvement est un régulateur puissant : une marche de 10 minutes change souvent l'humeur.",
        "Distingue ce qui dépend de toi de ce qui n'en dépend pas, et mets ton énergie dans la première colonne.",
        "L'évitement soulage à court terme et entretient l'anxiété à long terme. Approche-toi petit à petit.",
        "Sois pour toi-même l'ami que tu es pour les autres.",
        "La fatigue noircit les pensées. Avant une décision importante, demande-toi si tu es reposé.",
        "Planifier un moment agréable, même court, n'est pas un luxe : c'est de l'entretien.",
        "Les ruminations tournent en rond ; la résolution de problème avance. Demande-toi : « Quelle est la prochaine action concrète ? »",
        "Écrire une inquiétude la rend plus petite et plus précise que lorsqu'elle tourne dans la tête.",
        "Tu n'as pas à te sentir motivé pour commencer. La motivation vient souvent après l'action.",
        "Une journée difficile n'efface pas tes progrès. Regarde la tendance, pas un seul point.",
        "Écrans et infos en continu entretiennent la vigilance. Offre-toi des plages sans notifications.",
        "La gratitude ne nie pas les difficultés : elle élargit simplement le champ de vision.",
        "Un « non » posé clairement est souvent plus respectueux qu'un « oui » plein de ressentiment.",
        "Ton corps parle avant ta tête : mâchoire serrée, épaules hautes… c'est un signal, pas un échec.",
        "Le perfectionnisme ralentit plus qu'il ne protège. Vise le « suffisamment bien ».",
    )

    fun tipOfDay(epochDay: Long) = tips[Math.floorMod(epochDay * 7 + 3, tips.size.toLong()).toInt()]

    // ---------------- Parcours ----------------

    val programs = listOf(
        Program("stress", "Apaiser le stress", "7 jours pour comprendre et désamorcer la tension du quotidien", "🌿", listOf(
            ProgramDay("Comprendre le stress",
                "Le stress est une réaction normale : face à une exigence, le corps mobilise de l'énergie (cœur plus rapide, muscles tendus, attention aiguisée). Il devient un problème quand il dure sans période de récupération. L'objectif n'est pas de supprimer le stress, mais de retrouver des moments de redescente.",
                "Fais ton premier check-in : comment te sens-tu et qu'est-ce qui pèse aujourd'hui ?", ToolLink("mood")),
            ProgramDay("Le frein du corps",
                "La respiration est l'une des rares fonctions automatiques que l'on peut piloter. En allongeant l'expiration, on active le système nerveux qui ralentit le rythme cardiaque. Quelques minutes suffisent pour sentir une différence, et l'effet se renforce avec la régularité.",
                "Pratique 5 minutes de cohérence cardiaque.", ToolLink("breath", "coherence")),
            ProgramDay("Repérer ses signaux",
                "Le stress s'annonce souvent par de petits signaux : mâchoire serrée, épaules hautes, irritabilité, sommeil léger, besoin de grignoter. Les reconnaître tôt permet d'agir avant le débordement. Chacun a sa « signature » de stress.",
                "Note dans ton journal tes trois premiers signaux de stress et ce qui les déclenche le plus souvent.",
                ToolLink("journal", "Quels sont mes trois premiers signaux de stress, et qu'est-ce qui les déclenche ?")),
            ProgramDay("Les pensées qui accélèrent",
                "Une même situation stresse plus ou moins selon ce qu'on s'en dit. « Je n'y arriverai jamais » accélère ; « C'est beaucoup, je vais prioriser » stabilise. Les pensées automatiques ne sont pas des faits : on peut les examiner.",
                "Remplis une fiche de restructuration sur une situation stressante récente.", ToolLink("thought")),
            ProgramDay("Relâcher les muscles",
                "La tension musculaire entretient le message de danger envoyé au cerveau. La relaxation progressive consiste à contracter puis relâcher chaque groupe de muscles, pour apprendre à reconnaître — et lâcher — la tension.",
                "Fais une séance de relaxation musculaire progressive.", ToolLink("pmr")),
            ProgramDay("Remettre du bon dans l'agenda",
                "Sous stress, on sacrifie d'abord ce qui recharge : sport, amis, loisirs. Pourtant ce sont ces moments qui permettent de tenir. Les planifier, même brièvement, est une stratégie, pas un caprice.",
                "Planifie une activité agréable pour demain.", ToolLink("activity")),
            ProgramDay("Ta boîte à outils",
                "Tu as maintenant plusieurs leviers : respirer, repérer tes signaux, questionner tes pensées, relâcher le corps, protéger tes moments ressources. Le plus efficace est celui que tu utilises réellement. Choisis-en deux à garder au quotidien.",
                "Écris quels outils tu gardes et quand tu vas les utiliser.",
                ToolLink("journal", "Quels sont les deux outils anti-stress que je garde, et à quel moment de ma journée je les utilise ?")),
        )),
        Program("anxiete", "Apprivoiser l'anxiété", "7 jours pour sortir du cercle des inquiétudes", "🌊", listOf(
            ProgramDay("À quoi sert l'anxiété",
                "L'anxiété est une alarme : elle prépare à un danger possible. Le problème n'est pas l'alarme, mais sa sensibilité — elle peut sonner pour des risques peu probables ou peu graves. Apprendre à vivre avec elle passe d'abord par ne plus la considérer comme un ennemi.",
                "Fais un check-in en notant précisément tes émotions.", ToolLink("mood")),
            ProgramDay("Revenir au présent",
                "L'anxiété vit dans le futur : « et si… ». Les sens, eux, sont toujours dans le présent. L'exercice 5-4-3-2-1 utilise la vue, le toucher, l'ouïe, l'odorat et le goût pour ramener l'attention ici et maintenant.",
                "Fais l'exercice d'ancrage 5-4-3-2-1.", ToolLink("grounding")),
            ProgramDay("Les pièges de la pensée",
                "L'anxiété s'appuie souvent sur les mêmes raccourcis : prédire le pire, surestimer la probabilité d'un danger, sous-estimer sa capacité à faire face. Connaître ces pièges permet de les repérer au moment où ils surviennent.",
                "Parcours la liste des pièges de pensée et repère ceux qui te concernent.", ToolLink("distortions")),
            ProgramDay("Examiner une inquiétude",
                "Une inquiétude devient plus petite quand on l'examine : quelle est la probabilité réelle ? Quelles preuves pour, quelles preuves contre ? Et si cela arrivait, que pourrais-je faire ? On ne cherche pas à se convaincre que tout ira bien, mais à voir plus juste.",
                "Remplis une fiche de restructuration sur une inquiétude actuelle.", ToolLink("thought")),
            ProgramDay("Le temps d'inquiétude",
                "Plutôt que de lutter toute la journée contre les inquiétudes, on peut leur donner rendez-vous : 15 minutes fixes par jour pour les écrire. En dehors, quand une inquiétude surgit, on la note et on la reporte à ce rendez-vous. Beaucoup perdent leur urgence entre-temps.",
                "Écris tes inquiétudes du moment, puis ferme le journal.",
                ToolLink("journal", "Mon temps d'inquiétude : qu'est-ce qui me préoccupe, et sur quoi ai-je une prise ?")),
            ProgramDay("L'évitement entretient la peur",
                "Éviter ce qui fait peur soulage tout de suite, mais confirme au cerveau que c'était dangereux. S'en approcher progressivement, par petites marches, permet de constater que l'on peut faire face. Choisis une marche facile pour commencer.",
                "Planifie une petite action que tu évites habituellement.", ToolLink("activity")),
            ProgramDay("Accueillir sans lutter",
                "Chercher à chasser l'anxiété la renforce souvent. Une autre voie : la remarquer (« voilà l'anxiété »), respirer avec elle et continuer ce qui compte pour toi. L'émotion passe plus vite quand on ne lui résiste pas.",
                "Pratique 3 minutes de respiration carrée en laissant les sensations être là.", ToolLink("breath", "carree")),
        )),
        Program("sommeil", "Retrouver le sommeil", "7 jours pour préparer des nuits plus sereines", "🌙", listOf(
            ProgramDay("Observer ses nuits",
                "Avant de changer quoi que ce soit, il est utile d'observer : heure de coucher, de lever, réveils, ressenti au matin. Le lien entre humeur et sommeil apparaîtra dans tes statistiques si tu ajoutes le facteur « Sommeil » à tes check-ins.",
                "Fais un check-in et coche le facteur Sommeil s'il a joué aujourd'hui.", ToolLink("mood")),
            ProgramDay("Un rythme régulier",
                "L'horloge interne aime la régularité. Se lever à heure fixe, même le week-end, est souvent plus efficace que de chercher à se coucher tôt. La lumière du matin aide aussi à caler ce rythme.",
                "Écris l'heure de lever que tu vas viser cette semaine.",
                ToolLink("journal", "Quelle heure de lever régulière je vise, et qu'est-ce qui pourrait m'aider à la tenir ?")),
            ProgramDay("Le sas du soir",
                "Le cerveau a besoin d'une transition entre l'activité et le sommeil. Trente à soixante minutes plus calmes — lumière tamisée, moins d'écrans, activités répétitives — envoient le signal que la journée se termine.",
                "Ce soir, fais la respiration 4-7-8 au moment du coucher.", ToolLink("breath", "478")),
            ProgramDay("Vider la tête",
                "Les pensées du soir tournent souvent autour de ce qui reste à faire. Les écrire sur papier — tâches de demain, préoccupations — permet de les « ranger » plutôt que de les garder actives au lit.",
                "Écris ce qui occupe ton esprit avant de dormir.",
                ToolLink("journal", "Ce qui occupe mon esprit ce soir, et ce que je ferai demain à ce sujet :")),
            ProgramDay("Le lit, pour dormir",
                "Si l'on passe de longs moments éveillé au lit à ruminer, le cerveau finit par associer le lit à l'éveil. Quand le sommeil ne vient pas après un long moment, il peut être utile de se lever, faire quelque chose de calme, puis revenir quand la somnolence arrive.",
                "Fais un scan corporel pour t'aider à relâcher avant de dormir.", ToolLink("bodyscan")),
            ProgramDay("Pensées nocturnes",
                "La nuit, les problèmes paraissent plus graves : la fatigue amplifie la dramatisation. Se rappeler « c'est une pensée de 3 h du matin, je la réexaminerai demain » aide à ne pas s'y accrocher.",
                "Remplis une fiche sur une pensée qui te tient éveillé.", ToolLink("thought")),
            ProgramDay("Ton rituel",
                "Rassemble ce qui a fonctionné cette semaine en un rituel simple : une heure de lever, un sas du soir, un exercice de détente. La constance compte plus que la perfection. Si les difficultés de sommeil persistent, parles-en à ton médecin.",
                "Écris ton rituel du soir en trois étapes.",
                ToolLink("journal", "Mon rituel du soir en trois étapes :")),
        )),
        Program("confiance", "Être plus bienveillant avec soi", "7 jours pour adoucir la voix critique intérieure", "☀️", listOf(
            ProgramDay("La voix critique",
                "Beaucoup de personnes ont une voix intérieure plus dure avec elles-mêmes qu'avec quiconque. Elle se croit utile (« c'est pour me motiver ») mais fatigue et décourage. La première étape est simplement de la remarquer.",
                "Note une phrase que ta voix critique t'a dite récemment.",
                ToolLink("journal", "Une phrase que ma voix critique m'a dite récemment, et dans quelle situation :")),
            ProgramDay("Parler comme à un ami",
                "Que dirais-tu à un ami dans la même situation ? Probablement quelque chose de plus juste et de plus doux. Cette perspective n'est pas de la complaisance : c'est un regard plus réaliste.",
                "Réécris la phrase d'hier comme tu le dirais à un ami.",
                ToolLink("journal", "Ce que je dirais à un ami dans ma situation :")),
            ProgramDay("Étiquettes et nuances",
                "« Je suis nul », « je suis un raté » : l'étiquetage résume une personne entière à un moment. Décrire le comportement précis (« j'ai oublié ce dossier ») rend le problème plus petit et plus facile à corriger.",
                "Remplis une fiche en repérant l'étiquetage ou le tout-ou-rien.", ToolLink("thought")),
            ProgramDay("Les réussites oubliées",
                "Le cerveau retient mieux ce qui a mal tourné. Noter délibérément ce qui a fonctionné, même modestement, rééquilibre l'image que l'on a de soi.",
                "Écris trois choses que tu as bien faites cette semaine.",
                ToolLink("journal", "Trois choses que j'ai bien faites cette semaine :")),
            ProgramDay("Prendre soin de soi",
                "La bienveillance envers soi passe aussi par des actes : se reposer, manger correctement, s'accorder un plaisir simple. Ce ne sont pas des récompenses à mériter, mais des besoins.",
                "Planifie un moment rien que pour toi.", ToolLink("activity")),
            ProgramDay("Accueillir l'imperfection",
                "Faire des erreurs fait partie de l'expérience humaine commune. Se rappeler « je ne suis pas le seul à vivre cela » diminue l'isolement et la honte.",
                "Prends 4 minutes de respiration à expiration longue en te répétant : « C'est difficile, et c'est humain. »",
                ToolLink("breath", "apaisante")),
            ProgramDay("Une nouvelle voix",
                "La voix bienveillante se renforce avec l'usage, comme un muscle. Choisis une phrase qui te soutient et que tu pourras te redire dans les moments difficiles.",
                "Écris ta phrase de soutien.",
                ToolLink("journal", "La phrase de soutien que je veux me redire dans les moments difficiles :")),
        )),
    )

    fun program(id: String?) = programs.firstOrNull { it.id == id }

    // ---------------- Aide ----------------

    data class HelpLine(val name: String, val number: String, val desc: String)

    val helpLines = listOf(
        HelpLine("Urgences", "112", "Danger immédiat pour toi ou quelqu'un d'autre."),
        HelpLine("Centre de Prévention du Suicide", "080032123", "0800 32 123 — gratuit, 24 h/24, en français."),
        HelpLine("Zelfmoordlijn", "1813", "1813 — 24 h/24, en néerlandais."),
        HelpLine("Télé-Accueil", "107", "107 — écoute anonyme 24 h/24, en français."),
        HelpLine("Tele-Onthaal", "106", "106 — écoute anonyme 24 h/24, en néerlandais."),
    )

    const val DISCLAIMER =
        "Sérénité propose des outils d'auto-assistance inspirés des thérapies cognitives et comportementales. " +
            "Elle ne remplace pas un professionnel de santé. Si tu traverses une période difficile qui dure, " +
            "parles-en à ton médecin ou à un psychologue."
}
