breed [ targets target ]
breed [ drugs drug ]
globals [colorList targetShape drugShape effCount counter]
drugs-own [bindTime bound parent]  
  
to start
  ca
  set colorList [ yellow green red 42]
  set targetShape "egg"
  set drugShape "default"
  
  set-current-plot "Dose-Response"
  set-plot-x-range 0 maxDrugMols
  set-plot-y-range 0 initialTargetMols
  set-current-plot-pen "effect"
  set-plot-pen-color (item 2 colorList)
  set-current-plot-pen "target"
  set-plot-pen-color (item 0 colorList)
  set-current-plot "Time-Course"
  set-plot-x-range 0 maxDrugMols
  set-plot-y-range 0 maxDrugMols
  set-current-plot-pen "drug"
  set-plot-pen-color (item 1 colorList)
  set-current-plot-pen "target"
  set-plot-pen-color (item 0 colorList)
  set-current-plot-pen "effect"
  set-plot-pen-color (item 2 colorList)
        
  cct-targets initialTargetMols [set color (item 0 colorList)]
  ask targets
  [ 
    set shape targetShape
    set ycor random-pycor
    set xcor random-pxcor
    while [any? other-targets-here or ycor = min-pycor or ycor = max-pycor] ;ensures only one target per patch
    [set ycor random-pycor
    set xcor random-pxcor]
  ]
  
  
  if simType = "dose-response"
  [
    let drCounter 1
    let inc 2
    let Emax 0
    
    while [drCounter <= maxDrugMols and count targets with [not hidden?] > 0] 
    [ 
      ask drugs [die]
      ask targets [set hidden? false]
      cct-drugs drCounter
      [ 
        set shape drugShape
        set color (item 1 colorList)
        set ycor max-pycor
        set xcor random-pxcor
        set bound false
        set parent false
      ]
      while [count drugs with [color = (item 1 colorList)] > 0 and count targets with [not hidden?] > 0][move generate]
      
      set-current-plot "Dose-Response"
      set-current-plot-pen "effect"
      plotxy drCounter count drugs with [color = (item 2 colorList)]
      set-current-plot-pen "target"
      plotxy drCounter count targets with [not hidden?] + count drugs with [color = (item 3 colorList)]
      set-current-plot "Time-Course"
      set-current-plot-pen "drug"
      plotxy drCounter drCounter
      set-current-plot-pen "effect"
      plotxy drCounter count drugs with [color = (item 2 colorList)]
      set-current-plot-pen "target"
      plotxy drCounter count targets with [not hidden?] + count drugs with [color = (item 3 colorList)]
      
      if drCounter > (maxDrugMols / 2) [set inc int (maxDrugMols / 10)]
      if inc < 1 [set inc 1]
      set drCounter drCounter + inc    
      if count drugs with [color = (item 2 colorList)] > Emax [set Emax count drugs with [color = (item 2 colorList)]]  
  
    ] 
      output-type "observed Emax: "
      output-print Emax
      output-type "final effect: "
      output-print count drugs with [color = (item 2 colorList)]
      output-type "percent targets remaining: "
      let temp ((count targets with [not hidden?] + count drugs with [color = (item 3 colorList)]) / initialTargetMols) * 100
      output-type precision temp 2
      output-print "%"
   ]
   
   if simType = "bolus time-course"
   [
     let time 0
     let Emax 0
     cct-drugs maxDrugMols
     [ 
       set shape drugShape
       set color (item 1 colorList)
       set ycor random-pycor
       set xcor random-pxcor
       set bound false
       set parent false
     ]
     set-current-plot "Time-Course"
     while [time <= simLength and count drugs with [color = (item 1 colorList)] > 0 and count targets with [not hidden?] > 0] 
     [
       move generate 
       set-current-plot-pen "effect"
       plotxy time count drugs with [color = (item 2 colorList)]
       set-current-plot-pen "target"
       plotxy time count targets with [not hidden?] + count drugs with [color = (item 3 colorList)]
       set time time + 1
       if count drugs with [color = (item 2 colorList)] > Emax [set Emax count drugs with [color = (item 2 colorList)]]
     ]  
      output-type "observed Emax: " 
      output-print Emax
      output-type "final effect: "
      output-print count drugs with [color = (item 2 colorList)] 
      output-type "percent targets remaining: "
      let temp ((count targets with [not hidden?] + count drugs with [color = (item 3 colorList)]) / initialTargetMols) * 100
      output-type precision temp 2
      output-print "%"
    ]
     
   if simType = "steady-state"
   [
     let ssTime 0
     let numDrugs 0
     let Emax 0
     let inc int (maxDrugMols / 10)
     if inc < 1 [set inc 1]
     
     while [ssTime <= (maxDrugMols * 2) and count targets with [not hidden?] > 0]
     [
      set numDrugs int ((maxDrugMols * ssTime) / ((maxDrugMols / 4) + ssTime))
      
      cct-drugs numDrugs
      [ 
        set shape drugShape
        set color (item 1 colorList)
        set ycor max-pycor
        set xcor random-pxcor
        set bound false
        set parent false
      ]
      
      while [count drugs with [color = (item 1 colorList)] > 0 and count targets with [not hidden?] > 0][move generate] 
      set-current-plot "Dose-Response"
      set-current-plot-pen "effect"
      plotxy numDrugs count drugs with [color = (item 2 colorList)]
      set-current-plot-pen "target"
      plotxy numDrugs count targets with [not hidden?] + count drugs with [color = (item 3 colorList)]
      set-current-plot "Time-Course"
      set-current-plot-pen "drug"
      plotxy ssTime numDrugs
      set-current-plot-pen "effect"
      plotxy ssTime count drugs with [color = (item 2 colorList)]
      set-current-plot-pen "target"
      plotxy ssTime count targets with [not hidden?] + count drugs with [color = (item 3 colorList)]

      set ssTime ssTime + inc
      if count drugs with [color = (item 2 colorList)] > Emax [set Emax count drugs with [color = (item 2 colorList)]]
           
     ]
      output-type "observed Emax: "
      output-print Emax
      output-type "final effect: "
      output-print count drugs with [color = (item 2 colorList)]
      output-type "percent targets remaining: "
      let temp ((count targets with [not hidden?] + count drugs with [color = (item 3 colorList)]) / initialTargetMols) * 100
      output-type precision temp 2
      output-print "%"
      
   ]
   
   if simType = "hysteresis"
   [
     let hTime 0
     let hTime2 0
     let numDrugs 10
     let Emax 0
     let flag true
    
     let inc 1
     let f (1 / 1.4267 * ln (maxDrugMols / 0.1796))
          
     while [numDrugs > 0 and count targets with [not hidden?] > 0 or flag]
     [
        ifelse hTime < (-5 * f + 30) [set inc 1] [set inc (int (maxDrugMols / 10))]
        if inc < 1 [set inc 1]
        
        let temp 0
        while [int ((-1 * exp ((-.3 * temp) + 6)) + (3 * exp (-.08 * temp + f))) < 1]
        [set temp temp + 1]
        
        set numDrugs int ((-1 * exp ((-.3 * (hTime + temp)) + 6)) + (3 * exp (-.08 * (hTime + temp) + f)))
        if numDrugs < 0 [set numDrugs 0]      
        
        if numDrugs > 0 [set flag false]
      
        cct-drugs numDrugs 
        [
          set shape drugShape
          set color (item 1 colorList)
          set ycor max-pycor
          set xcor random-pxcor
          set bound false
          set parent false
        ]
        
        while [count drugs with [color = (item 1 colorList)] > 0 and count targets with [not hidden?] > 0][move generate]
            
        set-current-plot "Dose-Response"
        set-current-plot-pen "effect"
        plotxy numDrugs count drugs with [color = (item 2 colorList)]
        set-current-plot-pen "target"
        plotxy numDrugs count targets with [not hidden?] + count drugs with [color = (item 3 colorList)]
        set-current-plot "Time-Course"
        set-current-plot-pen "drug"
        plotxy hTime numDrugs
        set-current-plot-pen "target"
        plotxy hTime count targets with [not hidden?] + count drugs with [color = (item 3 colorList)]
        set-current-plot-pen "effect"
        plotxy hTime count drugs with [color = (item 2 colorList)]
      
        set hTime hTime + inc
        if count drugs with [color = (item 2 colorList)] > Emax [set Emax count drugs with [color = (item 2 colorList)]]
     ]
      output-type "observed Emax: "
      output-print Emax
      output-type "final effect: "
      output-print count drugs with [color = (item 2 colorList)] 
      output-type "percent targets remaining: "
      let temp ((count targets with [not hidden?] + count drugs with [color = (item 3 colorList)]) / initialTargetMols) * 100
      output-type precision temp 2
      output-print "%"
    ]
 end

