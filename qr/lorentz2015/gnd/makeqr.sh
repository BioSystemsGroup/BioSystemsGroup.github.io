#!/bin/sh
BACK=fffbca
FORE=1a3278
qrencode -o automod.png --foreground=${FORE} --background=${BACK} http://biosystemsgroup.github.io/qr/lorentz2015/gnd/automod.html
qrencode -o implimodels.png --foreground=${FORE} --background=${BACK} http://biosystemsgroup.github.io/qr/lorentz2015/gnd/implimodels.html
qrencode -o integsub.png --foreground=${FORE} --background=${BACK} http://biosystemsgroup.github.io/qr/lorentz2015/gnd/integsub.html
qrencode -o knowemb.png --foreground=${FORE} --background=${BACK} http://biosystemsgroup.github.io/qr/lorentz2015/gnd/knowemb.html
qrencode -o measuncert.png --foreground=${FORE} --background=${BACK} http://biosystemsgroup.github.io/qr/lorentz2015/gnd/measuncert.html
qrencode -o prediction.png --foreground=${FORE} --background=${BACK} http://biosystemsgroup.github.io/qr/lorentz2015/gnd/prediction.html
qrencode -o repuncert.png --foreground=${FORE} --background=${BACK} http://biosystemsgroup.github.io/qr/lorentz2015/gnd/repuncert.html
qrencode -o scaling.png --foreground=${FORE} --background=${BACK} http://biosystemsgroup.github.io/qr/lorentz2015/gnd/scaling.html
qrencode -o sensrob.png --foreground=${FORE} --background=${BACK} http://biosystemsgroup.github.io/qr/lorentz2015/gnd/sensrob.html
qrencode -o structanalog.png --foreground=${FORE} --background=${BACK} http://biosystemsgroup.github.io/qr/lorentz2015/gnd/structanalog.html
qrencode -o translation.png --foreground=${FORE} --background=${BACK} http://biosystemsgroup.github.io/qr/lorentz2015/gnd/translation.html
qrencode -o validation.png --foreground=${FORE} --background=${BACK} http://biosystemsgroup.github.io/qr/lorentz2015/gnd/validation.html
exit 0
