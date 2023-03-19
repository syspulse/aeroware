# aw-sigmet

## Info

[https://en.wikipedia.org/wiki/SIGMET]
[https://wradlib.org/]
[https://www.aviationweather.gov/help/webservice?page=isigmetjson]

## Examples

```
WVRA31 RUPK 270603
UHMM SIGMET P02 VALID 270604/271120 UHPP-
UHMM MAGADAN FIR VA ERUPTION MT SHEVELUCH PSN N5638 E16119 VA CLD
OBS AT 0520Z WI N5643 E16118 - N5654 E16152 - N5642 E16157 - N5635
E16119 - N5643 E16118 SFC/FL130 FCST AT 1120Z WI N5652 E16336
- N5639 E16121 - N5743 E16357 - N5731 E16500 - N5652 E16336=
```

Europe:

```
WSSP32 LEMM 191022
LECB SIGMET 3 VALID 191100/191500 LEVA-
LECB BARCELONA UIR SEV TURB FCST WI N4024 E00441 - N3756 E00221
- N36 W00207 - N4240 E00118 - N4201 E00438 - N4024 E00441
FL210/330 MOV E  NC=
```

US:
```
WSNT01 KKCI 191245
SIGA0A
KZWY KZMA SIGMET ALFA 5 VALID 191245/191645 KKCI-
NEW YORK OCEANIC FIR MIAMI OCEANIC FIR FRQ TS OBS AT 1245Z WI
N3245 W07200 - N3145 W06945 - N2745 W07615 - N2845 W07745 - N3245
W07200. TOP FL320. MOV E 15KT. NC.
```

Example parsed:

```
{ "type": "FeatureCollection",
    "features": [
    { "type": "Feature",
      "id": "562732",
      "properties": {
          "data": "ISIGMET",
          "icaoId": "NZKL", 
          "firId": "NZZO", 
          "firName": "NZZO AUCKLAND OCEANIC", 
          "seriesId": "48", 
          "hazard": "TURB", 
          "validTimeFrom": "2019-12-30T16:08:00Z", 
          "validTimeTo": "2019-12-30T19:12:00Z", 
          "qualifier": "SEV", 
          "geom": "AREA", 
          "coords": "-57.667,174.500,-47.333,177.833,-42.667,182.667,-56.833,179.500,
          -67.333,168.833,-64.000,162.833,-57.667,174.500", 
          "base": 20000, 
          "top": 46000, 
          "dir": "SE", 
          "spd": "25", 
          "chng": "NC", 
          "rawSigmet": "WSPS21 NZKL 301606\nNZZO SIGMET 48 VALID 301608/302008 NZKL-\n
              NZZO AUCKLAND OCEANIC FIR SEV TURB FCST WI S5740 E17430 - S4720\n
              E17750 - S4240 W17720 - S5650 E17930 - S6720 E16850 - S6400 E16250 -\n
              S5740 E17430 FL200/460 MOV SE 25KT NC="
      },
      "geometry": {
          "type": "Polygon",
          "coordinates": [
              [[174.50,-57.67],[177.83,-47.33],[182.67,-42.67],[179.50,-56.83],
          [168.83,-67.33],[162.83,-64.00],[174.50,-57.67] ]
          ]
      } 
    },
  ]
}
```