to generate
  random-seed new-seed
  let growth random 101
  set growth growth + 1
    
  if growthRate < 0 and (-1 * growth) >= growthRate and any? targets
  [
   ask one-of targets [die]
  ]
     
  if growthRate > 0 and growth <= growthRate
  [ cct-targets 1 
    [
     set shape targetShape
     set color (item 0 colorList)
     set ycor random-pycor
     set xcor random-pxcor
     while [any? other-targets-here or ycor = min-pycor or ycor = max-pycor] ;ensures only one target per patch
     [set ycor random-pycor
     set xcor random-pxcor]
    ]    
  ]
end

to move
if Start/Stop
[
  ask drugs with [color = (item 1 colorList)]
  [
      random-seed new-seed
      let affinity random 100
      set affinity affinity + 1
      ifelse any? targets-here and not any? other-drugs-here and affinity <= bindingAffinity
      
      [set bound true
       set color (item 3 colorList) 
       set shape "pentagon"
       set size 0.85
       set bindTime counter    
      ] ;if there is a free mol here, bind
      
      [
        ifelse simType = "bolus time-course" ;move in any direction
        [random-seed new-seed
        facexy random-pxcor random-pycor
        fd random-float 2.5
        ]        
        [random-seed new-seed   ;move downwards
         facexy random-pxcor min-pycor
         fd random-float 2.5] 
        ]
        
      if simType != "bolus time-course"
      [if ycor <= (min-pycor + 0.75) [die]] ;at the bottom of the simulation
     ] 
   
   ask drugs with [color = (item 3 colorList)] ;newly bound drugs, test time delay and efficacy
   [
     random-seed new-seed
     let tempEff random 100
     set tempEff tempEff + 1
     if tempEff <= efficacy and counter = bindTime + timeDelay [set color (item 2 colorList)] 
   ]
 
   ask drugs with [bound = true] ;dissociation
   [
     random-seed new-seed
     let diss random 100
     set diss diss + 1
     
     if diss <= dissociation
     [set color (item 1 colorList) set shape drugShape set size 1.0
      set ycor ycor - 1]
      
   ]
   
   ask drugs with [bound = true and parent = false] ;regulation, does nothing if targetRegulation is 0
   [  
     random-seed new-seed
     let reg random 101
     set reg reg + 1
     
     if targetRegulation < 0 and (-1 * reg) >= targetRegulation
     [
       ask targets-here [die]
       set bound false
       
     ]
     
     if targetRegulation > 0 and reg <= targetRegulation
     [ hatch-targets 1 
       [
         set heading random 361
         fd 1 
         set color (item 0 colorList)
         set shape targetShape
         set size 1.0
         if any? other-targets-here [die]
       ]
       
     ]
     set parent true
     
   ]
   
   ask drugs with [color = (item 2 colorList)] ;a drug can only attack one target, then it is inactivated
   [if not any? targets-here [die]]
   
   ask targets
   [ 
    ifelse any? drugs-here with [bound = true] [set hidden? true] [set hidden? false]
   ]
 
   set counter counter + 1
  ]

  if not Start/Stop [ask turtles [die] ask drugs [die]]
 
