set -e
cd /home/claude/reno-v5
cat part_models.js part_persist.js core_a.js part_money.js core_b.js part_catalog.js core_c.js core_d.js part_findpro.js part_share.js part_export.js part_photos.js part_add.js part_tail.js > app.logic.js
cat app.head.js app.scene.js app.logic.js > app.js && printf '\n' >> app.js && cat ten.part.js >> app.js && cat app.tail.js >> app.js && node --check app.js && echo BUILD OK
# cache-bust: give the script a new name each build
rm -f app.[0-9]*.js; V=$(date +%s); cp app.js app.$V.js; sed -i "s#src=\"./app[^\"]*\.js\"#src=\"./app.$V.js\"#" index.html; echo "script: app.$V.js"
