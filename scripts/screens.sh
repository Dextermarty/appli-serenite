#!/usr/bin/env bash
# Parcourt l'appli sur l'émulateur et enregistre des captures dans shots/ (exécuté par GitHub Actions).
set -u
PKG=be.dexter.serenite
ACT=$PKG/.MainActivity
OUT=shots
mkdir -p $OUT
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell settings put system screen_off_timeout 600000
adb logcat -c

snap() { adb exec-out screencap -p > "$OUT/$1.png"; }
open() { # open <nom> <extra shot>
  adb shell am start -S -W -n $ACT --es shot "$2" > /dev/null; sleep 4; snap "$1"
}
scroll() { adb shell input swipe 540 1900 540 700 400; sleep 1.5; snap "$1"; }
tap() { # tap <texte exact visible>
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml > /dev/null 2>&1
  xy=$(python3 - "$1" <<'P'
import sys, re, xml.etree.ElementTree as ET
t = sys.argv[1]
for n in ET.parse('/tmp/ui.xml').iter('node'):
    if n.get('text') == t or n.get('content-desc') == t:
        a, b, c, d = map(int, re.findall(r'\d+', n.get('bounds')))
        print((a + c) // 2, (b + d) // 2); break
P
)
  if [ -n "$xy" ]; then adb shell input tap $xy; sleep 1.5; else echo "INTROUVABLE: $1" | tee -a $OUT/missing.txt; fi
}

# 1. Premier lancement (installation propre)
adb shell am start -W -n $ACT > /dev/null; sleep 5; snap 00_onboarding

# 2. Données de démo puis chaque écran
adb shell am start -S -W -n $ACT --ez demo true > /dev/null; sleep 4
open 01_accueil "tab:0"; scroll 01b_accueil_bas
open 02_humeur "tab:1"; scroll 02b_humeur_bas; scroll 02c_humeur_bas2
open 03_journal "tab:2"
open 04_exercices "tab:3"; scroll 04b_exercices; scroll 04c_exercices
open 05_explorer "tab:4"; scroll 05b_explorer
open 06_checkin "route:mood"; scroll 06b_checkin
open 07_bilans "route:tests"
open 08_bat12 "route:test|bat12"
open 09_habitudes "route:habits"
open 10_plan "route:plan"
open 11_theme_burnout "route:theme|burnout"; scroll 11b_theme
open 12_parcours_burnout "route:program|burnout"
open 13_jour "route:programDay|burnout:3"
open 14_respiration "route:breath|coherence"
open 15_fiche "route:thought"
open 16_reglages "route:settings"; scroll 16b_reglages
open 17_aide "route:help"
open 18_recherche "route:search"
open 19_objectifs "route:objectives"
open 20_activites "route:activity"
open 21_guide "route:decompression"

# 3. Parcours interactifs
open 30_bat_debut "route:test|bat12"
tap "Commencer"; snap 31_bat_question1
for i in $(seq 1 12); do tap "Souvent"; done
sleep 1; snap 32_bat_resultat; scroll 33_bat_resultat_bas

open 34_resp "route:breath|coherence"
tap "Commencer"; sleep 3; snap 35_resp_inspire; sleep 5; snap 36_resp_expire

open 37_checkin "route:mood"
tap "Bien"; tap "Calme"; tap "Sport"; scroll 38_checkin_rempli
tap "Enregistrer"; sleep 2; snap 39_apres_checkin

open 40_recherche "route:search"
adb shell input text "sommeil"; sleep 2; snap 41_recherche_sommeil

open 42_guide "route:pause3"; tap "Commencer"; sleep 4; snap 43_guide_en_cours

# 4. Journaux
adb logcat -d -b crash > $OUT/crash.txt 2>&1 || true
adb logcat -d '*:E' | grep -iE "serenite|AndroidRuntime|FATAL" | tail -300 > $OUT/errors.txt || true
ls -la $OUT