end
 
 
@#$#@#$#@
GRAPHICS-WINDOW
687
10
1089
433
-1
-1
7.0
1
8
1
1
1
0
1
0
1
0
55
0
55

CC-WINDOW
5
530
1112
625
Command Center
0

SLIDER
226
77
398
110
maxDrugMols
maxDrugMols
0
200
200
1
1
NIL

SLIDER
226
38
398
71
initialTargetMols
initialTargetMols
0
500
40
1
1
NIL

PLOT
287
297
542
495
Dose-Response
Dose
Effect
0.0
10.0
0.0
10.0
true
true
PENS
"effect" 1.0 0 -2674135 true
"target" 1.0 0 -1184463 true

BUTTON
27
36
90
69
NIL
start
NIL
1
T
OBSERVER
T
NIL

SLIDER
227
128
399
161
bindingAffinity
bindingAffinity
0
100
100
1
1
NIL

SLIDER
228
167
400
200
dissociation
dissociation
0
100
3
1
1
NIL

CHOOSER
39
88
185
133
simType
simType
"dose-response" "bolus time-course" "steady-state" "hysteresis"
0

SLIDER
29
215
201
248
simLength
simLength
0
1000
100
1
1
NIL

TEXTBOX
45
249
226
278
applies only to bolus time-course

PLOT
21
296
274
493
Time-Course
Time
Count
0.0
10.0
0.0
10.0
true
true
PENS
"effect" 1.0 0 -2674135 true
"drug" 1.0 0 -10899396 true
"target" 1.0 0 -1184463 true

SLIDER
439
52
611
85
efficacy
efficacy
0
100
100
1
1
NIL

SLIDER
439
89
611
122
timeDelay
timeDelay
0
100
0
1
1
NIL

SLIDER
440
130
612
163
targetRegulation
targetRegulation
-100
100
0
1
1
NIL

SWITCH
98
36
211
69
Start/Stop
Start/Stop
0
1
-1000

SLIDER
442
216
614
249
growthRate
growthRate
-100
100
0
1
1
NIL

TEXTBOX
542
251
681
281
per 100 turns

OUTPUT
548
449
1103
516

@#$#@#$#@
WHAT IS IT?
-----------
This section could give a general understanding of what the model is trying to show or explain.


HOW IT WORKS
------------
At the start of a simulation, the TARGETS are distributed randomly through the
WORLD. In most cases, DRUGS are distributed randomly within the top of the WORLD.
DRUGS PERFUSE down the world using a random walk that is biased in the x-direction.
They are ELIMINATED at the bottom (exceptions are bolus time-course simulations, detailed in Table I). The input of drug can follow one of four patterns, detailed as
simulationTypes in Table I.


HOW TO USE IT
-------------
This section could explain how to use the model, including a description of each of the items in the interface tab.




@#$#@#$#@
default
true
0
Polygon -7500403 true true 150 5 40 250 150 205 260 250

link
true
0
Line -7500403 true 150 0 150 300

link direction
true
0
Line -7500403 true 150 150 30 225
Line -7500403 true 150 150 270 225

airplane
true
0
Polygon -7500403 true true 150 0 135 15 120 60 120 105 15 165 15 195 120 180 135 240 105 270 120 285 150 270 180 285 210 270 165 240 180 180 285 195 285 165 180 105 180 60 165 15

arrow
true
0
Polygon -7500403 true true 150 0 0 150 105 150 105 293 195 293 195 150 300 150

box
false
0
Polygon -7500403 true true 150 285 285 225 285 75 150 135
Polygon -7500403 true true 150 135 15 75 150 15 285 75
Polygon -7500403 true true 15 75 15 225 150 285 150 135
Line -16777216 false 150 285 150 135
Line -16777216 false 150 135 15 75
Line -16777216 false 150 135 285 75

bug
true
0
Circle -7500403 true true 96 182 108
Circle -7500403 true true 110 127 80
Circle -7500403 true true 110 75 80
Line -7500403 true 150 100 80 30
Line -7500403 true 150 100 220 30

butterfly
true
0
Polygon -7500403 true true 150 165 209 199 225 225 225 255 195 270 165 255 150 240
Polygon -7500403 true true 150 165 89 198 75 225 75 255 105 270 135 255 150 240
Polygon -7500403 true true 139 148 100 105 55 90 25 90 10 105 10 135 25 180 40 195 85 194 139 163
Polygon -7500403 true true 162 150 200 105 245 90 275 90 290 105 290 135 275 180 260 195 215 195 162 165
Polygon -16777216 true false 150 255 135 225 120 150 135 120 150 105 165 120 180 150 165 225
Circle -16777216 true false 135 90 30
Line -16777216 false 150 105 195 60
Line -16777216 false 150 105 105 60

car
false
0
Polygon -7500403 true true 300 180 279 164 261 144 240 135 226 132 213 106 203 84 185 63 159 50 135 50 75 60 0 150 0 165 0 225 300 225 300 180
Circle -16777216 true false 180 180 90
Circle -16777216 true false 30 180 90
Polygon -16777216 true false 162 80 132 78 134 135 209 135 194 105 189 96 180 89
Circle -7500403 true true 47 195 58
Circle -7500403 true true 195 195 58

circle
false
0
Circle -7500403 true true 0 0 300

circle 2
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240

cow
false
0
Polygon -7500403 true true 200 193 197 249 179 249 177 196 166 187 140 189 93 191 78 179 72 211 49 209 48 181 37 149 25 120 25 89 45 72 103 84 179 75 198 76 252 64 272 81 293 103 285 121 255 121 242 118 224 167
Polygon -7500403 true true 73 210 86 251 62 249 48 208
Polygon -7500403 true true 25 114 16 195 9 204 23 213 25 200 39 123

cylinder
false
0
Circle -7500403 true true 0 0 300

dot
false
0
Circle -7500403 true true 90 90 120

egg
false
0
Circle -7500403 true true 96 76 108
Circle -7500403 true true 72 104 156
Polygon -7500403 true true 221 149 195 101 106 99 80 148

face happy
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 255 90 239 62 213 47 191 67 179 90 203 109 218 150 225 192 218 210 203 227 181 251 194 236 217 212 240

face neutral
false
0
Circle -7500403 true true 8 7 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Rectangle -16777216 true false 60 195 240 225

face sad
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 168 90 184 62 210 47 232 67 244 90 220 109 205 150 198 192 205 210 220 227 242 251 229 236 206 212 183

fish
false
0
Polygon -1 true false 44 131 21 87 15 86 0 120 15 150 0 180 13 214 20 212 45 166
Polygon -1 true false 135 195 119 235 95 218 76 210 46 204 60 165
Polygon -1 true false 75 45 83 77 71 103 86 114 166 78 135 60
Polygon -7500403 true true 30 136 151 77 226 81 280 119 292 146 292 160 287 170 270 195 195 210 151 212 30 166
Circle -16777216 true false 215 106 30

flag
false
0
Rectangle -7500403 true true 60 15 75 300
Polygon -7500403 true true 90 150 270 90 90 30
Line -7500403 true 75 135 90 135
Line -7500403 true 75 45 90 45

flower
false
0
Polygon -10899396 true false 135 120 165 165 180 210 180 240 150 300 165 300 195 240 195 195 165 135
Circle -7500403 true true 85 132 38
Circle -7500403 true true 130 147 38
Circle -7500403 true true 192 85 38
Circle -7500403 true true 85 40 38
Circle -7500403 true true 177 40 38
Circle -7500403 true true 177 132 38
Circle -7500403 true true 70 85 38
Circle -7500403 true true 130 25 38
Circle -7500403 true true 96 51 108
Circle -16777216 true false 113 68 74
Polygon -10899396 true false 189 233 219 188 249 173 279 188 234 218
Polygon -10899396 true false 180 255 150 210 105 210 75 240 135 240

house
false
0
Rectangle -7500403 true true 45 120 255 285
Rectangle -16777216 true false 120 210 180 285
Polygon -7500403 true true 15 120 150 15 285 120
Line -16777216 false 30 120 270 120

leaf
false
0
Polygon -7500403 true true 150 210 135 195 120 210 60 210 30 195 60 180 60 165 15 135 30 120 15 105 40 104 45 90 60 90 90 105 105 120 120 120 105 60 120 60 135 30 150 15 165 30 180 60 195 60 180 120 195 120 210 105 240 90 255 90 263 104 285 105 270 120 285 135 240 165 240 180 270 195 240 210 180 210 165 195
Polygon -7500403 true true 135 195 135 240 120 255 105 255 105 285 135 285 165 240 165 195

line
true
0
Line -7500403 true 150 0 150 300

line half
true
0
Line -7500403 true 150 0 150 150

pentagon
false
0
Polygon -7500403 true true 150 15 15 120 60 285 240 285 285 120

person
false
0
Circle -7500403 true true 110 5 80
Polygon -7500403 true true 105 90 120 195 90 285 105 300 135 300 150 225 165 300 195 300 210 285 180 195 195 90
Rectangle -7500403 true true 127 79 172 94
Polygon -7500403 true true 195 90 240 150 225 180 165 105
Polygon -7500403 true true 105 90 60 150 75 180 135 105

plant
false
0
Rectangle -7500403 true true 135 90 165 300
Polygon -7500403 true true 135 255 90 210 45 195 75 255 135 285
Polygon -7500403 true true 165 255 210 210 255 195 225 255 165 285
Polygon -7500403 true true 135 180 90 135 45 120 75 180 135 210
Polygon -7500403 true true 165 180 165 210 225 180 255 120 210 135
Polygon -7500403 true true 135 105 90 60 45 45 75 105 135 135
Polygon -7500403 true true 165 105 165 135 225 105 255 45 210 60
Polygon -7500403 true true 135 90 120 45 150 15 180 45 165 90

square
false
0
Rectangle -7500403 true true 30 30 270 270

square 2
false
0
Rectangle -7500403 true true 30 30 270 270
Rectangle -16777216 true false 60 60 240 240

star
false
0
Polygon -7500403 true true 151 1 185 108 298 108 207 175 242 282 151 216 59 282 94 175 3 108 116 108

target
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240
Circle -7500403 true true 60 60 180
Circle -16777216 true false 90 90 120
Circle -7500403 true true 120 120 60

tree
false
0
Circle -7500403 true true 118 3 94
Rectangle -6459832 true false 120 195 180 300
Circle -7500403 true true 65 21 108
Circle -7500403 true true 116 41 127
Circle -7500403 true true 45 90 120
Circle -7500403 true true 104 74 152

triangle
false
0
Polygon -7500403 true true 150 30 15 255 285 255

triangle 2
false
0
Polygon -7500403 true true 150 30 15 255 285 255
Polygon -16777216 true false 151 99 225 223 75 224

truck
false
0
Rectangle -7500403 true true 4 45 195 187
Polygon -7500403 true true 296 193 296 150 259 134 244 104 208 104 207 194
Rectangle -1 true false 195 60 195 105
Polygon -16777216 true false 238 112 252 141 219 141 218 112
Circle -16777216 true false 234 174 42
Rectangle -7500403 true true 181 185 214 194
Circle -16777216 true false 144 174 42
Circle -16777216 true false 24 174 42
Circle -7500403 false true 24 174 42
Circle -7500403 false true 144 174 42
Circle -7500403 false true 234 174 42

turtle
true
0
Polygon -10899396 true false 215 204 240 233 246 254 228 266 215 252 193 210
Polygon -10899396 true false 195 90 225 75 245 75 260 89 269 108 261 124 240 105 225 105 210 105
Polygon -10899396 true false 105 90 75 75 55 75 40 89 31 108 39 124 60 105 75 105 90 105
Polygon -10899396 true false 132 85 134 64 107 51 108 17 150 2 192 18 192 52 169 65 172 87
Polygon -10899396 true false 85 204 60 233 54 254 72 266 85 252 107 210
Polygon -7500403 true true 119 75 179 75 209 101 224 135 220 225 175 261 128 261 81 224 74 135 88 99

wheel
false
0
Circle -7500403 true true 3 3 294
Circle -16777216 true false 30 30 240
Line -7500403 true 150 285 150 15
Line -7500403 true 15 150 285 150
Circle -7500403 true true 120 120 60
Line -7500403 true 216 40 79 269
Line -7500403 true 40 84 269 221
Line -7500403 true 40 216 269 79
Line -7500403 true 84 40 221 269

x
false
0
Polygon -7500403 true true 270 75 225 30 30 225 75 270
Polygon -7500403 true true 30 75 75 30 270 225 225 270

@#$#@#$#@
NetLogo 3.1.4
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
